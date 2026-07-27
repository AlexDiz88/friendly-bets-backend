package net.friendly_bets.sportsru;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class SportsRuParsedSchedule {

    @Builder.Default
    List<Round> rounds = new ArrayList<>();

    @Value
    @Builder
    public static class Round {
        int number;
        @Builder.Default
        List<Match> matches = new ArrayList<>();
    }

    @Value
    @Builder
    public static class Match {
        String homeName;
        String awayName;
        /** Relative path e.g. {@code /football/match/arsenal-vs-coventry-city-fc/}. */
        String matchPath;
        Instant utcKickoff;
        String status;
    }

    public Map<Integer, Round> roundsByNumber() {
        Map<Integer, Round> map = new LinkedHashMap<>();
        for (Round round : rounds) {
            map.put(round.getNumber(), round);
        }
        return map;
    }
}
