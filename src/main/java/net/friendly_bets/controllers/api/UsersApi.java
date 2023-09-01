package net.friendly_bets.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import net.friendly_bets.dto.*;
import net.friendly_bets.security.details.AuthenticatedUser;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;

@Tags(value = {
        @Tag(name = "Users")
})
public interface UsersApi {

    @Operation(summary = "Получение своего профиля", description = "Доступно только аутентифицированному пользователю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация о профиле",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<UserDto> getProfile(@Parameter(hidden = true) AuthenticatedUser currentUser);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Изменить почту профиля", description = "Доступно только аутентифицированному пользователю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новая почта профиля",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<UserDto> editEmail(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                      @Parameter(description = "новая почта пользователя") @Valid UpdatedEmailDto updatedEmailDto);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Изменить пароль профиля", description = "Доступно только аутентифицированному пользователю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новый пароль профиля",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<UserDto> editPassword(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                         @Parameter(description = "текущий и новый пароль пользователя") @Valid UpdatedPasswordDto updatedPasswordDto);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Изменить имя профиля", description = "Доступно только аутентифицированному пользователю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новое имя профиля",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<UserDto> editUsername(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                         @Parameter(description = "новое имя пользователя") @Valid UpdatedUsernameDto updatedUsernameDto);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Получить список статистики всех участников сезона", description = "Доступно всем")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статистика всех участников",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PlayersStatsPage.class))
                    }
            ),
    })
    ResponseEntity<PlayersStatsPage> getPlayersStatsBySeason(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                                             @Parameter(description = "ID сезона") String seasonId);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Получить список статистики всех участников сезона по лигам", description = "Доступно всем")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статистика всех участников по лигам",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PlayersStatsByLeaguesPage.class))
                    }
            ),
    })
    ResponseEntity<PlayersStatsByLeaguesPage> getPlayersStatsByLeagues(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                                                       @Parameter(description = "ID сезона") String seasonId);

}