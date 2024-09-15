package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.SignUpApi;
import net.friendly_bets.dto.NewUserDto;
import net.friendly_bets.dto.UserDto;
import net.friendly_bets.services.SignUpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/register")
public class SignUpController implements SignUpApi {

    private final SignUpService signUpService;

    @Override
    @PostMapping
    public ResponseEntity<UserDto> signUp(@RequestBody @Valid NewUserDto newUser) {
        return ResponseEntity
                .status(201)
                .body(signUpService.signUp(newUser));
    }
}
