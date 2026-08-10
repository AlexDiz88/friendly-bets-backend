package net.friendly_bets.ruscore;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class RuscoreParsedDayPage {

    String date;
    @Builder.Default
    List<CompetitionBlock> competitions = new ArrayList<>();

    @Value
    @Builder
    public static class CompetitionBlock {
        String title;
        /** Ruscore tournament season id from /tournament/{slug}/{id}, if present. */
        Integer seasonId;
        String tournamentSlug;
        @Builder.Default
        List<Match> matches = new ArrayList<>();
    }

    @Value
    @Builder
    public static class Match {
        String eventId;
        String slug;
        String homeName;
        String awayName;
        Instant utcKickoff;
        String statusText;
        String scoreText;
    }
}
