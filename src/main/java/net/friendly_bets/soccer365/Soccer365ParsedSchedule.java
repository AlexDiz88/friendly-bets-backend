package net.friendly_bets.soccer365;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class Soccer365ParsedSchedule {

    int competitionId;
    @Builder.Default
    List<Round> rounds = new ArrayList<>();
    @Builder.Default
    List<String> clubFilterNames = new ArrayList<>();

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
        LocalDateTime utcKickoff;
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
