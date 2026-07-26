package net.friendly_bets.twentyfourscore;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class TwentyFourScoreParsedDatePage {

    @Builder.Default
    List<CompetitionBlock> competitions = new ArrayList<>();

    @Value
    @Builder
    public static class CompetitionBlock {
        String title;
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
        String fullTimeScore;
        String firstTimeScore;
        String liveMinuteLabel;
        String status;
    }
}
