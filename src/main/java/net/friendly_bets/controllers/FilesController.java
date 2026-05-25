package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.FilesApi;
import net.friendly_bets.dto.StandardResponseDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.services.FilesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/files")
public class FilesController implements FilesApi {

    private final FilesService filesService;

    @Override
    @PreAuthorize("hasAuthority('USER') || hasAuthority('MODERATOR') || hasAuthority('ADMIN')")
    @PostMapping("/upload/avatars")
    public ResponseEntity<StandardResponseDto> saveAvatarImage(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam("file") MultipartFile file
    ) {
        String currentUserId = authenticatedUser.getUser().getId();
        return ResponseEntity.ok(filesService.saveAvatarImage(currentUserId, file));
    }

}
