package net.friendly_bets.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import net.friendly_bets.dto.AllPlayersStatsByLeaguesDto;
import net.friendly_bets.dto.AllPlayersStatsDto;
import org.springframework.http.ResponseEntity;

@Tags(value = {
        @Tag(name = "Player Stats")
})
public interface PlayerStatsApi {

    @Operation(summary = "Общая статистика всех участников турнира в сезоне", description = "Доступно всем")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Общая статистика всех участников турнира в сезоне",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AllPlayersStatsDto.class))
                    }
            ),
    })
    ResponseEntity<AllPlayersStatsDto> getAllPlayersStatsBySeason(@Parameter(description = "ID сезона") String seasonId);

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
}