package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.AuthApi;
import net.friendly_bets.dto.EmailRequestDto;
import net.friendly_bets.dto.ResetPasswordDto;
import net.friendly_bets.dto.StandardResponseDto;
import net.friendly_bets.services.EmailVerificationService;
import net.friendly_bets.services.PasswordResetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController implements AuthApi {

    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    @Override
    @GetMapping("/confirm-email")
    public ResponseEntity<StandardResponseDto> confirmEmail(@RequestParam String token) {
        emailVerificationService.confirmEmail(token);
        return ResponseEntity.ok(StandardResponseDto.builder()
                .status(HttpStatus.OK.value())
                .message("emailConfirmed")
                .build());
    }

    @Override
    @PostMapping("/forgot-password")
    public ResponseEntity<StandardResponseDto> forgotPassword(@RequestBody @Valid EmailRequestDto request) {
        passwordResetService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(StandardResponseDto.builder()
                .status(HttpStatus.OK.value())
                .message("passwordResetEmailSentIfApplicable")
                .build());
    }

    @Override
    @PostMapping("/reset-password")
    public ResponseEntity<StandardResponseDto> resetPassword(@RequestBody @Valid ResetPasswordDto request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(StandardResponseDto.builder()
                .status(HttpStatus.OK.value())
                .message("passwordResetSuccess")
                .build());
    }

    @Override
    @PostMapping("/resend-verification")
    public ResponseEntity<StandardResponseDto> resendVerification(@RequestBody @Valid EmailRequestDto request) {
        emailVerificationService.resendVerification(request.getEmail());
        return ResponseEntity.ok(StandardResponseDto.builder()
                .status(HttpStatus.OK.value())
                .message("verificationEmailSentIfApplicable")
                .build());
    }
}
