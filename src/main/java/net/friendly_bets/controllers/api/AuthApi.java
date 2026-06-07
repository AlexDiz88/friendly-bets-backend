package net.friendly_bets.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import net.friendly_bets.dto.EmailRequestDto;
import net.friendly_bets.dto.ResetPasswordDto;
import net.friendly_bets.dto.StandardResponseDto;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;

@Tags(value = {
        @Tag(name = "Auth")
})
public interface AuthApi {

    @Operation(summary = "Confirm email by token from message")
    ResponseEntity<StandardResponseDto> confirmEmail(String token);

    @Operation(summary = "Request password reset email (confirmed emails only; generic response)")
    ResponseEntity<StandardResponseDto> forgotPassword(@Valid EmailRequestDto request);

    @Operation(summary = "Reset password using token from email")
    ResponseEntity<StandardResponseDto> resetPassword(@Valid ResetPasswordDto request);

    @Operation(summary = "Resend email verification link")
    ResponseEntity<StandardResponseDto> resendVerification(@Valid EmailRequestDto request);
}
