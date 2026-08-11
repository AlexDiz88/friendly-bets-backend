package net.friendly_bets.flashscore;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class FlashscoreParsedDayPage {

    String date;
    @Builder.Default
    List<CompetitionBlock> competitions = new ArrayList<>();

    @Value
    @Builder
    public static class CompetitionBlock {
        String title;
        String stageId;
        String tournamentPath;
        @Builder.Default
        List<Match> matches = new ArrayList<>();
    }

    @Value
    @Builder
    public static class Match {
        String eventId;
        String homeName;
        String awayName;
        String homeParticipantId;
        String awayParticipantId;
        Instant utcKickoff;
        String statusText;
        String scoreText;
    }
}
