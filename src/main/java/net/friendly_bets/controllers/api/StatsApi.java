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

@Tags(value = {
        @Tag(name = "Player Stats")
})
public interface StatsApi {

    @Operation(summary = "Общая статистика всех участников турнира в сезоне", description = "Доступно всем")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Общая статистика всех участников турнира в сезоне",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AllPlayersStatsPage.class))
                    }
            ),
    })
    ResponseEntity<AllPlayersStatsPage> getAllPlayersStatsBySeason(@Parameter(description = "ID сезона") String seasonId);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Получить список статистики всех участников сезона по лигам", description = "Доступно всем")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статистика всех участников по лигам",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AllPlayersStatsByLeaguesDto.class))
                    }
            ),
    })
    ResponseEntity<AllPlayersStatsByLeaguesDto> getAllPlayersStatsByLeagues(@Parameter(description = "ID сезона") String seasonId);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Статистика всех участников турнира в сезоне по командам", description = "Доступно всем")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статистика всех участников турнира в сезоне по командам",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AllStatsByTeamsInSeasonDto.class))
                    }
            ),
    })
    ResponseEntity<AllStatsByTeamsInSeasonDto> getAllStatsByTeamsInSeason(@Parameter(description = "ID сезона") String seasonId);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Статистика по командам", description = "Доступно всем")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статистика по командам",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PlayerStatsByTeamsDto.class))
                    }
            ),
    })
    ResponseEntity<StatsByTeamsDto> getStatsByTeams(@Parameter(description = "ID сезона") String seasonId,
                                                    @Parameter(description = "ID лиги") String leagueId,
                                                    @Parameter(description = "ID игрока") String userId);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Полный пересчет всей статистики игроков по сезону", description = "Доступно только администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пересчитанная статистика всех игроков в сезоне",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AllPlayersStatsPage.class))
                    }
            ),
    })
    @ApiResponse(responseCode = "403", description = "userNotAuthenticated",
            content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(ref = "StandardResponseDto"))
            }
    )
    ResponseEntity<AllPlayersStatsPage> playersStatsFullRecalculation(@Parameter(description = "ID сезона") String seasonId);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Полный пересчет всей статистики игроков по командам в сезоне", description = "Доступно только администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пересчитанная статистика всех игроков по командам в сезоне"),
    })
    @ApiResponse(responseCode = "403", description = "userNotAuthenticated",
            content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(ref = "StandardResponseDto"))
            }
    )
    ResponseEntity<Void> playersStatsByTeamsRecalculation(@Parameter(description = "ID сезона") String seasonId);

    // ------------------------------------------------------------------------------------------------------ //

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пересчитанная статистика всех игровых туров календаря"),
    })
    @ApiResponse(responseCode = "403", description = "userNotAuthenticated",
            content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(ref = "StandardResponseDto"))
            }
    )
    ResponseEntity<Void> recalculateAllGameweekStats(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                                     @Parameter(description = "ID сезона") String seasonId);

    // ------------------------------------------------------------------------------------------------------ //


}