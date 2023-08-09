package net.friendly_bets.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import net.friendly_bets.dto.BetDto;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.security.details.AuthenticatedUser;
import org.springframework.http.ResponseEntity;

@Tags(value = {
        @Tag(name = "Bets")
})
public interface BetsApi {

    @Operation(summary = "Получение списка всех ставок", description = "Доступно только аутентифицированному пользователю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация о ставках",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BetsPage.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<BetsPage> getAllBets(@Parameter(hidden = true) AuthenticatedUser currentUser);

    // ------------------------------------------------------------------------------------------------------ //

    @Operation(summary = "Удалить ставку", description = "Доступно только администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Удаленная ставка",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BetDto.class))
                    }
            ),
            @ApiResponse(responseCode = "403", description = "Пользователь не аутентифицирован",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(ref = "StandardResponseDto"))
                    }
            )
    })
    ResponseEntity<BetDto> deleteBet(@Parameter(hidden = true) AuthenticatedUser currentUser,
                                     String betId);

    // ------------------------------------------------------------------------------------------------------ //


}