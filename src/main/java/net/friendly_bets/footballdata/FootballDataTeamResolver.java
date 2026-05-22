package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.footballdata.config.FootballDataProperties;
import net.friendly_bets.models.Team;
import net.friendly_bets.repositories.TeamsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FootballDataTeamResolver {

    private static final Logger log = LoggerFactory.getLogger(FootballDataTeamResolver.class);

    private final TeamsRepository teamsRepository;
    private final FootballDataProperties properties;

    public Optional<Team> resolve(int footballDataTeamId, String footballDataTeamName) {
        Optional<Team> byId = teamsRepository.findByFootballDataTeamId(footballDataTeamId);
        if (byId.isPresent()) {
            return byId;
        }

        String mappedTitle = properties.getTeamIds().get(footballDataTeamId);
        if (mappedTitle == null && footballDataTeamName != null) {
            mappedTitle = properties.getTeamNames().get(footballDataTeamName);
        }

        if (mappedTitle != null) {
            Optional<Team> byTitle = teamsRepository.findByTitleIgnoreCase(mappedTitle);
            if (byTitle.isPresent()) {
                return byTitle;
            }
            log.warn("Team mapping points to missing title '{}', football-data id={}, name={}",
                    mappedTitle, footballDataTeamId, footballDataTeamName);
        } else {
            log.debug("No mapping for football-data team id={}, name={}", footballDataTeamId, footballDataTeamName);
        }

        return Optional.empty();
    }
}
