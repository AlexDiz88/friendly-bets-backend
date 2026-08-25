package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ErrorLogDto;
import net.friendly_bets.services.ErrorLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/error-logs")
public class ErrorLogController {

    private final ErrorLogService errorLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<List<ErrorLogDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(errorLogService.listRecent(page, size));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Long>> count() {
        return ResponseEntity.ok(Map.of("count", errorLogService.count()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        errorLogService.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "errorLogDeleted"));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Object>> clearAll() {
        long cleared = errorLogService.clearAll();
        return ResponseEntity.ok(Map.of(
                "message", "errorLogsCleared",
                "cleared", cleared
        ));
    }
}
