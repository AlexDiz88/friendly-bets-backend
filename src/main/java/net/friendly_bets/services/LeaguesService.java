package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.LeagueDto;
import net.friendly_bets.dto.LeagueWithoutFormatDto;
import net.friendly_bets.dto.LeaguesWithoutFormatPage;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.repositories.LeaguesRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.repositories.TournamentFormatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaguesService {

    SeasonsRepository seasonsRepository;
    LeaguesRepository leaguesRepository;
    TournamentFormatsRepository tournamentFormatsRepository;
    GetEntityService getEntityService;

    @Transactional(readOnly = true)
    public LeaguesWithoutFormatPage getLeaguesWithoutFormat() {
        List<LeagueWithoutFormatDto> result = new ArrayList<>();
        for (Season season : seasonsRepository.findAll()) {
            if (season.getLeagues() == null) {
                continue;
            }
            for (League league : season.getLeagues()) {
                if (league.getTournamentFormatId() == null || league.getTournamentFormatId().isBlank()) {
                    result.add(LeagueWithoutFormatDto.of(season, league));
                }
            }
        }
        return LeaguesWithoutFormatPage.builder().leagues(result).build();
    }

    @Transactional
    public LeagueDto assignTournamentFormat(String leagueId, String tournamentFormatId) {
        League league = getEntityService.getLeagueOrThrow(leagueId);
        if (!tournamentFormatsRepository.existsById(tournamentFormatId)) {
            throw new NotFoundException("TournamentFormat", tournamentFormatId);
        }
        league.setTournamentFormatId(tournamentFormatId);
        leaguesRepository.save(league);
        return LeagueDto.from(league);
    }
}
