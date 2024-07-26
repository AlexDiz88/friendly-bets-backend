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
import net.friendly_bets.security.details.AuthenticatedUser;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;

@Tags(value = {
        @Tag(name = "Teams")
})
public interface TeamsApi {

    @Operation(summary = "Получение списка всех команд", description = "Доступно только администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список команд",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TeamsPage.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<TeamsPage> getTeams(@Parameter(hidden = true) AuthenticatedUser currentUser);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Получение списка команд лиги", description = "Доступно только администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список команд лиги",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TeamsPage.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<TeamsPage> getLeagueTeams(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                             @Parameter(description = "ID лиги") String leagueId);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Добавить новую команду", description = "Доступно только администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новая команда",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TeamDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<TeamDto> createTeam(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                       @Valid NewTeamDto newTeam);

    // ------------------------------------------------------------------------------------------------------ //


}