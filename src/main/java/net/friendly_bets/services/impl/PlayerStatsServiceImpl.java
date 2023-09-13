package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.repositories.UsersRepository;
import net.friendly_bets.services.PlayerStatsService;
import net.friendly_bets.services.UsersService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.friendly_bets.utils.GetEntityOrThrow.getSeasonOrThrow;
import static net.friendly_bets.utils.GetEntityOrThrow.getUserOrThrow;

@RequiredArgsConstructor
@Service
public class PlayerStatsServiceImpl implements PlayerStatsService {


    @Override
    public AllPlayersStatsDto getAllPlayersStatsBySeason(String seasonId) {
        return null;
    }

    @Override
    public AllPlayersStatsByLeaguesDto getAllPlayersStatsByLeagues(String seasonId) {
        return null;
    }
}
