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
import org.springframework.http.ResponseEntity;

import javax.validation.constraints.NotBlank;

@Tags(value = {
        @Tag(name = "Player Stats")
})
public interface StatsApi {

    @Operation(summary = "Get overall statistics of all tournament participants in a season", description = "Accessible to everyone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Overall statistics of all participants in the season",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AllPlayersStatsPage.class))),
            @ApiResponse(responseCode = "400", description = "Invalid season ID provided",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "404", description = "Season not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<AllPlayersStatsPage> getAllPlayersStatsBySeason(
            @Parameter(description = "Season ID") @NotBlank String seasonId);

    @Operation(summary = "Get list of statistics for all participants in the season by leagues", description = "Accessible to everyone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics of all participants by leagues",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AllPlayersStatsByLeaguesDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid season ID provided",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "404", description = "Season not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<AllPlayersStatsByLeaguesDto> getAllPlayersStatsByLeagues(
            @Parameter(description = "Season ID") @NotBlank String seasonId);

    @Operation(summary = "Statistics of all tournament participants in a season by teams", description = "Accessible to everyone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics of all participants in the season by teams",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AllStatsByTeamsInSeasonDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid season ID provided",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "404", description = "Season not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<AllStatsByTeamsInSeasonDto> getAllStatsByTeamsInSeason(
            @Parameter(description = "Season ID") @NotBlank String seasonId);

    @Operation(summary = "Statistics of all tournament participants in a season by bet titles", description = "Accessible to everyone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics of all participants in the season by bet titles",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AllStatsByBetTitlesInSeasonDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid season ID provided",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "404", description = "Season not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<AllStatsByBetTitlesInSeasonDto> getAllStatsByBetTitlesInSeason(
            @Parameter(description = "Season ID") @NotBlank String seasonId);

    @Operation(summary = "Team statistics", description = "Accessible to everyone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics by teams",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlayerStatsByTeamsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid season, league, or player ID provided",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "404", description = "Season, league, or player not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<StatsByTeamsDto> getStatsByTeams(
            @Parameter(description = "Season ID") @NotBlank String seasonId,
            @Parameter(description = "League ID") @NotBlank String leagueId,
            @Parameter(description = "Player ID") @NotBlank String userId);

    @Operation(summary = "Full recalculation of all player statistics for a season", description = "Accessible only to administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recalculated statistics of all players in the season",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AllPlayersStatsPage.class))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or not authorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "404", description = "Season not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<AllPlayersStatsPage> playersStatsFullRecalculation(
            @Parameter(description = "Season ID") @NotBlank String seasonId);

    @Operation(summary = "Full recalculation of team player statistics for a season", description = "Accessible only to administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recalculated statistics of all players by teams in the season"),
            @ApiResponse(responseCode = "403", description = "User not authenticated or not authorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "404", description = "Season not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<Void> playersStatsByTeamsRecalculation(
            @Parameter(description = "Season ID") @NotBlank String seasonId);

    @Operation(summary = "Recalculate all gameweek statistics", description = "Accessible only to administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recalculated statistics of all gameweeks"),
            @ApiResponse(responseCode = "403", description = "User not authenticated or not authorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "404", description = "Season not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<Void> recalculateAllGameweekStats(
            @Parameter(description = "Season ID") @NotBlank String seasonId);

}
