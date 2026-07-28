package net.friendly_bets.melbet.client;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.melbet.config.MelbetProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Decrypts Digitain {@code {payload,timestamp}} responses via bundled WASM + Node CLI.
 * Stdout/stderr are drained asynchronously to avoid Windows pipe deadlock when output &gt; ~4KB.
 */
@Component
@RequiredArgsConstructor
public class MelbetPayloadDecryptor {

    private static final Logger log = LoggerFactory.getLogger(MelbetPayloadDecryptor.class);
    private static final long TIMEOUT_SEC = 60L;

    private final MelbetProperties properties;
    private final AtomicReference<Path> workDir = new AtomicReference<>();
    private final AtomicReference<String> resolvedNode = new AtomicReference<>();

    @PostConstruct
    void prepareResources() {
        try {
            ensureWorkDir();
            String node = resolveNodeExecutable();
            resolvedNode.set(node);
            log.info("melbet decrypt ready; nodeExecutable={}", node);
        } catch (Exception e) {
            log.warn("melbet decrypt resources not prepared at startup: {}", e.getMessage());
        }
    }

    public String decryptPayloadBase64(String payloadBase64) {
        if (payloadBase64 == null || payloadBase64.isBlank()) {
            throw new BadRequestException("melbetDecryptFailed");
        }
        try {
            Path dir = ensureWorkDir();
            Path cli = dir.resolve("decrypt-cli.cjs");
            String node = resolvedNode.updateAndGet(current ->
                    current != null && !current.isBlank() ? current : resolveNodeExecutable());
            ProcessBuilder pb = new ProcessBuilder(node, cli.toAbsolutePath().toString());
            pb.directory(dir.toFile());
            pb.redirectErrorStream(false);
            Process process = pb.start();

            // Drain pipes immediately — otherwise large WASM JSON stdout blocks Node on Windows.
            CompletableFuture<byte[]> stdoutFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return process.getInputStream().readAllBytes();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            CompletableFuture<byte[]> stderrFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return process.getErrorStream().readAllBytes();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });

            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(payloadBase64.getBytes(StandardCharsets.UTF_8));
            }

            boolean finished = process.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                stdoutFuture.cancel(true);
                stderrFuture.cancel(true);
                log.warn("melbet decrypt timed out after {}s", TIMEOUT_SEC);
                throw new BadRequestException("melbetDecryptFailed");
            }

            byte[] stdoutBytes = stdoutFuture.get(5, TimeUnit.SECONDS);
            byte[] stderrBytes = stderrFuture.get(5, TimeUnit.SECONDS);
            String stdout = new String(stdoutBytes, StandardCharsets.UTF_8);
            String stderr = new String(stderrBytes, StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                log.warn("melbet decrypt exit={} stderr={}", process.exitValue(), truncate(stderr));
                throw new BadRequestException("melbetDecryptFailed");
            }
            if (stdout.isBlank()) {
                log.warn("melbet decrypt empty stdout stderr={}", truncate(stderr));
                throw new BadRequestException("melbetDecryptFailed");
            }
            return stdout;
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            log.warn("melbet decrypt IO (nodeExecutable={}): {}",
                    resolvedNode.get() != null ? resolvedNode.get() : properties.getNodeExecutable(),
                    e.getMessage());
            throw new BadRequestException("melbetDecryptFailed");
        } catch (Exception e) {
            log.warn("melbet decrypt failed: {}", e.getMessage());
            throw new BadRequestException("melbetDecryptFailed");
        }
    }

    String resolveNodeExecutable() {
        String configured = properties.getNodeExecutable();
        if (configured != null && !configured.isBlank()) {
            String trimmed = configured.trim();
            Path asPath = Paths.get(trimmed);
            if (asPath.isAbsolute() && Files.isRegularFile(asPath)) {
                return asPath.toAbsolutePath().toString();
            }
            if (!"node".equalsIgnoreCase(trimmed) && !"node.exe".equalsIgnoreCase(trimmed)) {
                return trimmed;
            }
        }
        for (Path candidate : windowsNodeCandidates()) {
            if (Files.isRegularFile(candidate)) {
                log.info("melbet: using node at {}", candidate);
                return candidate.toAbsolutePath().toString();
            }
        }
        return configured != null && !configured.isBlank() ? configured.trim() : "node";
    }

    private static List<Path> windowsNodeCandidates() {
        List<Path> paths = new ArrayList<>();
        String nvmSymlink = System.getenv("NVM_SYMLINK");
        if (nvmSymlink != null && !nvmSymlink.isBlank()) {
            paths.add(Paths.get(nvmSymlink, "node.exe"));
        }
        paths.add(Paths.get("C:\\nvm4w\\nodejs\\node.exe"));
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            paths.add(Paths.get(localAppData, "nvm", "nodejs", "node.exe"));
        }
        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null && !programFiles.isBlank()) {
            paths.add(Paths.get(programFiles, "nodejs", "node.exe"));
        }
        return paths;
    }

    private Path ensureWorkDir() throws IOException {
        Path existing = workDir.get();
        if (existing != null && Files.isRegularFile(existing.resolve("decrypt-cli.cjs"))) {
            return existing;
        }
        synchronized (workDir) {
            existing = workDir.get();
            if (existing != null && Files.isRegularFile(existing.resolve("decrypt-cli.cjs"))) {
                return existing;
            }
            Path dir = Files.createTempDirectory("melbet-decrypt-");
            copyResource("melbet/decrypt-cli.cjs", dir.resolve("decrypt-cli.cjs"));
            copyResource("melbet/decrypt.js", dir.resolve("decrypt.js"));
            copyResource("melbet/decrypt.wasm", dir.resolve("decrypt.wasm"));
            workDir.set(dir);
            return dir;
        }
    }

    private static void copyResource(String classpath, Path target) throws IOException {
        ClassPathResource resource = new ClassPathResource(classpath);
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}
