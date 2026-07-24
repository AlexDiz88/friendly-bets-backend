package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.repositories.GameResultRecordRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameResultQueryService {

    private final GameResultRecordRepository gameResultRecordRepository;

    public List<GameResultRecord> getMatches(
            String pathLeagueOrCompetitionCode,
            int matchday,
            String season,
            String leagueId
    ) {
        String leagueCode = LeagueCodePathSupport.resolveStorageLeagueCode(pathLeagueOrCompetitionCode);
        return getMatchesByLeagueCode(leagueCode, matchday, season, leagueId);
    }

    public List<GameResultRecord> getMatchesByLeagueCode(
            String leagueCode,
            int matchday,
            String season,
            String leagueId
    ) {
        return new ArrayList<>(
                gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(
                        leagueCode, matchday, season));
    }
}
