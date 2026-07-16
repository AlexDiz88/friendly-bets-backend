package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ClientVersionDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.ClientVersion;
import net.friendly_bets.repositories.ClientVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClientVersionService {

    private final ClientVersionRepository clientVersionRepository;

    @Transactional(readOnly = true)
    public ClientVersionDto getCurrent() {
        return clientVersionRepository.findById(ClientVersion.CURRENT_ID)
                .map(doc -> ClientVersionDto.builder().buildId(doc.getBuildId()).build())
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

        ClientVersion current = clientVersionRepository.findById(ClientVersion.CURRENT_ID).orElse(null);
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

        ClientVersion saved = clientVersionRepository.save(ClientVersion.builder()
                .id(ClientVersion.CURRENT_ID)
                .buildId(buildId)
                .updatedAt(LocalDateTime.now())
                .build());

        return ClientVersionDto.builder().buildId(saved.getBuildId()).build();
    }
}
