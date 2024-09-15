package net.friendly_bets.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import net.friendly_bets.dto.NewTeamDto;
import net.friendly_bets.dto.TeamDto;
import net.friendly_bets.dto.TeamsPage;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Tags(value = {
        @Tag(name = "Teams")
})
public interface TeamsApi {

    @Operation(summary = "Get a list of all teams", description = "Accessible only to administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all teams in the system",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamsPage.class))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or not authorized to access this resource",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<TeamsPage> getTeams();

    @Operation(summary = "Get a list of league teams", description = "Accessible only to administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of teams in the specified league",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamsPage.class))),
            @ApiResponse(responseCode = "400", description = "Invalid league ID provided",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or not authorized to access this resource",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "404", description = "League not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<TeamsPage> getLeagueTeams(
            @Parameter(description = "League ID") @NotBlank String leagueId);

    @Operation(summary = "Add a new team", description = "Accessible only to administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Newly created team",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid team data provided",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated or not authorized to perform this action",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<TeamDto> createTeam(
            @Parameter(description = "Details of the new team to be created") @Valid NewTeamDto newTeam);

}
