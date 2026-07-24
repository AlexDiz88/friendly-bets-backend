package net.friendly_bets.models.tournamentarchive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "tournament_archives")
public class TournamentArchive {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Indexed(unique = true)
    @Field(name = "edition_code")
    private String editionCode;

    /** WC | EURO */
    @Field(name = "competition_type")
    private String competitionType;

    @Field(name = "name")
    private String name;

    @Field(name = "year")
    private int year;

    @Field(name = "source")
    private String source;

    @Field(name = "exported_at")
    private LocalDateTime exportedAt;

    @Field(name = "imported_at")
    private LocalDateTime importedAt;

    @Field(name = "matches")
    @Builder.Default
    private List<TournamentArchiveMatch> matches = new ArrayList<>();

    /**
     * Сетка плей-офф: кто из каких матчей выходит в следующий
     * (пары номеров матчей, без per-match homeFrom/awayFrom).
     */
    @Field(name = "bracket")
    @Builder.Default
    private List<TournamentArchiveBracketPair> bracket = new ArrayList<>();

    @Field(name = "group_standings")
    @Builder.Default
    private List<TournamentArchiveStandingRow> groupStandings = new ArrayList<>();

    @Field(name = "best_third_places")
    @Builder.Default
    private List<TournamentArchiveBestThirdRow> bestThirdPlaces = new ArrayList<>();

    @Field(name = "unresolved_teams")
    @Builder.Default
    private List<String> unresolvedTeams = new ArrayList<>();
}
