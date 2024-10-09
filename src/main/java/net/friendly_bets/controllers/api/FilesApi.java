package net.friendly_bets.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import net.friendly_bets.dto.StandardResponseDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import java.io.IOException;

@Tags(value = {
        @Tag(name = "Files")
})
public interface FilesApi {

    @Operation(summary = "Upload a user avatar to S3", description = "Accessible to users with 'USER', 'MODERATOR', or 'ADMIN' roles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avatar successfully uploaded",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file data or upload failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or does not have the required permissions",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<StandardResponseDto> uploadUserAvatar(
            @Parameter(description = "Authenticated user details") AuthenticatedUser authenticatedUser,
            @Parameter(description = "Avatar file to upload") MultipartFile file) throws IOException;

    @Operation(summary = "Download a file from S3", description = "Accessible to users with 'ADMIN' or 'MODERATOR' roles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File successfully downloaded",
                    content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "404", description = "File not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or does not have the required permissions",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "Name of the file to download") @NotBlank String filename);

    @Operation(summary = "Save a user avatar image", description = "Accessible to users with 'USER', 'MODERATOR', or 'ADMIN' roles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avatar image successfully saved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file data or save failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or does not have the required permissions",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<StandardResponseDto> saveAvatarImage(
            @Parameter(hidden = true) AuthenticatedUser authenticatedUser,
            @Parameter(description = "Avatar file to save") MultipartFile file);

}
