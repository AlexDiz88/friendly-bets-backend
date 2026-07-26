package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.Soccer365ScheduleSyncResultDto;
import net.friendly_bets.dto.Soccer365TeamNameChipDto;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.LayerProviderRouter;
import net.friendly_bets.providers.ScheduleProvider;
import net.friendly_bets.services.ExternalTeamNamesService;
import net.friendly_bets.utils.TeamTitleUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/soccer365")
public class Soccer365AdminController {

    private final ExternalTeamNamesService externalTeamNamesService;
    private final LayerProviderRouter router;

    /** @deprecated prefer {@code POST /api/admin/external-data/team-names} */
    @PostMapping("/team-names")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Soccer365TeamNameChipDto>> fetchTeamNames(
            @RequestParam String leagueCode
    ) {
        return ResponseEntity.ok(externalTeamNamesService.fetchUnmappedTeamNames(
                TeamTitleUtils.SOCCER365_PROVIDER, leagueCode));
    }

    /** @deprecated prefer {@code POST /api/admin/external-data/schedule/sync} */
    @PostMapping("/sync-schedule")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Soccer365ScheduleSyncResultDto> syncSchedule(
            @RequestParam String leagueCode,
            @RequestParam(required = false) Integer matchday
    ) {
        return ResponseEntity.ok(router.execute(
                ExternalDataLayer.SCHEDULE,
                ScheduleProvider.class,
                p -> p.syncByLeagueCode(leagueCode, matchday),
                leagueCode
        ));
    }
}
