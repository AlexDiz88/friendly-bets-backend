package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.FilesApi;
import net.friendly_bets.dto.ImageDto;
import net.friendly_bets.dto.StandardResponseDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.services.FilesService;
import net.friendly_bets.services.impl.S3Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/files")
public class FilesController implements FilesApi {

    private final S3Service s3Service;
    private final FilesService filesService;

    @PreAuthorize("hasAuthority('USER') || hasAuthority('MODERATOR') || hasAuthority('ADMIN')")
    @PostMapping("/upload/s3/avatars")
    public ResponseEntity<StandardResponseDto> uploadUserAvatar(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                                @RequestParam("file") MultipartFile file) throws IOException {
        String currentUserId = authenticatedUser.getUser().getId();
        filesService.s3uploadUserAvatar(currentUserId, file);
        return ResponseEntity.ok(StandardResponseDto.builder()
                .status(HttpStatus.OK.value())
                .message("avatarWasSuccessfullyUploaded")
                .build());
    }

    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String filename) {
        byte[] fileData = s3Service.downloadFile(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(fileData);
    }

    @PreAuthorize("hasAuthority('USER') || hasAuthority('MODERATOR') || hasAuthority('ADMIN')")
    @PostMapping("/upload/avatars")
    public ResponseEntity<StandardResponseDto> saveAvatarImage(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                               @RequestParam("file") MultipartFile file) {
        String currentUserId = authenticatedUser.getUser().getId();
        return ResponseEntity.ok()
                .body(filesService.saveAvatarImage(currentUserId, file));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/upload/logo/{team-id}")
    public ResponseEntity<ImageDto> saveLogoImage(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                  @PathVariable("team-id") String teamId,
                                                  @RequestParam("image") MultipartFile image) {
        return ResponseEntity.status(201)
                .body(filesService.saveLogoImage(teamId, image));
    }
}
