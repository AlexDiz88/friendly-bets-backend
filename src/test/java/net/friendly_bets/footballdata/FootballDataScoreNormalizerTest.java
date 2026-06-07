package net.friendly_bets.footballdata;

import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import net.friendly_bets.models.GameScore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FootballDataScoreNormalizerTest {

    private final FootballDataScoreNormalizer normalizer = new FootballDataScoreNormalizer();

    @Test
    @DisplayName("PSG–Arsenal final: regularTime 1:1, penalties 4:3, API fullTime 5:4")
    void penaltyFinalUsesRegularTime() {
        FootballDataMatchDto dto = penaltyFinalDto("1:1", "0:1", "0:0", "5:4", "4:3");

        GameScore canonical = normalizer.normalize(dto);

        assertNotNull(canonical);
        assertEquals("1:1", canonical.getFullTime());
        assertEquals("0:1", canonical.getFirstTime());
        assertEquals("0:0", canonical.getOverTime());
        assertEquals("4:3", canonical.getPenalty());
    }

    @Test
    @DisplayName("Fallback subtracts penalties from fullTime when regularTime missing")
    void penaltyFallbackSubtractsPenalties() {
        FootballDataMatchDto dto = penaltyFinalDto(null, "0:1", "0:0", "5:4", "4:3");

        GameScore canonical = normalizer.normalize(dto);

        assertEquals("1:1", canonical.getFullTime());
        assertEquals("4:3", canonical.getPenalty());
    }

    @Test
    @DisplayName("Regular league match uses fullTime as canonical fullTime")
    void regularMatchUsesFullTime() {
        FootballDataMatchDto dto = baseDto("FINISHED", FootballDataScoreNormalizer.DURATION_REGULAR);
        FootballDataMatchDto.Score score = dto.getScore();
        score.setFullTime(line(2, 1));
        score.setHalfTime(line(1, 0));

        GameScore canonical = normalizer.normalize(dto);

        assertEquals("2:1", canonical.getFullTime());
        assertEquals("1:0", canonical.getFirstTime());
        assertNull(canonical.getOverTime());
        assertNull(canonical.getPenalty());
    }

    @Test
    @DisplayName("Raw snapshot keeps API fullTime including penalties")
    void rawSnapshotPreservesApiFullTime() {
        FootballDataMatchDto dto = penaltyFinalDto("1:1", "0:1", "0:0", "5:4", "4:3");

        GameScore raw = normalizer.toRawApiScore(dto);

        assertEquals("5:4", raw.getFullTime());
        assertEquals("4:3", raw.getPenalty());
    }

    @Test
    @DisplayName("normalizeFromRawSnapshot for recovery")
    void normalizeFromRawSnapshot() {
        GameScore raw = GameScore.builder()
                .fullTime("5:4")
                .firstTime("0:1")
                .overTime("0:0")
                .penalty("4:3")
                .build();

        GameScore canonical = normalizer.normalizeFromRawSnapshot(
                raw, FootballDataScoreNormalizer.DURATION_PENALTY_SHOOTOUT);

        assertEquals("1:1", canonical.getFullTime());
        assertEquals("4:3", canonical.getPenalty());
    }

    private static FootballDataMatchDto penaltyFinalDto(
            String regular,
            String half,
            String extra,
            String full,
            String pen
    ) {
        FootballDataMatchDto dto = baseDto("FINISHED", FootballDataScoreNormalizer.DURATION_PENALTY_SHOOTOUT);
        FootballDataMatchDto.Score score = dto.getScore();
        if (regular != null) {
            score.setRegularTime(parse(regular));
        }
        score.setHalfTime(parse(half));
        score.setExtraTime(parse(extra));
        score.setFullTime(parse(full));
        score.setPenalties(parse(pen));
        return dto;
    }

    private static FootballDataMatchDto baseDto(String status, String duration) {
        FootballDataMatchDto dto = new FootballDataMatchDto();
        dto.setStatus(status);
        FootballDataMatchDto.Score score = new FootballDataMatchDto.Score();
        score.setDuration(duration);
        dto.setScore(score);
        return dto;
    }

    private static FootballDataMatchDto.ScoreLine line(int home, int away) {
        FootballDataMatchDto.ScoreLine line = new FootballDataMatchDto.ScoreLine();
        line.setHome(home);
        line.setAway(away);
        return line;
    }

    private static FootballDataMatchDto.ScoreLine parse(String value) {
        String[] p = value.split(":");
        return line(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
    }
}
