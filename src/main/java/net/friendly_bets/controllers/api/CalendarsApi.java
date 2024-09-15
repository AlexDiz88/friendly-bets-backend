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
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Tags(value = {
        @Tag(name = "Calendars")
})
public interface CalendarsApi {

    @Operation(summary = "Get all season rounds calendar", description = "Accessible to everyone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all season rounds for the specified season. The response contains a paginated list of calendar nodes representing each round in the season.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CalendarNodesPage.class))),
            @ApiResponse(responseCode = "403", description = "User not authenticated. Access is restricted to authenticated users.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<CalendarNodesPage> getAllSeasonCalendarNodes(
            @Parameter(description = "Season ID") @NotBlank String seasonId);

    @Operation(summary = "Get season rounds with bets", description = "Accessible to everyone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of season rounds that have associated bets. The response contains a paginated list of calendar nodes with bets for the specified season.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CalendarNodesPage.class))),
            @ApiResponse(responseCode = "403", description = "User not authenticated. Access is restricted to authenticated users.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<CalendarNodesPage> getSeasonCalendarHasBetsNodes(
            @Parameter(description = "Season ID") @NotBlank String seasonId);

    @Operation(summary = "Get current round calendar", description = "Accessible to everyone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of bets for the current calendar round. The response contains a paginated list of bets that are active for the current round.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BetsPage.class))),
            @ApiResponse(responseCode = "403", description = "User not authenticated. Access is restricted to authenticated users.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<BetsPage> getActualCalendarNodeBets(
            @Parameter(description = "Season ID") @NotBlank String seasonId);

    @Operation(summary = "Add new calendar entry", description = "Accessible only to moderators and administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "New calendar entry created successfully. The response contains the details of the newly created calendar entry.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CalendarNodeDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid details for the calendar entry. The request may be missing required fields or contain invalid values.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated. Access is restricted to authenticated moderators and administrators.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<CalendarNodeDto> createCalendarNode(
            @Parameter(description = "New calendar entry details") @Valid NewCalendarNodeDto newCalendarNode);

    @Operation(summary = "Get bets by calendar entry", description = "Accessible to everyone")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of bets associated with the specified calendar entry. The response contains a paginated list of bets for the given calendar entry.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BetsPage.class))),
            @ApiResponse(responseCode = "400", description = "Invalid calendar entry ID. The request may be missing required fields or contain invalid values.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<BetsPage> getBetsByCalendarNode(
            @Parameter(description = "Calendar entry ID") @NotBlank String calendarNodeId);

    @Operation(summary = "Delete calendar entry", description = "Accessible only to moderators and administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Calendar entry successfully deleted. The response contains details of the deleted calendar entry.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CalendarNodeDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid calendar entry ID. The request may be missing required fields or contain invalid values.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto"))),
            @ApiResponse(responseCode = "403", description = "User not authenticated. Access is restricted to authenticated moderators and administrators.",
                    content = @Content(mediaType = "application/json", schema = @Schema(ref = "StandardResponseDto")))
    })
    ResponseEntity<CalendarNodeDto> deleteCalendarNode(
            @Parameter(description = "Calendar entry ID") @NotBlank String calendarNodeId);
}
