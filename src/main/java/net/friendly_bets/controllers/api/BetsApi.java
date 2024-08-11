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
        @Tag(name = "Bets")
})
public interface BetsApi {

    @Operation(summary = "Добавить новую ставку", description = "Доступно только модератору и администратору (до реализации автоматического приёма ставок)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Новая ставка",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BetDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "userNotAuthenticated",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<BetDto> addBet(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                  @Parameter(description = "новая ставка") @Valid NewBetDto newBet);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Добавить пустую ставку", description = "Доступно только модератору и администратору (до реализации автоматического приёма ставок)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Новая пустая ставка",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BetDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "userNotAuthenticated",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<BetDto> addEmptyBet(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                       @Parameter(description = "новая пустая ставка") @Valid NewEmptyBetDto newEmptyBet);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Результат ставки (по итогам проверки)", description = "Доступно только модератору и администратору (до реализации автоматического приёма ставок)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новый статус и результат ставки",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BetDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "userNotAuthenticated",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<BetDto> setBetResult(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                        @Parameter(description = "ID ставки") String betId,
                                        @Parameter(description = "результат матча и статус ставки") @Valid NewBetResult newBetResult);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Получить список всех открытых(OPENED) ставок", description = "Доступно всем")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список всех открытых(OPENED) ставок",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BetsPage.class))
                    }
            ),
    })
    ResponseEntity<BetsPage> getOpenedBets(@Parameter(description = "ID сезона") String seasonId);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Получить список всех завершенных(COMPLETED) ставок", description = "Доступно всем")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список всех завершенных(COMPLETED) ставок",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BetsPage.class))
                    }
            ),
    })
    ResponseEntity<BetsPage> getCompletedBets(@Parameter(description = "ID сезона") String seasonId,
                                              @Parameter(description = "параметр фильтрации по игроку") String playerId,
                                              @Parameter(description = "параметр фильтрации по лиге") String leagueId,
                                              @Parameter(description = "страница запроса") int page,
                                              @Parameter(description = "количество элементов запроса") int size,
                                              @Parameter(description = "поле сортировки") String sortBy);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Получить список всех ставок (OPENED + COMPLETED)", description = "Доступно только модератору и администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список всех ставок (OPENED + COMPLETED)",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BetsPage.class))
                    }
            ),
    })
    ResponseEntity<BetsPage> getAllBets(@Parameter(description = "ID сезона") String seasonId,
                                        @Parameter(description = "страница запроса") int page,
                                        @Parameter(description = "количество элементов запроса") int size,
                                        @Parameter(description = "поле сортировки") String sortBy);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Отредактировать ставку", description = "Доступно только администратору и модератору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Отредактированная ставка",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BetDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "userNotAuthenticated",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<BetDto> editBet(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                   @Parameter(description = "ID ставки") String betId,
                                   @Valid EditedBetDto editedBet);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Аннулировать ставку", description = "Доступно только администратору и модератору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аннулированная ставка",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BetDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "userNotAuthenticated",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<BetDto> deleteBet(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                     @Parameter(description = "ID ставки") String betId,
                                     @Valid DeletedBetDto deletedBetMetaData);

    // ------------------------------------------------------------------------------------------------------ //


}