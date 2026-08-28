package net.friendly_bets.eurofootball;

import lombok.Builder;
import lombok.Value;
import net.friendly_bets.providers.live.LiveMatchSnapshot;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class EuroFootballParsedDatePage {

    @Builder.Default
    List<CompetitionBlock> competitions = new ArrayList<>();

    @Value
    @Builder
    public static class CompetitionBlock {
        Integer tournamentId;
        String title;
        String slug;
        String parentSlug;
        @Builder.Default
        List<MatchRow> matches = new ArrayList<>();
    }

    @Value
    @Builder
    public static class MatchRow {
        String externalMatchId;
        String homeName;
        String awayName;
        String scoreText;
        LiveMatchSnapshot snapshot;
    }
}
