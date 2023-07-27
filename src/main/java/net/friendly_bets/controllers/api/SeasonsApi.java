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

import java.util.List;

@Tags(value = {
        @Tag(name = "Seasons")
})

public interface SeasonsApi {

    @Operation(summary = "Получение списка всех сезонов", description = "Доступно только аутентифицированному пользователю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информацию о сезонах",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SeasonsPage.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<SeasonsPage> getSeasons(@Parameter(hidden = true) AuthenticatedUser currentUser);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Добавить новый сезон", description = "Доступно только администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новый сезон",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SeasonDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<SeasonDto> addSeason(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                        @Parameter(description = "новый сезон")
                                        NewSeasonDto newSeason);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Изменить статус сезона", description = "Доступно только администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новый сезон",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SeasonDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<SeasonDto> changeSeasonStatus(@Parameter(hidden = true)
                                                 AuthenticatedUser currentUser,
                                                 @Parameter(description = "ID сезона")
                                                 String id,
                                                 @Parameter(description = "статус сезона")
                                                 String status);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Получение списка всех статусов сезона", description = "Доступно только администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информацию о статусах сезона",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = List.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<List<String>> getSeasonStatusList(@Parameter(hidden = true) AuthenticatedUser currentUser);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Получить текущий (активный) сезон", description = "Доступно только аутентифицированному пользователю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Текущий сезон",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SeasonDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<SeasonDto> getActiveSeason(@Parameter(hidden = true) AuthenticatedUser currentUser);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Получить запланированный (открытый для регистрации) сезон", description = "Доступно только аутентифицированному пользователю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запланированный сезон",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SeasonDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<SeasonDto> getScheduledSeason(@Parameter(hidden = true) AuthenticatedUser currentUser);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Регистрация пользователя в запланированном сезоне", description = "Доступно только аутентифицированному пользователю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Регистрация в запланированном сезоне",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SeasonDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<SeasonDto> registrationInSeason(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                                   @Parameter(description = "ID сезона") String seasonId);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Список футбольных лиг сезона", description = "Доступно только аутентифицированному пользователю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Футбольные лиги",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = LeaguesPage.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<LeaguesPage> getLeaguesBySeason(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                                   @Parameter(description = "ID сезона") String seasonId);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Добавить футбольную лигу в сезон", description = "Доступно только администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новая футбольная лига",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SeasonDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<SeasonDto> addLeagueToSeason(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                                  @Parameter(description = "ID сезона") String seasonId,
                                                @Parameter(description = "новая лига") NewLeagueDto newLeague);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Добавить команду в лигу сезона", description = "Доступно только администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Команда в лиге",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SeasonDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<SeasonDto> addTeamToLeagueInSeason(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                                @Parameter(description = "ID сезона") String seasonId,
                                                @Parameter(description = "ID лиги") String leagueId,
                                                @Parameter(description = "ID команды") String teamId);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Добавить новую ставку в лигу сезона", description = "Доступно только модератору и администратору (до реализации автоматического приёма ставок)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новая ставка",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SeasonDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<SeasonDto> addBetToLeagueInSeason(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                                     @Parameter(description = "ID сезона") String seasonId,
                                                     @Parameter(description = "ID лиги") String leagueId,
                                                     @Parameter(description = "новая ставка") NewBetDto newBet);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Добавить пустую ставку в лигу сезона", description = "Доступно только модератору и администратору (до реализации автоматического приёма ставок)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новая пустая ставка",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SeasonDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<SeasonDto> addEmptyBetToLeagueInSeason(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                                     @Parameter(description = "ID сезона") String seasonId,
                                                     @Parameter(description = "ID лиги") String leagueId,
                                                     @Parameter(description = "новая пустая ставка") NewEmptyBetDto newEmptyBet);

    // ------------------------------------------------------------------------------------------------------ //
}