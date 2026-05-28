package net.friendly_bets.models.gameresults;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.GameScore;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Снимок матча от одного внешнего API. После первичного сохранения родительской записи
 * обновляются только {@link #status} и {@link #fetchedAt}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class GameResultSourceSnapshot {

    @Field(name = "external_match_id")
    private long externalMatchId;

    @Field(name = "external_competition_code")
    private String externalCompetitionCode;

    @Field(name = "external_matchday")
    private Integer externalMatchday;

    @Field(name = "external_season")
    private String externalSeason;

    @Field(name = "status")
    private String status;

    @Field(name = "utc_date")
    private LocalDateTime utcDate;

    @Field(name = "game_score")
    private GameScore gameScore;

    @Field(name = "home")
    private GameResultSideSnapshot home;

    @Field(name = "away")
    private GameResultSideSnapshot away;

    /** Время последнего изменения матча по данным API (поле lastUpdated у football-data). */
    @Field(name = "api_last_updated")
    private LocalDateTime apiLastUpdated;

    /** Время нашей синхронизации этого снимка. */
    @Field(name = "fetched_at")
    private LocalDateTime fetchedAt;
}
