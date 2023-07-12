package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.UsersApi;
import net.friendly_bets.dto.UserDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.services.UsersService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UsersController implements UsersApi {

    private final UsersService usersService;


    @PreAuthorize("isAuthenticated()")
    @Override
    public ResponseEntity<UserDto> getProfile(AuthenticatedUser currentUser) {
        String currentUserId = currentUser.getUser().getId();
        UserDto profile = usersService.getProfile(currentUserId);

        return ResponseEntity.ok(profile);
    }
}
