package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ClientVersionDto;
import net.friendly_bets.dto.RegisterClientVersionDto;
import net.friendly_bets.services.ClientVersionService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client-version")
public class ClientVersionController {

    private final ClientVersionService clientVersionService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<ClientVersionDto> getCurrent() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(clientVersionService.getCurrent());
    }

    @PostMapping("/register")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ClientVersionDto> register(@RequestBody @Valid RegisterClientVersionDto dto) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(clientVersionService.registerIfNewer(dto.getBuildId()));
    }
}
