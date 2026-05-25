package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.footballdata.config.FootballDataProperties;
import net.friendly_bets.models.Team;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.utils.TeamTitleUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TeamAliasResolver {

    private final TeamsRepository teamsRepository;
    private final FootballDataProperties footballDataProperties;

    public Optional<Team> resolveFootballData(int footballDataTeamId, String footballDataTeamName) {
        Optional<Team> byStoredId = teamsRepository.findByFootballDataTeamId(footballDataTeamId);
        if (byStoredId.isPresent()) {
            return byStoredId;
        }

        Optional<Team> byAliasId = teamsRepository.findByExternalAliasId(
                TeamTitleUtils.FOOTBALL_DATA_PROVIDER, footballDataTeamId);
        if (byAliasId.isPresent()) {
            return byAliasId;
        }

        if (footballDataTeamName != null && !footballDataTeamName.isBlank()) {
            Optional<Team> byAliasName = teamsRepository.findByExternalAliasName(
                    TeamTitleUtils.FOOTBALL_DATA_PROVIDER, footballDataTeamName);
            if (byAliasName.isPresent()) {
                return byAliasName;
            }
        }

        String mappedTitle = footballDataProperties.getTeamIds().get(footballDataTeamId);
        if (mappedTitle == null && footballDataTeamName != null) {
            mappedTitle = footballDataProperties.getTeamNames().get(footballDataTeamName);
        }
        if (mappedTitle != null) {
            Optional<Team> byTitle = teamsRepository.findByTitleIgnoreCase(mappedTitle);
            if (byTitle.isPresent()) {
                return byTitle;
            }
            String compact = mappedTitle.replaceAll("\\s+", "");
            if (!compact.equals(mappedTitle)) {
                return teamsRepository.findByTitle(compact);
            }
        }

        return Optional.empty();
    }
}
