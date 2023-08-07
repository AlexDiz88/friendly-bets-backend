package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.UsersApi;
import net.friendly_bets.dto.PlayersStatsPage;
import net.friendly_bets.dto.UserDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.services.UsersService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
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

    @Override
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/my/profile/email")
    public ResponseEntity<UserDto> editEmail(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                             @RequestBody String newEmail) {
        String currentUserId = currentUser.getUser().getId();
        return ResponseEntity
                .ok(usersService.editEmail(currentUserId, newEmail));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/my/profile/username")
    public ResponseEntity<UserDto> editUsername(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @RequestBody String newUsername) {
        String currentUserId = currentUser.getUser().getId();
        return ResponseEntity
                .ok(usersService.editUsername(currentUserId, newUsername));
    }

    @Override
    @GetMapping("/season/{season-id}/stats")
    public ResponseEntity<PlayersStatsPage> getPlayersStatsBySeason(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                                    @PathVariable("season-id") String seasonId) {
        return ResponseEntity.status(200)
                .body(usersService.getPlayersStatsBySeason(seasonId));
    }
}
