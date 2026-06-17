package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.MatchResultSyncSettingsDto;
import net.friendly_bets.dto.PatchMatchResultSyncSettingsDto;
import net.friendly_bets.gameresults.config.MatchResultSyncProperties;
import net.friendly_bets.models.gameresults.MatchResultSyncSettings;
import net.friendly_bets.repositories.MatchResultSyncSettingsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatchResultSyncSettingsService {

    private static final Set<String> KNOWN_PROVIDERS = Set.of(
            MatchDataProviders.FOURSCORE,
            MatchDataProviders.TWENTYFOUR_SCORE
    );

    private final MatchResultSyncProperties properties;
    private final MatchResultSyncSettingsRepository repository;

    public EffectiveMatchResultSyncSettings getEffective() {
        MatchResultSyncSettings stored = repository.findById(MatchResultSyncSettings.SINGLETON_ID).orElse(null);
        return EffectiveMatchResultSyncSettings.builder()
                .primaryProvider(pick(stored != null ? stored.getPrimaryProvider() : null, properties.getPrimaryProvider()))
                .secondaryProvider(pick(stored != null ? stored.getSecondaryProvider() : null, properties.getSecondaryProvider()))
                .dualVerificationEnabled(pickBool(stored != null ? stored.getDualVerificationEnabled() : null, properties.isDualVerificationEnabled()))
                .requireStablePolls(pickInt(stored != null ? stored.getRequireStablePolls() : null, properties.getRequireStablePolls()))
                .minMinutesAfterKickoff(pickInt(stored != null ? stored.getMinMinutesAfterKickoff() : null, properties.getMinMinutesAfterKickoff()))
                .autoSettleEnabled(pickBool(stored != null ? stored.getAutoSettleEnabled() : null, properties.isAutoSettleEnabled()))
                .build();
    }

    public MatchResultSyncSettingsDto toDto() {
        EffectiveMatchResultSyncSettings effective = getEffective();
        return MatchResultSyncSettingsDto.builder()
                .primaryProvider(effective.getPrimaryProvider())
                .secondaryProvider(effective.getSecondaryProvider())
                .dualVerificationEnabled(effective.isDualVerificationEnabled())
                .requireStablePolls(effective.getRequireStablePolls())
                .minMinutesAfterKickoff(effective.getMinMinutesAfterKickoff())
                .autoSettleEnabled(effective.isAutoSettleEnabled())
                .build();
    }

    public MatchResultSyncSettingsDto patch(PatchMatchResultSyncSettingsDto patch) {
        validatePatch(patch);
        MatchResultSyncSettings stored = repository.findById(MatchResultSyncSettings.SINGLETON_ID)
                .orElse(MatchResultSyncSettings.builder().id(MatchResultSyncSettings.SINGLETON_ID).build());

        if (patch.getPrimaryProvider() != null) {
            stored.setPrimaryProvider(patch.getPrimaryProvider().trim());
        }
        if (patch.getSecondaryProvider() != null) {
            stored.setSecondaryProvider(patch.getSecondaryProvider().trim());
        }
        if (patch.getDualVerificationEnabled() != null) {
            stored.setDualVerificationEnabled(patch.getDualVerificationEnabled());
        }
        if (patch.getRequireStablePolls() != null) {
            stored.setRequireStablePolls(patch.getRequireStablePolls());
        }
        if (patch.getMinMinutesAfterKickoff() != null) {
            stored.setMinMinutesAfterKickoff(patch.getMinMinutesAfterKickoff());
        }
        if (patch.getAutoSettleEnabled() != null) {
            stored.setAutoSettleEnabled(patch.getAutoSettleEnabled());
        }
        stored.setUpdatedAt(LocalDateTime.now());
        repository.save(stored);
        return toDto();
    }

    private static void validatePatch(PatchMatchResultSyncSettingsDto patch) {
        if (patch.getPrimaryProvider() != null && !KNOWN_PROVIDERS.contains(patch.getPrimaryProvider().trim())) {
            throw new net.friendly_bets.exceptions.BadRequestException("invalidMatchResultProvider");
        }
        if (patch.getSecondaryProvider() != null && !KNOWN_PROVIDERS.contains(patch.getSecondaryProvider().trim())) {
            throw new net.friendly_bets.exceptions.BadRequestException("invalidMatchResultProvider");
        }
    }

    private static String pick(String override, String fallback) {
        return override != null && !override.isBlank() ? override.trim() : fallback;
    }

    private static boolean pickBool(Boolean override, boolean fallback) {
        return override != null ? override : fallback;
    }

    private static int pickInt(Integer override, int fallback) {
        return override != null ? override : fallback;
    }

    @lombok.Builder
    @lombok.Value
    public static class EffectiveMatchResultSyncSettings {
        String primaryProvider;
        String secondaryProvider;
        boolean dualVerificationEnabled;
        int requireStablePolls;
        int minMinutesAfterKickoff;
        boolean autoSettleEnabled;
    }
}
