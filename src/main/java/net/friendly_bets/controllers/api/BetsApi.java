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
import net.friendly_bets.models.BetResult;
import net.friendly_bets.security.details.AuthenticatedUser;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Map;

@Tags(value = {
        @Tag(name = "Bets")
})
public interface BetsApi {

    @Operation(summary = "Add a new bet", description = "Accessible only to moderators and administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "The new bet has been successfully created. The response contains the created bet object with all details.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BetDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data. The request may be missing required fields or contain invalid values.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated. Access is restricted to authenticated moderators and administrators.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<BetDto> addBet(
            @Parameter(hidden = true) AuthenticatedUser currentUser,
            @Parameter(description = "New bet") @Valid NewBetDto newBetDto);

    @Operation(summary = "Add an empty bet", description = "Accessible only to moderators and administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "The empty bet has been successfully created. The response contains the created bet object with default values.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BetDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data. The request may be missing required fields or contain invalid values.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated. Access is restricted to authenticated moderators and administrators.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<BetDto> addEmptyBet(
            @Parameter(hidden = true) AuthenticatedUser currentUser,
            @Parameter(description = "New empty bet") @Valid NewEmptyBet newEmptyBet);

    @Operation(summary = "Set bet result", description = "Accessible only to moderators and administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The result of the bet has been successfully updated. The response contains the updated bet object reflecting the new result and status.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BetDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data. The request may be missing required fields or contain invalid values.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated. Access is restricted to authenticated moderators and administrators.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<BetDto> setBetResult(
            @Parameter(hidden = true) AuthenticatedUser currentUser,
            @Parameter(description = "Bet ID") @NotBlank String betId,
            @Parameter(description = "Bet result and status") @Valid BetResult betResult);

    @Operation(summary = "Get list of all opened bets", description = "Accessible to everyone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all opened bets. The response contains a paginated list of bets that are currently open.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BetsPage.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data. The request may be missing required fields or contain invalid values.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<BetsPage> getOpenedBets(
            @Parameter(description = "Season ID") @NotBlank String seasonId);

    @Operation(summary = "Get list of all completed bets", description = "Accessible to everyone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all completed bets. The response contains a paginated list of bets that have been completed.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BetsPage.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data. The request may be missing required fields or contain invalid values.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<BetsPage> getCompletedBets(
            @Parameter(description = "Season ID") @NotBlank String seasonId,
            @Parameter(description = "Filter by player ID") String playerId,
            @Parameter(description = "Filter by league ID") String leagueId,
            @Parameter(description = "Page number") int page,
            @Parameter(description = "Page size") int size,
            @Parameter(description = "Sort field") String sortBy);

    @Operation(summary = "Get list of all bets (opened + completed)", description = "Accessible only to moderators and administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all bets, both opened and completed. The response contains a paginated list of bets according to the specified filters.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BetsPage.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data. The request may be missing required fields or contain invalid values.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated. Access is restricted to authenticated moderators and administrators.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<BetsPage> getAllBets(
            @Parameter(description = "Season ID") @NotBlank String seasonId,
            @Parameter(description = "Page number") int page,
            @Parameter(description = "Page size") int size,
            @Parameter(description = "Sort field") String sortBy);

    @Operation(summary = "Edit a bet", description = "Accessible only to moderators and administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The bet has been successfully edited. The response contains the updated bet object reflecting the changes.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BetDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data. The request may be missing required fields or contain invalid values.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated. Access is restricted to authenticated moderators and administrators.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<BetDto> editBet(
            @Parameter(hidden = true) AuthenticatedUser currentUser,
            @Parameter(description = "Edited bet ID") @NotBlank String editedBetId,
            @Parameter(description = "Edited bet details") @Valid EditedBetDto editedBet);

    @Operation(summary = "Delete a bet", description = "Accessible only to moderators and administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The bet has been successfully deleted. The response contains the deleted bet object with metadata if applicable.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BetDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data. The request may be missing required fields or contain invalid values.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated. Access is restricted to authenticated moderators and administrators.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<BetDto> deleteBet(
            @Parameter(hidden = true) AuthenticatedUser currentUser,
            @Parameter(description = "Bet ID") @NotBlank String betId,
            @Parameter(description = "Deleted bet metadata") @Valid DeletedBetDto deletedBetMetaData);

    @Operation(summary = "Get all bet title codes with labels", description = "Accessible only to moderators and administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The collection of code:label has been successfully loaded.",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "User not authenticated. Access is restricted to authenticated moderators and administrators.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<Map<Short, String>> getBetTitleCodeLabelMap();
}
