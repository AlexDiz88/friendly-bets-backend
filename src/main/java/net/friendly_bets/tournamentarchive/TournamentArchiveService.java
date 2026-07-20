package net.friendly_bets.tournamentarchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.tournamentarchive.TournamentArchive;
import net.friendly_bets.models.tournamentarchive.TournamentArchiveBestThirdRow;
import net.friendly_bets.models.tournamentarchive.TournamentArchiveMatch;
import net.friendly_bets.models.tournamentarchive.TournamentArchiveStandingRow;
import net.friendly_bets.repositories.TournamentArchiveRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentArchiveService {

    public static final String WC_2026 = "WC_2026";

    private final TournamentArchiveRepository tournamentArchiveRepository;
    private final TournamentArchiveTeamResolver teamResolver;
    private final ObjectMapper objectMapper;

    public TournamentArchive getByEditionCode(String editionCode) {
        String code = normalize(editionCode);
        return tournamentArchiveRepository.findByEditionCode(code)
                .orElseThrow(() -> new NotFoundException("TournamentArchive", code));
    }

    public boolean exists(String editionCode) {
        return tournamentArchiveRepository.existsByEditionCode(normalize(editionCode));
    }

    @Transactional
    public TournamentArchive importArchive(TournamentArchive archive) {
        if (archive == null || archive.getEditionCode() == null || archive.getEditionCode().isBlank()) {
            throw new BadRequestException("tournamentArchiveInvalid");
        }
        String code = normalize(archive.getEditionCode());
        archive.setEditionCode(code);
        archive.setImportedAt(LocalDateTime.now());

        teamResolver.reset();
        resolveTeamIds(archive);
        stripImportOnlyFifaCodes(archive);

        archive.setUnresolvedTeams(teamResolver.unresolvedCodes().stream().sorted().toList());

        tournamentArchiveRepository.findByEditionCode(code).ifPresent(existing -> {
            // Перезаписываем тот же документ; id всегда String (как в остальных коллекциях).
            String existingId = existing.getId();
            if (existingId != null && !existingId.isBlank()) {
                archive.setId(existingId);
            }
        });
        if (archive.getId() == null || archive.getId().isBlank()) {
            // Hex-строка, не BSON ObjectId — тот же стиль, что у Team/Season/...
            archive.setId(new ObjectId().toHexString());
        }
        return tournamentArchiveRepository.save(archive);
    }

    private void resolveTeamIds(TournamentArchive archive) {
        if (archive.getMatches() != null) {
            for (TournamentArchiveMatch match : archive.getMatches()) {
                if (isBlank(match.getHomeTeamId())) {
                    match.setHomeTeamId(teamResolver.resolveTeamId(match.getHomeTeamFifaCode()));
                }
                if (isBlank(match.getAwayTeamId())) {
                    match.setAwayTeamId(teamResolver.resolveTeamId(match.getAwayTeamFifaCode()));
                }
                if (isBlank(match.getWinnerTeamId())) {
                    match.setWinnerTeamId(teamResolver.resolveTeamId(match.getWinnerTeamFifaCode()));
                }
            }
        }
        if (archive.getGroupStandings() != null) {
            for (TournamentArchiveStandingRow row : archive.getGroupStandings()) {
                if (isBlank(row.getTeamId())) {
                    row.setTeamId(teamResolver.resolveTeamId(row.getFifaCode()));
                }
            }
        }
        if (archive.getBestThirdPlaces() != null) {
            for (TournamentArchiveBestThirdRow row : archive.getBestThirdPlaces()) {
                if (isBlank(row.getTeamId())) {
                    row.setTeamId(teamResolver.resolveTeamId(row.getFifaCode()));
                }
            }
        }
    }

    /**
     * FIFA-коды нужны только на этапе import; в Mongo не храним.
     */
    private static void stripImportOnlyFifaCodes(TournamentArchive archive) {
        if (archive.getMatches() != null) {
            for (TournamentArchiveMatch match : archive.getMatches()) {
                match.setHomeTeamFifaCode(null);
                match.setAwayTeamFifaCode(null);
                match.setWinnerTeamFifaCode(null);
            }
        }
        if (archive.getGroupStandings() != null) {
            for (TournamentArchiveStandingRow row : archive.getGroupStandings()) {
                row.setFifaCode(null);
            }
        }
        if (archive.getBestThirdPlaces() != null) {
            for (TournamentArchiveBestThirdRow row : archive.getBestThirdPlaces()) {
                row.setFifaCode(null);
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Transactional
    public TournamentArchive importFromReviewFile(String editionCode) {
        Path path = reviewJsonPath(editionCode);
        if (!Files.isRegularFile(path)) {
            throw new BadRequestException("tournamentArchiveReviewFileMissing");
        }
        try {
            TournamentArchive archive = objectMapper.readValue(path.toFile(), TournamentArchive.class);
            return importArchive(archive);
        } catch (IOException e) {
            throw new BadRequestException("tournamentArchiveImportParseError");
        }
    }

    public Path reviewJsonPath(String editionCode) {
        String code = normalize(editionCode);
        String fileName = "tournament-archive-" + code.toLowerCase(Locale.ROOT).replace('_', '-') + ".json";
        return Paths.get("data", fileName);
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
     * Display близкий к frontend getGameScoreView (без i18n): FT (HT) [OT… PEN…].
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
                extras.append("OT").append(gameScore.getOverTime().trim());
            }
            if (hasPen) {
                if (extras.length() > 0) {
                    extras.append(' ');
                }
                extras.append("PEN").append(gameScore.getPenalty().trim());
            }
            if (extras.length() > 0) {
                result.append(" [").append(extras).append(']');
            } else if (hasPen && !hasFt) {
                result.append(" [PEN").append(gameScore.getPenalty().trim()).append(']');
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
