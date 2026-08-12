package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ClientVersionDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.AppSettings;
import net.friendly_bets.repositories.AppSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ClientVersionService {

    private final AppSettingsService appSettingsService;
    private final AppSettingsRepository appSettingsRepository;

    @Transactional(readOnly = true)
    public ClientVersionDto getCurrent() {
        return appSettingsRepository.findById(AppSettings.DEFAULT_ID)
                .map(AppSettings::getClientVersion)
                .map(block -> ClientVersionDto.builder().buildId(block != null ? block.getBuildId() : null).build())
                .orElseGet(() -> ClientVersionDto.builder().buildId(null).build());
    }

    /**
     * CI deploy: принимает buildId только если он больше текущего (миллисекунды сборки).
     */
    @Transactional
    public ClientVersionDto setIfNewer(String buildId) {
        long incoming = parseBuildId(buildId);

        AppSettings settings = appSettingsService.getOrCreate();
        AppSettings.ClientVersionBlock current = settings.getClientVersion();
        if (current != null && current.getBuildId() != null) {
            long existing = parseBuildIdLenient(current.getBuildId());
            if (incoming <= existing) {
                return ClientVersionDto.builder().buildId(current.getBuildId()).build();
            }
        }

        AppSettings.ClientVersionBlock next = AppSettings.ClientVersionBlock.builder()
                .buildId(buildId)
                .updatedAt(Instant.now())
                .build();
        settings.setClientVersion(next);
        appSettingsService.save(settings);
        return ClientVersionDto.builder().buildId(next.getBuildId()).build();
    }

    private static long parseBuildId(String buildId) {
        try {
            return Long.parseLong(buildId);
        } catch (NumberFormatException e) {
            throw new BadRequestException("invalidClientBuildId");
        }
    }

    private static long parseBuildIdLenient(String buildId) {
        try {
            return Long.parseLong(buildId);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
