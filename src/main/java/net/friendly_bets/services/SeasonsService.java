package net.friendly_bets.services;

import net.friendly_bets.dto.NewSeasonDto;
import net.friendly_bets.dto.SeasonDto;
import net.friendly_bets.dto.SeasonsPage;
import net.friendly_bets.security.details.AuthenticatedUser;

import java.util.List;

public interface SeasonsService {

    SeasonsPage getAll();

    SeasonDto addSeason(NewSeasonDto newSeason);

    SeasonDto changeSeasonStatus(String title, String status);

    List<String> getSeasonStatusList();

    SeasonDto getActiveSeason();

}
