package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ClientVersionDto;
import net.friendly_bets.dto.SetClientVersionDto;
import net.friendly_bets.exceptions.ForbiddenException;
import net.friendly_bets.services.ClientVersionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client-version")
public class ClientVersionController {

    private final ClientVersionService clientVersionService;

    @Value("${app.deploy.token:}")
    private String deployToken;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<ClientVersionDto> getCurrent() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(clientVersionService.getCurrent());
    }

    /**
     * CI deploy only. Requires {@code X-Deploy-Token} matching {@code app.deploy.token}.
     */
    @PutMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<ClientVersionDto> setFromDeploy(
            @RequestHeader(value = "X-Deploy-Token", required = false) String token,
            @RequestBody @Valid SetClientVersionDto dto
    ) {
        if (!StringUtils.hasText(deployToken) || !deployToken.equals(token)) {
            throw new ForbiddenException("accessDenied");
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(clientVersionService.setIfNewer(dto.getBuildId()));
    }
}
