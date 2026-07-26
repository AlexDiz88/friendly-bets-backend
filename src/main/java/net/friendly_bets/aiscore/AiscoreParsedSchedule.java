package net.friendly_bets.aiscore;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class AiscoreParsedSchedule {

    String tournamentPath;
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
        Instant utcKickoff;
        String status;
        String aiscoreMatchId;
        String homeTeamId;
        String awayTeamId;
    }

    public Map<Integer, Round> roundsByNumber() {
        Map<Integer, Round> map = new LinkedHashMap<>();
        for (Round round : rounds) {
            map.put(round.getNumber(), round);
        }
        return map;
    }
}
