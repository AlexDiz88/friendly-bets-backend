package net.friendly_bets.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import net.friendly_bets.dto.NewUserDto;
import net.friendly_bets.dto.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tags(value = {
        @Tag(name = "Users")
})
@CrossOrigin(origins = {"*", "http://localhost:3000", "https://friendly-bets.net", "https://www.friendly-bets.net", "http://friendly-bets.net", "http://www.friendly-bets.net", "https://friendly-bets.up.railway.app", "https://alexdiz88.github.io/" })
public interface SignUpApi {

    @Operation(summary = "Регистрация пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Зарегистрированный пользователь",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserDto.class))
                    }
            )
    })
    ResponseEntity<UserDto> signUp(@RequestBody NewUserDto newUser);
}
