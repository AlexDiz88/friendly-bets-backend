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
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

@Tags(value = {
        @Tag(name = "Seasons")
})
public interface SeasonsApi {

    @Operation(summary = "Get all seasons", description = "Available to authenticated users only")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved seasons",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SeasonsPage.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<SeasonsPage> getSeasons();

    @Operation(summary = "Add new season", description = "Available to admin only")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created a new season",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SeasonDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, invalid season data",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or not authorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<SeasonDto> addSeason(
            @Parameter(description = "New season") @Valid NewSeasonDto newSeason);

    @Operation(summary = "Change season status", description = "Available to admin only")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated season status",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SeasonDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, invalid season ID or status",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or not authorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<SeasonDto> changeSeasonStatus(
            @Parameter(description = "Season ID") @NotBlank String id,
            @Parameter(description = "Season status") @NotBlank String status);

    @Operation(summary = "Get all season statuses", description = "Available to admin only")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved season statuses",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or not authorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<List<String>> getSeasonStatusList();

    @Operation(summary = "Get all league codes", description = "Available to admin only")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved league codes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or not authorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<List<String>> getLeagueCodeList();

    @Operation(summary = "Get current (active) season", description = "Available to everyone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved current season",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SeasonDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, unable to retrieve current season",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<SeasonDto> getActiveSeason();

    @Operation(summary = "Get ID of current (active) season", description = "Available to everyone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved ID of current season",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ActiveSeasonIdDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, unable to retrieve season ID",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<ActiveSeasonIdDto> getActiveSeasonId();

    @Operation(summary = "Get scheduled (open for registration) season", description = "Available to authenticated users only")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved scheduled season",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SeasonDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, unable to retrieve scheduled season",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<SeasonDto> getScheduledSeason();

    @Operation(summary = "Register user in scheduled season", description = "Available to authenticated users only")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully registered user in scheduled season",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SeasonDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, invalid season ID or user registration issue",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<SeasonDto> registrationInSeason(
            @Parameter(hidden = true) AuthenticatedUser currentUser,
            @Parameter(description = "Season ID") @NotBlank String seasonId);

    @Operation(summary = "List of football leagues for the season", description = "Available to authenticated users only")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of football leagues",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LeaguesPage.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, invalid season ID",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<LeaguesPage> getLeaguesBySeason(
            @Parameter(description = "Season ID") @NotBlank String seasonId);

    @Operation(summary = "Add football league to season", description = "Available to admin only")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added a new football league to season",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SeasonDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, invalid season ID or league data",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or not authorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<SeasonDto> addLeagueToSeason(
            @Parameter(description = "Season ID") @NotBlank String seasonId,
            @Parameter(description = "New league") @Valid NewLeagueDto newLeague);

    @Operation(summary = "Add team to league in season", description = "Available to admin only")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added a team to the league in season",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, invalid season ID, league ID, or team ID",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or not authorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<TeamDto> addTeamToLeagueInSeason(
            @Parameter(description = "Season ID") @NotBlank String seasonId,
            @Parameter(description = "League ID") @NotBlank String leagueId,
            @Parameter(description = "Team ID") @NotBlank String teamId);

    @Operation(summary = "DB update", description = "Available to admin only")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated the database",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, database update issue",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or not authorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<Map<String, Object>> dbUpdate();
}
