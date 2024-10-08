package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.UsersApi;
import net.friendly_bets.dto.UpdatedEmailDto;
import net.friendly_bets.dto.UpdatedPasswordDto;
import net.friendly_bets.dto.UpdatedUsernameDto;
import net.friendly_bets.dto.UserDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.services.UsersService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UsersController implements UsersApi {

    private final UsersService usersService;

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my/profile")
    public ResponseEntity<UserDto> getProfile(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        String currentUserId = currentUser.getUser().getId();
        return ResponseEntity.ok(usersService.getProfile(currentUserId));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/my/profile/email")
    public ResponseEntity<UserDto> editEmail(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                             @RequestBody @Valid UpdatedEmailDto updatedEmailDto) {
        String currentUserId = currentUser.getUser().getId();
        return ResponseEntity.ok(usersService.editEmail(currentUserId, updatedEmailDto));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/my/profile/password")
    public ResponseEntity<UserDto> editPassword(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @RequestBody @Valid UpdatedPasswordDto updatedPasswordDto) {
        String currentUserId = currentUser.getUser().getId();
        return ResponseEntity.ok(usersService.editPassword(currentUserId, updatedPasswordDto));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/my/profile/username")
    public ResponseEntity<UserDto> editUsername(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @RequestBody @Valid UpdatedUsernameDto updatedUsernameDto) {
        String currentUserId = currentUser.getUser().getId();
        return ResponseEntity.ok(usersService.editUsername(currentUserId, updatedUsernameDto));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/my/profile/language")
    public ResponseEntity<UserDto> changeLanguage(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                  @RequestBody String language) {
        String currentUserId = currentUser.getUser().getId();
        return ResponseEntity.ok(usersService.changeLanguage(currentUserId, language));
    }
}
