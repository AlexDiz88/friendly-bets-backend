package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalApiMonitoringLayerPageDto;
import net.friendly_bets.dto.ExternalApiMonitoringRunDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.services.ExternalApiMonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/external-api-monitoring")
public class ExternalApiMonitoringController {

    private final ExternalApiMonitoringService monitoringService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<ExternalApiMonitoringLayerPageDto> list(
            @RequestParam String layer,
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "50") int limit
    ) {
        ExternalDataLayer parsed = parseLayer(layer);
        return ResponseEntity.ok(monitoringService.listPageByLayer(parsed, hours, limit));
    }

    @GetMapping("/latest")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, ExternalApiMonitoringRunDto>> latestByLayers() {
        Map<String, ExternalApiMonitoringRunDto> out = new LinkedHashMap<>();
        for (ExternalDataLayer layer : ExternalDataLayer.values()) {
            ExternalApiMonitoringRun latest = monitoringService.latestByLayer(layer);
            if (latest != null) {
                out.put(layer.name(), ExternalApiMonitoringRunDto.summary(latest));
            }
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<ExternalApiMonitoringRunDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(ExternalApiMonitoringRunDto.from(monitoringService.getById(id)));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Object>> deleteByLayer(@RequestParam String layer) {
        ExternalDataLayer parsed = parseLayer(layer);
        long deleted = monitoringService.deleteByLayer(parsed);
        return ResponseEntity.ok(Map.of(
                "message", "externalApiMonitoringLayerCleared",
                "deleted", deleted
        ));
    }

    private static ExternalDataLayer parseLayer(String layer) {
        if (layer == null || layer.isBlank()) {
            throw new BadRequestException("externalApiMonitoringLayerRequired");
        }
        try {
            return ExternalDataLayer.valueOf(layer.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("externalApiMonitoringLayerInvalid");
        }
    }
}
