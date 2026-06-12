package net.friendly_bets.models.gameresults;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.GameScore;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "game_results")
@CompoundIndex(
        name = "league_matchday_season_teams_unique",
        def = "{'league_code': 1, 'matchday': 1, 'season': 1, 'home_team_id': 1, 'away_team_id': 1}",
        unique = true
)
public class GameResultRecord {

    @MongoId
    @Field(name = "_id")
    private String id;

    /** Внутренний код лиги ({@link net.friendly_bets.models.League.LeagueCode#name()}, напр. EPL). */
    @Field(name = "league_code")
    private String leagueCode;

    @Field(name = "matchday")
    private int matchday;

    @Field(name = "season")
    private String season;

    @Field(name = "league_id")
    private String leagueId;

    @Field(name = "home_team_id")
    private String homeTeamId;

    @Field(name = "away_team_id")
    private String awayTeamId;

    @Field(name = "status")
    private String status;

    @Field(name = "utc_date")
    private LocalDateTime utcDate;

    @Field(name = "game_score")
    private GameScore gameScore;

    @Field(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @Field(name = "provider")
    private String provider;

    @Field(name = "sources")
    @Builder.Default
    private Map<String, GameResultSourceSnapshot> sources = new HashMap<>();

    @Field(name = "finalized_at")
    private LocalDateTime finalizedAt;

    /** {@link GameResultFinalizedSource#name()}. */
    @Field(name = "finalized_source")
    private String finalizedSource;

    /** Ручная правка админом — запись не обновляется из API. */
    @Field(name = "admin_corrected")
    private boolean adminCorrected;

    /** football-data score/duration на момент последнего sync канона. */
    @Field(name = "score_duration")
    private String scoreDuration;

    /** Текущая минута live с 4score (напр. 72'), null после финиша. */
    @Field(name = "live_minute_label")
    private String liveMinuteLabel;

    /** Первый poll с terminal status. */
    @Field(name = "first_terminal_at")
    private LocalDateTime firstTerminalAt;

    /** Хеш канонического счёта с предыдущего poll. */
    @Field(name = "last_seen_canonical_score_hash")
    private String lastSeenCanonicalScoreHash;

    /** Подряд идущих poll с одинаковым каноническим счётом. */
    @Field(name = "stable_score_poll_count")
    @Builder.Default
    private int stableScorePollCount = 0;

    /** Событие odds-api.io после успешного сопоставления. */
    @Field(name = "odds_api_event_id")
    private Long oddsApiEventId;

    /** treeId события new.marathonbet.ru после успешного сопоставления. */
    @Field(name = "marathonbet_tree_id")
    private Long marathonbetTreeId;

    /** Slug страницы матча на 4score.ru (напр. angliya-costa-rica-10-06-2026). */
    @Field(name = "fourscore_event_slug")
    private String fourscoreEventSlug;

    /** Id матча в {@code wc26_schedule} (1–104). */
    @Field(name = "wc26_schedule_id")
    private Integer wc26ScheduleId;

    public boolean isFinalized() {
        return finalizedAt != null;
    }

    public GameResultSourceSnapshot sourceFor(String providerId) {
        if (sources == null || providerId == null) {
            return null;
        }
        return sources.get(providerId);
    }

    public GameResultSourceSnapshot footballDataSource() {
        return sourceFor(MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA));
    }

    public GameResultSourceSnapshot apiFootballSource() {
        return sourceFor(MatchDataProviders.sourcesStorageKey(MatchDataProviders.API_FOOTBALL));
    }

    public GameResultSourceSnapshot fourScoreSource() {
        return sourceFor(MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE));
    }
}
