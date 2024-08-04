package net.friendly_bets.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.dto.CalendarNodeDto;
import net.friendly_bets.dto.CalendarNodesPage;
import net.friendly_bets.dto.NewCalendarNodeDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;

@Tags(value = {
        @Tag(name = "Calendars")
})
public interface CalendarsApi {

    @Operation(summary = "Получение календаря всех туров сезона", description = "Доступно только модератору и администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Календарь всех туров сезона",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = CalendarNodesPage.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<CalendarNodesPage> getAllSeasonCalendarNodes(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                                                @Parameter(description = "ID сезона") String seasonId);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Добавить новую запись календаря", description = "Доступно только модератору и администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Новая запись календаря",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = CalendarNodeDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<CalendarNodeDto> createCalendarNode(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                                       @Valid NewCalendarNodeDto newCalendarNode);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Добавить ставку в запись календаря", description = "Доступно только модератору и администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Обновленная запись календаря",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = CalendarNodeDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<CalendarNodeDto> addBetToCalendarNode(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                                         @Parameter(description = "ID ставки") String betId,
                                                         @Parameter(description = "ID записи календаря") String calendarNodeId);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Получить список ставок из записи календаря", description = "Доступно всем")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список ставок из записи календаря",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BetsPage.class))
                    }
            ),
    })
    ResponseEntity<BetsPage> getBetsByCalendarNode(@Parameter(description = "ID записи календаря") String calendarNodeId);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Удалить запись из календаря", description = "Доступно только модератору и администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Удалённая запись календаря",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = CalendarNodeDto.class))
                    }
            ),
    })
    ResponseEntity<CalendarNodeDto> deleteCalendarNode(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                                       @Parameter(description = "ID записи календаря") String calendarNodeId);

    // ------------------------------------------------------------------------------------------------------ //


}