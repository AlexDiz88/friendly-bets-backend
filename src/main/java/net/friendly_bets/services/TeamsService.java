package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.NewTeamDto;
import net.friendly_bets.dto.TeamDto;
import net.friendly_bets.dto.TeamDisplayNamesDto;
import net.friendly_bets.dto.TeamExternalAliasDto;
import net.friendly_bets.dto.TeamsPage;
import net.friendly_bets.dto.UpdateTeamDto;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamDisplayNames;
import net.friendly_bets.models.TeamExternalAlias;
import net.friendly_bets.models.League;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.utils.TeamTitleUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeamsService {

    TeamsRepository teamsRepository;
    GetEntityService getEntityService;

    public TeamsPage getAll() {
        List<Team> allTeams = teamsRepository.findAll();
        return TeamsPage.builder()
                .teams(TeamDto.from(allTeams))
                .build();
    }

    public TeamsPage getLeagueTeams(String leagueId) {
        League league = getEntityService.getLeagueOrThrow(leagueId);
        List<Team> allTeams = league.getTeams();
        return TeamsPage.builder()
                .teams(TeamDto.from(allTeams))
                .build();
    }

    @Transactional
    public TeamDto createTeam(NewTeamDto newTeam) {
        String title = TeamTitleUtils.normalizeTitle(newTeam.getTitle());
        if (teamsRepository.existsByTitle(title)) {
            throw new ConflictException("teamWithThisTitleAlreadyExist");
        }

        List<TeamExternalAlias> aliases = buildAliases(newTeam.getExternalAliases(), newTeam.getFootballDataTeamId());

        Team team = Team.builder()
                .createdAt(LocalDateTime.now())
                .title(title)
                .country(newTeam.getCountry())
                .displayNames(toDisplayNames(newTeam.getDisplayNames(), title))
                .logo(TeamTitleUtils.toLocalLogoFileKey(title))
                .externalAliases(aliases)
                .footballDataTeamId(newTeam.getFootballDataTeamId())
                .build();

        teamsRepository.save(team);
        return TeamDto.from(team);
    }

    @Transactional
    public TeamDto updateTeam(String teamId, UpdateTeamDto update) {
        Team team = getEntityService.getTeamOrThrow(teamId);

        if (update.getCountry() != null) {
            team.setCountry(update.getCountry());
        }

        if (update.getDisplayNames() != null) {
            team.setDisplayNames(update.getDisplayNames().toEntity());
        }

        if (update.getFootballDataTeamId() != null) {
            team.setFootballDataTeamId(update.getFootballDataTeamId());
        }

        if (update.getExternalAliases() != null) {
            team.setExternalAliases(update.getExternalAliases().stream()
                    .map(TeamExternalAliasDto::toEntity)
                    .collect(Collectors.toCollection(ArrayList::new)));
        }

        teamsRepository.save(team);
        return TeamDto.from(team);
    }

    private static List<TeamExternalAlias> buildAliases(
            List<TeamExternalAliasDto> fromDto,
            Integer footballDataTeamId
    ) {
        List<TeamExternalAlias> aliases = new ArrayList<>();
        if (fromDto != null) {
            aliases.addAll(fromDto.stream().map(TeamExternalAliasDto::toEntity).toList());
        }
        return aliases;
    }

    private static TeamDisplayNames toDisplayNames(TeamDisplayNamesDto dto, String title) {
        if (dto == null) {
            return null;
        }
        return TeamDisplayNames.builder()
                .en(dto.getEn())
                .ru(dto.getRu())
                .de(dto.getDe())
                .build();
    }
}
