package net.friendly_bets.services;

import net.friendly_bets.dto.ClientVersionDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.AppSettings;
import net.friendly_bets.repositories.AppSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientVersionServiceTest {

    @Mock
    AppSettingsService appSettingsService;

    @Mock
    AppSettingsRepository appSettingsRepository;

    @InjectMocks
    ClientVersionService clientVersionService;

    private AppSettings settings;

    @BeforeEach
    void setUp() {
        settings = AppSettings.builder()
                .id(AppSettings.DEFAULT_ID)
                .clientVersion(AppSettings.ClientVersionBlock.builder().buildId("100").build())
                .build();
    }

    @Test
    void getCurrent_returnsStoredBuildId() {
        when(appSettingsRepository.findById(AppSettings.DEFAULT_ID)).thenReturn(Optional.of(settings));

        ClientVersionDto dto = clientVersionService.getCurrent();

        assertEquals("100", dto.getBuildId());
    }

    @Test
    void setIfNewer_updatesWhenIncomingIsNewer() {
        when(appSettingsService.getOrCreate()).thenReturn(settings);
        when(appSettingsService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientVersionDto dto = clientVersionService.setIfNewer("200");

        assertEquals("200", dto.getBuildId());
        ArgumentCaptor<AppSettings> captor = ArgumentCaptor.forClass(AppSettings.class);
        verify(appSettingsService).save(captor.capture());
        assertEquals("200", captor.getValue().getClientVersion().getBuildId());
    }

    @Test
    void setIfNewer_keepsCurrentWhenIncomingIsOlder() {
        when(appSettingsService.getOrCreate()).thenReturn(settings);

        ClientVersionDto dto = clientVersionService.setIfNewer("50");

        assertEquals("100", dto.getBuildId());
    }

    @Test
    void setIfNewer_rejectsInvalidBuildId() {
        assertThrows(BadRequestException.class, () -> clientVersionService.setIfNewer("abc"));
    }
}
