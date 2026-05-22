package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.PlayoffRound;
import net.friendly_bets.models.TournamentFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentFormatDto {

    private String id;
    private String formatCode;
    private String name;
    private LocalDateTime createdAt;
    private RoundRobinStageDto regularStage;
    private RoundRobinStageDto groupStage;
    private List<PlayoffRoundDto> playoff;
    private List<ExpandedMatchdaySlotDto> expandedSlots;
    private long linkedLeagueCount;

    public static TournamentFormatDto from(TournamentFormat format) {
        return from(format, null, 0L);
    }

    public static TournamentFormatDto from(TournamentFormat format, List<ExpandedMatchdaySlotDto> expandedSlots) {
        return from(format, expandedSlots, 0L);
    }

    public static TournamentFormatDto from(
            TournamentFormat format,
            List<ExpandedMatchdaySlotDto> expandedSlots,
            long linkedLeagueCount) {
        return TournamentFormatDto.builder()
                .id(format.getId())
                .formatCode(format.getFormatCode())
                .name(format.getName())
                .createdAt(format.getCreatedAt())
                .regularStage(RoundRobinStageDto.from(format.getRegularStage()))
                .groupStage(RoundRobinStageDto.from(format.getGroupStage()))
                .playoff(fromPlayoffEntity(format.getPlayoff()))
                .expandedSlots(expandedSlots)
                .linkedLeagueCount(linkedLeagueCount)
                .build();
    }

    private static List<PlayoffRoundDto> fromPlayoffEntity(List<PlayoffRound> playoff) {
        if (playoff == null) {
            return null;
        }
        return playoff.stream().map(PlayoffRoundDto::from).collect(Collectors.toList());
    }
}
