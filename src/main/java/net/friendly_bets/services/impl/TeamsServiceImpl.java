package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.NewTeamDto;
import net.friendly_bets.dto.TeamDto;
import net.friendly_bets.dto.TeamsPage;
import net.friendly_bets.exceptions.BadDataException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.Team;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.TeamsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class TeamsServiceImpl implements TeamsService {

    private final TeamsRepository teamsRepository;
    @Override
    public TeamsPage getAll() {
        List<Team> allTeams = teamsRepository.findAll();
        return TeamsPage.builder()
                .teams(TeamDto.from(allTeams))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public TeamDto createTeam(NewTeamDto newTeam) {
        if (newTeam == null) {
            throw new BadDataException("Объект не должен быть пустым");
        }
        if (newTeam.getFullTitleRu() == null || newTeam.getFullTitleRu().trim().length() < 1) {
            throw new BadDataException("Название сезона(RU) не может быть пустым");
        }
        if (newTeam.getFullTitleEn() == null || newTeam.getFullTitleEn().trim().length() < 1) {
            throw new BadDataException("Название сезона(EN) не может быть пустым");
        }
        if (newTeam.getCountry() == null || newTeam.getCountry().trim().length() < 1) {
            throw new BadDataException("Название страны команды не может быть пустым");
        }
        if (teamsRepository.existsByFullTitleRuOrFullTitleEn(newTeam.getFullTitleRu(), newTeam.getFullTitleEn())) {
            throw new ConflictException("Команда с таким названием уже существует");
        }

        Team team = Team.builder()
                .createdAt(LocalDateTime.now())
                .fullTitleRu(newTeam.getFullTitleRu())
                .fullTitleEn(newTeam.getFullTitleEn())
                .country(newTeam.getCountry())
                .logo("")
                .build();
        // TODO переделать инициализацию logo, после добавления функционала загрузки изображений с ПК

        teamsRepository.save(team);
        return TeamDto.from(team);
    }

    // ------------------------------------------------------------------------------------------------------ //
}
