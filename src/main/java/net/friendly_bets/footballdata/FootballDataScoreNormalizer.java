package net.friendly_bets.footballdata;

import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import net.friendly_bets.models.GameScore;
import org.springframework.stereotype.Component;

/**
 * Преобразует счёт football-data v4 в канон FriendlyBets:
 * {@code fullTime} — после 90 минут ({@code regularTime}), не сумма с пенальти.
 */
@Component
public class FootballDataScoreNormalizer {

    public static final String DURATION_REGULAR = "REGULAR";
    public static final String DURATION_EXTRA_TIME = "EXTRA_TIME";
    public static final String DURATION_PENALTY_SHOOTOUT = "PENALTY_SHOOTOUT";

    public GameScore normalize(FootballDataMatchDto dto) {
        if (dto == null || dto.getScore() == null || !FootballDataMatchStatuses.hasScore(dto.getStatus())) {
            return null;
        }
        FootballDataMatchDto.Score score = dto.getScore();
        String duration = normalizeDuration(score.getDuration());

        GameScore.GameScoreBuilder builder = GameScore.builder();

        String fullTime = resolveFullTime(score, duration);
        if (fullTime == null) {
            return null;
        }
        builder.fullTime(fullTime);

        if (score.getHalfTime() != null
                && score.getHalfTime().getHome() != null
                && score.getHalfTime().getAway() != null) {
            builder.firstTime(formatScore(score.getHalfTime().getHome(), score.getHalfTime().getAway()));
        }

        if (hasExtraTimePeriod(duration)
                && score.getExtraTime() != null
                && score.getExtraTime().getHome() != null
                && score.getExtraTime().getAway() != null) {
            builder.overTime(formatScore(score.getExtraTime().getHome(), score.getExtraTime().getAway()));
        }

        if (DURATION_PENALTY_SHOOTOUT.equals(duration)
                && score.getPenalties() != null
                && score.getPenalties().getHome() != null
                && score.getPenalties().getAway() != null) {
            builder.penalty(formatScore(score.getPenalties().getHome(), score.getPenalties().getAway()));
        }

        return builder.build();
    }

    /**
     * Нормализация из сырого снимка football-data (для recovery после расхождения).
     */
    public GameScore normalizeFromRawSnapshot(GameScore raw, String duration) {
        if (raw == null || raw.getFullTime() == null) {
            return null;
        }
        FootballDataMatchDto.Score score = new FootballDataMatchDto.Score();
        score.setDuration(duration);
        score.setFullTime(parseLine(raw.getFullTime()));
        score.setHalfTime(parseLine(raw.getFirstTime()));
        score.setExtraTime(parseLine(raw.getOverTime()));
        score.setPenalties(parseLine(raw.getPenalty()));

        FootballDataMatchDto dto = new FootballDataMatchDto();
        dto.setStatus("FINISHED");
        dto.setScore(score);
        return normalize(dto);
    }

    private static FootballDataMatchDto.ScoreLine parseLine(String part) {
        if (part == null || part.isBlank()) {
            return null;
        }
        String[] tokens = part.trim().split(":");
        if (tokens.length != 2) {
            return null;
        }
        try {
            FootballDataMatchDto.ScoreLine line = new FootballDataMatchDto.ScoreLine();
            line.setHome(Integer.parseInt(tokens[0]));
            line.setAway(Integer.parseInt(tokens[1]));
            return line;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Сырой счёт API без нормализации — для {@code sources.football_data}. */
    public GameScore toRawApiScore(FootballDataMatchDto dto) {
        if (dto == null || dto.getScore() == null || !FootballDataMatchStatuses.hasScore(dto.getStatus())) {
            return null;
        }
        FootballDataMatchDto.Score score = dto.getScore();
        GameScore.GameScoreBuilder builder = GameScore.builder();

        if (score.getFullTime() != null
                && score.getFullTime().getHome() != null
                && score.getFullTime().getAway() != null) {
            builder.fullTime(formatScore(score.getFullTime().getHome(), score.getFullTime().getAway()));
        }
        if (score.getHalfTime() != null
                && score.getHalfTime().getHome() != null
                && score.getHalfTime().getAway() != null) {
            builder.firstTime(formatScore(score.getHalfTime().getHome(), score.getHalfTime().getAway()));
        }
        if (score.getExtraTime() != null
                && score.getExtraTime().getHome() != null
                && score.getExtraTime().getAway() != null) {
            builder.overTime(formatScore(score.getExtraTime().getHome(), score.getExtraTime().getAway()));
        }
        if (score.getPenalties() != null
                && score.getPenalties().getHome() != null
                && score.getPenalties().getAway() != null) {
            builder.penalty(formatScore(score.getPenalties().getHome(), score.getPenalties().getAway()));
        }

        GameScore gameScore = builder.build();
        if (gameScore.getFullTime() == null) {
            return null;
        }
        return gameScore;
    }

    public boolean isKnockoutDuration(String duration) {
        return hasExtraTimePeriod(duration) || DURATION_PENALTY_SHOOTOUT.equals(duration);
    }

    private static String resolveFullTime(FootballDataMatchDto.Score score, String duration) {
        if (score.getRegularTime() != null
                && score.getRegularTime().getHome() != null
                && score.getRegularTime().getAway() != null) {
            return formatScore(score.getRegularTime().getHome(), score.getRegularTime().getAway());
        }
        if (DURATION_PENALTY_SHOOTOUT.equals(duration)
                && score.getFullTime() != null
                && score.getPenalties() != null
                && score.getFullTime().getHome() != null
                && score.getFullTime().getAway() != null
                && score.getPenalties().getHome() != null
                && score.getPenalties().getAway() != null) {
            int home = score.getFullTime().getHome() - score.getPenalties().getHome();
            int away = score.getFullTime().getAway() - score.getPenalties().getAway();
            if (home >= 0 && away >= 0) {
                return formatScore(home, away);
            }
        }
        if (score.getFullTime() != null
                && score.getFullTime().getHome() != null
                && score.getFullTime().getAway() != null) {
            return formatScore(score.getFullTime().getHome(), score.getFullTime().getAway());
        }
        return null;
    }

    private static boolean hasExtraTimePeriod(String duration) {
        return DURATION_EXTRA_TIME.equals(duration) || DURATION_PENALTY_SHOOTOUT.equals(duration);
    }

    static String normalizeDuration(String duration) {
        if (duration == null || duration.isBlank()) {
            return DURATION_REGULAR;
        }
        return duration.trim().toUpperCase();
    }

    private static String formatScore(int home, int away) {
        return home + ":" + away;
    }
}
