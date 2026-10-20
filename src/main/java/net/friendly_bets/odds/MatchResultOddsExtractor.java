package net.friendly_bets.odds;

import net.friendly_bets.dto.MatchResultOddsDto;
import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.Odds;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Pulls full-match 1X2 (П1/Х/П2) best odds for compact matchday UI.
 * Ignores 1st/2nd half result rows that share {@code MATCH_RESULT} + HOME/DRAW/AWAY.
 */
public final class MatchResultOddsExtractor {

    private static final Set<Short> FULL_MATCH_RESULT_CODES = Set.of(
            BetTitleCode.HOME_WIN.getCode(),
            BetTitleCode.DRAW.getCode(),
            BetTitleCode.AWAY_WIN.getCode()
    );

    private MatchResultOddsExtractor() {
    }

    public static Optional<MatchResultOddsDto> extract(Odds odds) {
        if (odds == null || odds.getMarketGroups() == null || odds.getMarketGroups().isEmpty()) {
            return Optional.empty();
        }
        OddsMarketGroup matchResult = findMatchResultGroup(odds.getMarketGroups());
        if (matchResult == null || matchResult.getRows() == null || matchResult.getRows().isEmpty()) {
            return Optional.empty();
        }
        String home = null;
        String draw = null;
        String away = null;
        for (OddsLineRow row : matchResult.getRows()) {
            if (row == null || !isFullMatchResultRow(row)) {
                continue;
            }
            String code = normalizeCode(row.getSelectionCode());
            String value = resolveOddsValue(row);
            if (value == null) {
                continue;
            }
            if (OddsSelectionCode.HOME.name().equals(code)) {
                home = value;
            } else if (OddsSelectionCode.DRAW.name().equals(code)) {
                draw = value;
            } else if (OddsSelectionCode.AWAY.name().equals(code)) {
                away = value;
            }
        }
        if (home == null || draw == null || away == null) {
            return Optional.empty();
        }
        return Optional.of(MatchResultOddsDto.builder()
                .home(home)
                .draw(draw)
                .away(away)
                .build());
    }

    /**
     * Full-match П1/Х/П2 only ({@link BetTitleCode#HOME_WIN}/{@link BetTitleCode#DRAW}/{@link BetTitleCode#AWAY_WIN}).
     * Half-time rows use the same selection codes but different bet titles.
     */
    static boolean isFullMatchResultRow(OddsLineRow row) {
        BetTitle betTitle = row.getBetTitle();
        if (betTitle == null) {
            return false;
        }
        return FULL_MATCH_RESULT_CODES.contains(betTitle.getCode());
    }

    private static OddsMarketGroup findMatchResultGroup(List<OddsMarketGroup> groups) {
        for (OddsMarketGroup group : groups) {
            if (group == null || group.getCategory() == null) {
                continue;
            }
            if (OddsMarketCategory.MATCH_RESULT.name().equals(group.getCategory())) {
                return group;
            }
        }
        return null;
    }

    private static String normalizeCode(String selectionCode) {
        if (selectionCode == null || selectionCode.isBlank()) {
            return null;
        }
        return selectionCode.trim().toUpperCase(Locale.ROOT);
    }

    private static String resolveOddsValue(OddsLineRow row) {
        if (row.getBestOdds() != null && !row.getBestOdds().isBlank()) {
            return row.getBestOdds().trim();
        }
        if (row.getBookmakerOdds() == null || row.getBookmakerOdds().isEmpty()) {
            return null;
        }
        for (String value : row.getBookmakerOdds().values()) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
