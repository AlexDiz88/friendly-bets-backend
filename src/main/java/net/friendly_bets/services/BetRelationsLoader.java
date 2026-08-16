package net.friendly_bets.services;

import com.mongodb.DBRef;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.LazyLoadingProxy;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Batch-loads lazy {@code @DBRef} on bets (user / season / league / teams)
 * so DTO mapping does not N+1 against Mongo.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BetRelationsLoader {

    MongoTemplate mongoTemplate;

    public void hydrate(List<Bet> bets) {
        hydrate(bets, true);
    }

    /**
     * @param includeAvatars false skips {@code accounts.avatar} (and password hash) — for list endpoints
     *                       that already have player avatars from the season payload
     */
    public void hydrate(List<Bet> bets, boolean includeAvatars) {
        if (bets == null || bets.isEmpty()) {
            return;
        }

        Set<String> userIds = new HashSet<>();
        Set<String> seasonIds = new HashSet<>();
        Set<String> leagueIds = new HashSet<>();
        Set<String> teamIds = new HashSet<>();

        for (Bet bet : bets) {
            addId(userIds, bet.getUser());
            addId(seasonIds, bet.getSeason());
            addId(leagueIds, bet.getLeague());
            addId(teamIds, bet.getHomeTeam());
            addId(teamIds, bet.getAwayTeam());
        }

        Map<String, User> usersById = includeAvatars
                ? loadByIds(User.class, userIds)
                : loadUsersWithoutAvatars(userIds);
        Map<String, Season> seasonsById = loadByIds(Season.class, seasonIds);
        Map<String, League> leaguesById = loadByIds(League.class, leagueIds);
        Map<String, Team> teamsById = loadByIds(Team.class, teamIds);

        for (Bet bet : bets) {
            replace(bet, usersById, seasonsById, leaguesById, teamsById);
        }
    }

    private static void replace(
            Bet bet,
            Map<String, User> usersById,
            Map<String, Season> seasonsById,
            Map<String, League> leaguesById,
            Map<String, Team> teamsById
    ) {
        User user = usersById.get(refId(bet.getUser()));
        if (user != null) {
            bet.setUser(user);
        }
        Season season = seasonsById.get(refId(bet.getSeason()));
        if (season != null) {
            bet.setSeason(season);
        }
        League league = leaguesById.get(refId(bet.getLeague()));
        if (league != null) {
            bet.setLeague(league);
        }
        Team home = teamsById.get(refId(bet.getHomeTeam()));
        if (home != null) {
            bet.setHomeTeam(home);
        }
        Team away = teamsById.get(refId(bet.getAwayTeam()));
        if (away != null) {
            bet.setAwayTeam(away);
        }
    }

    private Map<String, User> loadUsersWithoutAvatars(Set<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Query query = Query.query(Criteria.where("_id").in(mongoIds(ids)));
        query.fields().exclude("avatar").exclude("hash_password");
        Map<String, User> byId = new HashMap<>();
        for (User user : mongoTemplate.find(query, User.class)) {
            if (user.getId() != null) {
                byId.put(user.getId(), user);
            }
        }
        return byId;
    }

    private <T> Map<String, T> loadByIds(Class<T> type, Set<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Query query = Query.query(Criteria.where("_id").in(mongoIds(ids)));
        Map<String, T> byId = new HashMap<>();
        for (T entity : mongoTemplate.find(query, type)) {
            String id = entityId(entity);
            if (id != null) {
                byId.put(id, entity);
            }
        }
        return byId;
    }

    private static List<Object> mongoIds(Set<String> ids) {
        List<Object> values = new ArrayList<>();
        for (String id : ids) {
            values.add(id);
            if (ObjectId.isValid(id)) {
                values.add(new ObjectId(id));
            }
        }
        return values;
    }

    private static void addId(Set<String> ids, Object entity) {
        String id = refId(entity);
        if (id != null && !id.isBlank()) {
            ids.add(id);
        }
    }

    /**
     * ID of a lazy {@code @DBRef} without initializing the proxy (avoids N+1).
     */
    static String refId(Object entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof LazyLoadingProxy proxy) {
            DBRef dbRef = proxy.toDBRef();
            if (dbRef == null || dbRef.getId() == null) {
                return null;
            }
            return dbRef.getId().toString();
        }
        return entityId(entity);
    }

    private static String entityId(Object entity) {
        if (entity instanceof User user) {
            return user.getId();
        }
        if (entity instanceof Team team) {
            return team.getId();
        }
        if (entity instanceof League league) {
            return league.getId();
        }
        if (entity instanceof Season season) {
            return season.getId();
        }
        return null;
    }
}
