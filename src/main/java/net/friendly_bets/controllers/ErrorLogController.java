package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ErrorLogDto;
import net.friendly_bets.services.ErrorLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
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
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return ResponseEntity.ok(errorLogService.listRecent(page, size, includeArchived));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Long>> count(
            @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        long activeCount = errorLogService.countActive();
        long archivedCount = errorLogService.countArchived();
        Map<String, Long> body = new LinkedHashMap<>();
        body.put("count", includeArchived ? activeCount + archivedCount : activeCount);
        body.put("activeCount", activeCount);
        body.put("archivedCount", archivedCount);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/archive-all")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Object>> archiveAll() {
        long archived = errorLogService.archiveAllActive();
        return ResponseEntity.ok(Map.of(
                "message", "errorLogsArchivedAll",
                "archived", archived
        ));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, String>> archive(@PathVariable String id) {
        errorLogService.archiveById(id);
        return ResponseEntity.ok(Map.of("message", "errorLogArchived"));
    }

    @PatchMapping("/{id}/unarchive")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, String>> unarchive(@PathVariable String id) {
        errorLogService.unarchiveById(id);
        return ResponseEntity.ok(Map.of("message", "errorLogUnarchived"));
    }

    /** Hard-delete archived entries only. Literal path before {@code /{id}}. */
    @DeleteMapping("/archived")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Object>> clearArchived() {
        long cleared = errorLogService.clearArchived();
        return ResponseEntity.ok(Map.of(
                "message", "errorLogsArchiveCleared",
                "cleared", cleared
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        errorLogService.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "errorLogDeleted"));
    }

    /** Hard-delete active (non-archived) entries only. */
    @DeleteMapping
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Object>> clearActive() {
        long cleared = errorLogService.clearActive();
        return ResponseEntity.ok(Map.of(
                "message", "errorLogsCleared",
                "cleared", cleared
        ));
    }
}
