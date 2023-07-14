package net.friendly_bets.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import net.friendly_bets.dto.NewSeasonDto;
import net.friendly_bets.dto.SeasonDto;
import net.friendly_bets.dto.SeasonsPage;
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
    ResponseEntity<SeasonDto> addSeason(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                        NewSeasonDto newSeason);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Изменить статус сезона", description = "Доступно только администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новый сезон",
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
    ResponseEntity<SeasonDto> changeSeasonStatus(@Parameter(hidden = true)
                                                 AuthenticatedUser currentUser,
                                                 @Parameter(description = "название сезона")
                                                 String title,
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
    ResponseEntity<SeasonDto> getActiveSeason(@Parameter(hidden = true) AuthenticatedUser currentUser);

    // ------------------------------------------------------------------------------------------------------ //
}