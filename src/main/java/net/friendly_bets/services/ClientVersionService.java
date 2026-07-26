package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ClientVersionDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.AppSettings;
import net.friendly_bets.repositories.AppSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClientVersionService {

    private final AppSettingsService appSettingsService;
    private final AppSettingsRepository appSettingsRepository;

    @Transactional(readOnly = true)
    public ClientVersionDto getCurrent() {
        return appSettingsRepository.findById(AppSettings.DEFAULT_ID)
                .map(AppSettings::getClientVersion)
                .map(block -> ClientVersionDto.builder().buildId(block != null ? block.getBuildId() : null).build())
                .orElseGet(() -> ClientVersionDto.builder().buildId(null).build());
    }

    /**
     * Принимает новый buildId только если он больше текущего (миллисекунды сборки).
     * Старые клиенты не могут откатить версию назад.
     */
    @Transactional
    public ClientVersionDto registerIfNewer(String buildId) {
        long incoming;
        try {
            incoming = Long.parseLong(buildId);
        } catch (NumberFormatException e) {
            throw new BadRequestException("invalidClientBuildId");
        }

        AppSettings settings = appSettingsService.getOrCreate();
        AppSettings.ClientVersionBlock current = settings.getClientVersion();
        if (current != null && current.getBuildId() != null) {
            long existing;
            try {
                existing = Long.parseLong(current.getBuildId());
            } catch (NumberFormatException e) {
                existing = -1L;
            }
            if (incoming <= existing) {
                return ClientVersionDto.builder().buildId(current.getBuildId()).build();
            }
        }

        AppSettings.ClientVersionBlock next = AppSettings.ClientVersionBlock.builder()
                .buildId(buildId)
                .updatedAt(LocalDateTime.now())
                .build();
        settings.setClientVersion(next);
        appSettingsService.save(settings);
        return ClientVersionDto.builder().buildId(next.getBuildId()).build();
    }
}
