package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.FilesApi;
import net.friendly_bets.dto.ImageDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.services.FilesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/files/upload")
public class FilesController implements FilesApi {

    private final FilesService filesService;

    @PreAuthorize("hasAuthority('USER') || hasAuthority('MODERATOR')")
    @PostMapping("/avatars")
    public ResponseEntity<ImageDto> saveAvatarImage(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                    @RequestParam("image") MultipartFile image) {
        String currentUserId = authenticatedUser.getUser().getId();
        return ResponseEntity.status(201)
                .body(filesService.saveAvatarImage(currentUserId, image));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/logo/{team-id}")
    public ResponseEntity<ImageDto> saveLogoImage(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                  @PathVariable("team-id") String teamId,
                                                  @RequestParam("image") MultipartFile image) {
        return ResponseEntity.status(201)
                .body(filesService.saveLogoImage(teamId, image));
    }
}
