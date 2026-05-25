package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.Team;
import net.friendly_bets.services.TeamAliasResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FootballDataTeamResolver {

    private static final Logger log = LoggerFactory.getLogger(FootballDataTeamResolver.class);

    private final TeamAliasResolver teamAliasResolver;

    public Optional<Team> resolve(int footballDataTeamId, String footballDataTeamName) {
        Optional<Team> team = teamAliasResolver.resolveFootballData(footballDataTeamId, footballDataTeamName);
        if (team.isEmpty()) {
            log.debug("No mapping for football-data team id={}, name={}", footballDataTeamId, footballDataTeamName);
        }
        return team;
    }
}
