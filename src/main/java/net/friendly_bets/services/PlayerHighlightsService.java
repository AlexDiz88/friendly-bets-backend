package net.friendly_bets.services;

import com.mongodb.DBRef;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.AllPlayerHighlightsDto;
import net.friendly_bets.dto.BestGameweekDto;
import net.friendly_bets.dto.BiggestWinDto;
import net.friendly_bets.dto.HighlightTeamDto;
import net.friendly_bets.dto.PlayerHighlightDto;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.CalendarNode;
import net.friendly_bets.models.GameweekStats;
import net.friendly_bets.models.Team;
import net.friendly_bets.repositories.TeamsRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.friendly_bets.utils.Constants.COMPLETED_BET_STATUSES;
import static net.friendly_bets.utils.Constants.TOTAL_ID;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerHighlightsService {

    static final int RECENT_FORM_LIMIT = 12;

    MongoTemplate mongoTemplate;
    TeamsRepository teamsRepository;
    GetEntityService getEntityService;

    public AllPlayerHighlightsDto getHighlights(String seasonId) {
        getEntityService.getSeasonOrThrow(seasonId);

        Map<String, List<HighlightBetRow>> betsByUser = loadBetRows(seasonId).stream()
                .filter(row -> row.getUserId() != null)
                .collect(Collectors.groupingBy(HighlightBetRow::getUserId));
        List<CalendarNode> finishedNodes = loadFinishedNodes(seasonId);
        Map<String, Map<String, Double>> teamBalancesByUser = loadTeamBalances(seasonId);

        Set<String> teamIds = new HashSet<>();
        Map<String, PlayerHighlightDraft> drafts = new HashMap<>();
        for (Map.Entry<String, List<HighlightBetRow>> entry : betsByUser.entrySet()) {
            PlayerHighlightDraft draft = draftPlayerHighlight(
                    entry.getKey(),
                    entry.getValue(),
                    finishedNodes,
                    teamBalancesByUser.getOrDefault(entry.getKey(), Map.of())
            );
            drafts.put(entry.getKey(), draft);
            addTeamId(teamIds, draft.biggestWinHomeTeamId);
            addTeamId(teamIds, draft.biggestWinAwayTeamId);
            addTeamId(teamIds, draft.profitableTeamId);
            addTeamId(teamIds, draft.unprofitableTeamId);
        }

        Map<String, Team> teamsById = loadTeams(teamIds);
        List<PlayerHighlightDto> players = drafts.values().stream()
                .map(draft -> toDto(draft, teamsById))
                .collect(Collectors.toList());

        return AllPlayerHighlightsDto.builder().players(players).build();
    }

    List<HighlightBetRow> loadBetRows(String seasonId) {
        Query query = Query.query(seasonRefCriteria(seasonId)
                .and("bet_status").in(COMPLETED_BET_STATUSES.stream().map(Enum::name).toList()));
        query.fields()
                .include("user")
                .include("bet_status")
                .include("balance_change")
                .include("bet_odds")
                .include("bet_result_added_at")
                .include("created_at")
                .include("home_team")
                .include("away_team");
        return mongoTemplate.find(query, Document.class, "bets").stream()
                .map(PlayerHighlightsService::toBetRow)
                .collect(Collectors.toList());
    }

    List<CalendarNode> loadFinishedNodes(String seasonId) {
        Query query = Query.query(Criteria.where("season_id").is(seasonId).and("is_finished").is(true));
        query.fields()
                .include("start_date")
                .include("end_date")
                .include("gameweek_stats");
        return mongoTemplate.find(query, CalendarNode.class, "calendar_nodes");
    }

    Map<String, Map<String, Double>> loadTeamBalances(String seasonId) {
        Query query = Query.query(Criteria.where("seasonId").is(seasonId).and("userId").ne(TOTAL_ID));
        query.fields().include("userId").include("teamStats");
        Map<String, Map<String, Double>> byUser = new HashMap<>();
        for (Document doc : mongoTemplate.find(query, Document.class, "player_stats_by_teams")) {
            String userId = doc.getString("userId");
            if (userId == null) {
                continue;
            }
            Map<String, Double> balances = byUser.computeIfAbsent(userId, key -> new HashMap<>());
            Object rawStats = doc.get("teamStats");
            if (!(rawStats instanceof List<?> teamStats)) {
                continue;
            }
            for (Object item : teamStats) {
                if (!(item instanceof Document teamStat)) {
                    continue;
                }
                String teamId = dbRefId(teamStat.get("team"));
                Double balance = asDouble(teamStat.get("actualBalance"));
                if (teamId == null || balance == null) {
                    continue;
                }
                balances.merge(teamId, balance, Double::sum);
            }
        }
        return byUser;
    }

    Map<String, Team> loadTeams(Collection<String> teamIds) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Team> teamsById = new HashMap<>();
        teamsRepository.findAllById(teamIds).forEach(team -> teamsById.put(team.getId(), team));
        return teamsById;
    }

    static PlayerHighlightDraft draftPlayerHighlight(
            String userId,
            List<HighlightBetRow> bets,
            List<CalendarNode> finishedNodes,
            Map<String, Double> teamBalances
    ) {
        List<HighlightBetRow> chronological = bets.stream()
                .sorted(Comparator.comparing(HighlightBetRow::time))
                .collect(Collectors.toList());
        HighlightBetRow biggest = biggestWinRow(chronological);
        String profitableId = extremeTeamId(teamBalances, true);
        String unprofitableId = extremeTeamId(teamBalances, false);
        return PlayerHighlightDraft.builder()
                .userId(userId)
                .recentForm(recentForm(chronological))
                .bestWinStreak(bestWinStreak(chronological))
                .biggestWinChange(biggest == null ? null : biggest.getBalanceChange())
                .biggestWinOdds(biggest == null ? null : biggest.getBetOdds())
                .biggestWinHomeTeamId(biggest == null ? null : biggest.getHomeTeamId())
                .biggestWinAwayTeamId(biggest == null ? null : biggest.getAwayTeamId())
                .bestGameweek(bestGameweek(userId, finishedNodes))
                .profitableTeamId(profitableId)
                .profitableTeamBalance(profitableId == null ? null : teamBalances.get(profitableId))
                .unprofitableTeamId(unprofitableId)
                .unprofitableTeamBalance(unprofitableId == null ? null : teamBalances.get(unprofitableId))
                .build();
    }

    static PlayerHighlightDto toDto(PlayerHighlightDraft draft, Map<String, Team> teamsById) {
        BiggestWinDto biggestWin = null;
        if (draft.biggestWinChange != null) {
            biggestWin = BiggestWinDto.builder()
                    .balanceChange(draft.biggestWinChange)
                    .betOdds(draft.biggestWinOdds)
                    .homeTeam(HighlightTeamDto.from(teamsById.get(draft.biggestWinHomeTeamId), null))
                    .awayTeam(HighlightTeamDto.from(teamsById.get(draft.biggestWinAwayTeamId), null))
                    .build();
        }
        return PlayerHighlightDto.builder()
                .userId(draft.userId)
                .recentForm(draft.recentForm)
                .biggestWin(biggestWin)
                .bestWinStreak(draft.bestWinStreak)
                .bestGameweek(draft.bestGameweek)
                .mostProfitableTeam(HighlightTeamDto.from(
                        teamsById.get(draft.profitableTeamId),
                        draft.profitableTeamBalance
                ))
                .mostUnprofitableTeam(HighlightTeamDto.from(
                        teamsById.get(draft.unprofitableTeamId),
                        draft.unprofitableTeamBalance
                ))
                .build();
    }

    static List<String> recentForm(List<HighlightBetRow> chronological) {
        if (chronological.isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, chronological.size() - RECENT_FORM_LIMIT);
        List<String> form = new ArrayList<>(chronological.size() - from);
        for (int i = from; i < chronological.size(); i++) {
            Bet.BetStatus status = chronological.get(i).getStatus();
            if (status != null) {
                form.add(status.name());
            }
        }
        return form;
    }

    static int bestWinStreak(List<HighlightBetRow> chronological) {
        int best = 0;
        int current = 0;
        for (HighlightBetRow row : chronological) {
            if (row.getStatus() == Bet.BetStatus.WON) {
                current += 1;
                if (current > best) {
                    best = current;
                }
            } else {
                current = 0;
            }
        }
        return best;
    }

    static HighlightBetRow biggestWinRow(List<HighlightBetRow> chronological) {
        HighlightBetRow best = null;
        for (HighlightBetRow row : chronological) {
            if (row.getStatus() != Bet.BetStatus.WON || row.getBalanceChange() == null) {
                continue;
            }
            if (best == null || row.getBalanceChange() > best.getBalanceChange()) {
                best = row;
            }
        }
        return best;
    }

    static BestGameweekDto bestGameweek(String userId, List<CalendarNode> finishedNodes) {
        BestGameweekDto best = null;
        for (CalendarNode node : finishedNodes) {
            if (node.getGameweekStats() == null) {
                continue;
            }
            for (GameweekStats stats : node.getGameweekStats()) {
                if (stats == null || !userId.equals(stats.getUserId()) || stats.getBalanceChange() == null) {
                    continue;
                }
                if (best == null || stats.getBalanceChange() > best.getBalanceChange()) {
                    best = BestGameweekDto.builder()
                            .calendarNodeId(node.getId())
                            .startDate(node.getStartDate())
                            .endDate(node.getEndDate())
                            .balanceChange(stats.getBalanceChange())
                            .build();
                }
            }
        }
        return best;
    }

    static String extremeTeamId(Map<String, Double> teamBalances, boolean profitable) {
        if (teamBalances == null || teamBalances.isEmpty()) {
            return null;
        }
        String chosenId = null;
        Double chosenBalance = null;
        for (Map.Entry<String, Double> entry : teamBalances.entrySet()) {
            if (chosenId == null) {
                chosenId = entry.getKey();
                chosenBalance = entry.getValue();
                continue;
            }
            int cmp = Double.compare(entry.getValue(), chosenBalance);
            if (profitable ? cmp > 0 : cmp < 0) {
                chosenId = entry.getKey();
                chosenBalance = entry.getValue();
            }
        }
        return chosenId;
    }

    static HighlightBetRow toBetRow(Document doc) {
        return HighlightBetRow.builder()
                .userId(dbRefId(doc.get("user")))
                .status(parseStatus(doc.get("bet_status")))
                .balanceChange(asDouble(doc.get("balance_change")))
                .betOdds(asDouble(doc.get("bet_odds")))
                .resultAt(asInstant(doc.get("bet_result_added_at")))
                .createdAt(asInstant(doc.get("created_at")))
                .homeTeamId(dbRefId(doc.get("home_team")))
                .awayTeamId(dbRefId(doc.get("away_team")))
                .build();
    }

    static Criteria seasonRefCriteria(String seasonId) {
        List<Criteria> parts = new ArrayList<>();
        parts.add(Criteria.where("season.$id").is(seasonId));
        if (ObjectId.isValid(seasonId)) {
            parts.add(Criteria.where("season.$id").is(new ObjectId(seasonId)));
        }
        return new Criteria().orOperator(parts.toArray(Criteria[]::new));
    }

    static String dbRefId(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof DBRef dbRef && dbRef.getId() != null) {
            return dbRef.getId().toString();
        }
        if (raw instanceof Document document) {
            Object id = document.get("$id");
            if (id == null) {
                id = document.get("id");
            }
            return id == null ? null : id.toString();
        }
        return raw.toString();
    }

    static Bet.BetStatus parseStatus(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Bet.BetStatus.valueOf(raw.toString());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static Double asDouble(Object raw) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    static Instant asInstant(Object raw) {
        if (raw instanceof Instant instant) {
            return instant;
        }
        if (raw instanceof Date date) {
            return date.toInstant();
        }
        return null;
    }

    static void addTeamId(Set<String> teamIds, String teamId) {
        if (teamId != null) {
            teamIds.add(teamId);
        }
    }

    @Value
    @Builder
    static class HighlightBetRow {
        String userId;
        Bet.BetStatus status;
        Double balanceChange;
        Double betOdds;
        Instant resultAt;
        Instant createdAt;
        String homeTeamId;
        String awayTeamId;

        Instant time() {
            if (resultAt != null) {
                return resultAt;
            }
            if (createdAt != null) {
                return createdAt;
            }
            return Instant.EPOCH;
        }
    }

    @Value
    @Builder
    static class PlayerHighlightDraft {
        String userId;
        List<String> recentForm;
        int bestWinStreak;
        Double biggestWinChange;
        Double biggestWinOdds;
        String biggestWinHomeTeamId;
        String biggestWinAwayTeamId;
        BestGameweekDto bestGameweek;
        String profitableTeamId;
        Double profitableTeamBalance;
        String unprofitableTeamId;
        Double unprofitableTeamBalance;
    }
}
