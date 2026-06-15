package net.friendly_bets.gameresults;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;

import java.util.Optional;

/** Преобразование path/query параметра (PL или EPL) во внутренний код лиги и обратно в внешний competition code. */
public final class LeagueCodePathSupport {

    private LeagueCodePathSupport() {
    }

    public static String resolveStorageLeagueCode(String pathParam) {
        if (pathParam == null || pathParam.isBlank()) {
            throw new BadRequestException("unknownLeagueCode");
        }
        String trimmed = pathParam.trim();
        Optional<League.LeagueCode> fromExternal = LeagueCompetitionMapping.toLeagueCode(trimmed);
        if (fromExternal.isPresent()) {
            return fromExternal.get().name();
        }
        try {
            return League.LeagueCode.valueOf(trimmed).name();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("unknownLeagueCode");
        }
    }

    public static String toExternalCompetitionCode(String leagueCode) {
        League.LeagueCode code = League.LeagueCode.valueOf(leagueCode);
        return LeagueCompetitionMapping.toCompetitionCode(code)
                .orElseThrow(() -> new BadRequestException("leagueNotSupportedForMatchResults"));
    }
}
