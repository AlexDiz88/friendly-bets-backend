package net.friendly_bets.tournamentarchive;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.tournamentarchive.TournamentArchive;
import net.friendly_bets.models.tournamentarchive.TournamentArchiveMatch;
import net.friendly_bets.repositories.TournamentArchiveRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentArchiveService {

    public static final String WC_2026 = "WC_2026";

    private final TournamentArchiveRepository tournamentArchiveRepository;

    public TournamentArchive getByEditionCode(String editionCode) {
        String code = normalize(editionCode);
        return tournamentArchiveRepository.findByEditionCode(code)
                .orElseThrow(() -> new NotFoundException("TournamentArchive", code));
    }

    public boolean exists(String editionCode) {
        return tournamentArchiveRepository.existsByEditionCode(normalize(editionCode));
    }

    public List<TournamentArchiveMatch> matchesForStage(String editionCode, String stageFilter) {
        TournamentArchive archive = getByEditionCode(editionCode);
        List<TournamentArchiveMatch> matches = archive.getMatches() != null ? archive.getMatches() : List.of();
        if (stageFilter != null && !stageFilter.isBlank() && !"all".equalsIgnoreCase(stageFilter)) {
            matches = matches.stream()
                    .filter(m -> TournamentArchiveStages.stageMatchesFilter(m.getStage(), stageFilter))
                    .collect(Collectors.toList());
        }
        return matches.stream()
                .sorted(Comparator.comparingInt(TournamentArchiveMatch::getMatchNumber))
                .toList();
    }

    public List<TournamentArchiveMatch> knockoutMatches(String editionCode, String stageFilter) {
        return matchesForStage(editionCode, stageFilter).stream()
                .filter(m -> TournamentArchiveStages.isKnockout(m.getStage()))
                .toList();
    }

    /**
     * Display как на странице «Результаты» (compact): {@code FT (HT)\n[OT, PEN]} без подписей OT/PEN.
     */
    public static String formatScoreView(GameScore gameScore) {
        if (gameScore == null) {
            return "—";
        }
        boolean hasFt = gameScore.getFullTime() != null && !gameScore.getFullTime().isBlank();
        boolean hasHt = gameScore.getFirstTime() != null && !gameScore.getFirstTime().isBlank();
        boolean hasOt = gameScore.getOverTime() != null && !gameScore.getOverTime().isBlank();
        boolean hasPen = gameScore.getPenalty() != null && !gameScore.getPenalty().isBlank();
        if (!hasFt && !hasOt && !hasPen) {
            return "—";
        }
        StringBuilder result = new StringBuilder();
        if (hasFt) {
            result.append(gameScore.getFullTime().trim());
            if (hasHt) {
                result.append(" (").append(gameScore.getFirstTime().trim()).append(")");
            }
        } else if (hasOt) {
            result.append(gameScore.getOverTime().trim());
        }
        if (hasOt || hasPen) {
            StringBuilder extras = new StringBuilder();
            if (hasOt && hasFt) {
                extras.append(gameScore.getOverTime().trim());
            }
            if (hasPen) {
                if (extras.length() > 0) {
                    extras.append(", ");
                }
                extras.append(gameScore.getPenalty().trim());
            }
            if (extras.length() > 0) {
                result.append('\n').append('[').append(extras).append(']');
            }
        }
        return result.toString();
    }

    private static String normalize(String editionCode) {
        if (editionCode == null || editionCode.isBlank()) {
            return WC_2026;
        }
        return editionCode.trim().toUpperCase(Locale.ROOT);
    }
}
