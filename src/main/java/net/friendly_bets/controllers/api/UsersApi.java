package net.friendly_bets.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import net.friendly_bets.dto.UpdatedEmailDto;
import net.friendly_bets.dto.UpdatedPasswordDto;
import net.friendly_bets.dto.UpdatedUsernameDto;
import net.friendly_bets.dto.UserDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;

@Tags(value = {
        @Tag(name = "Users")
})
public interface UsersApi {

    @Operation(summary = "Get your profile", description = "Accessible only to authenticated users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile information retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or does not have the necessary permissions",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<UserDto> getProfile(
            @Parameter(hidden = true) AuthenticatedUser currentUser);

    @Operation(summary = "Change profile email", description = "Accessible only to authenticated users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile email updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid email data provided; ensure the email format is correct and not already in use",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or does not have permission to change the email",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<UserDto> editEmail(
            @Parameter(hidden = true) AuthenticatedUser currentUser,
            @Parameter(description = "New email address for the user") @Valid UpdatedEmailDto updatedEmailDto);

    @Operation(summary = "Change profile password", description = "Accessible only to authenticated users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile password updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid password data provided; ensure the current password is correct and the new password meets the required criteria",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or does not have permission to change the password",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<UserDto> editPassword(
            @Parameter(hidden = true) AuthenticatedUser currentUser,
            @Parameter(description = "Current password and new password for the user") @Valid UpdatedPasswordDto updatedPasswordDto);

    @Operation(summary = "Change profile username", description = "Accessible only to authenticated users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile username updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid username data provided; ensure the username is not already in use and meets the required criteria",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or does not have permission to change the username",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<UserDto> editUsername(
            @Parameter(hidden = true) AuthenticatedUser currentUser,
            @Parameter(description = "New username for the user") @Valid UpdatedUsernameDto updatedUsernameDto);

}
