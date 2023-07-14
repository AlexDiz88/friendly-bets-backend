package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.UsersApi;
import net.friendly_bets.dto.UserDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.services.UsersService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        UserDto profile = usersService.getProfile(currentUserId);
        return ResponseEntity.ok(profile);
    }
}
