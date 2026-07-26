package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.ErrorLog;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLogDto {

    private String id;
    private Instant createdAt;
    private String severity;
    private String layer;
    private String provider;
    private String providerRole;
    private String code;
    private String message;
    private String leagueCode;
    private String season;
    private Integer matchday;
    private String matchScheduleId;
    private String externalMatchId;
    private String homeTeam;
    private String awayTeam;
    private Map<String, String> context;

    public static ErrorLogDto from(ErrorLog log) {
        if (log == null) {
            return null;
        }
        Map<String, String> ctx = log.getContext() != null
                ? new LinkedHashMap<>(log.getContext())
                : new LinkedHashMap<>();
        return ErrorLogDto.builder()
                .id(log.getId())
                .createdAt(log.getCreatedAt())
                .severity(log.getSeverity())
                .layer(log.getLayer())
                .provider(log.getProvider())
                .providerRole(log.getProviderRole())
                .code(log.getCode())
                .message(log.getMessage())
                .leagueCode(log.getLeagueCode())
                .season(log.getSeason())
                .matchday(log.getMatchday())
                .matchScheduleId(log.getMatchScheduleId())
                .externalMatchId(log.getExternalMatchId())
                .homeTeam(log.getHomeTeam())
                .awayTeam(log.getAwayTeam())
                .context(ctx)
                .build();
    }

    public static List<ErrorLogDto> fromList(List<ErrorLog> logs) {
        if (logs == null) {
            return List.of();
        }
        return logs.stream().map(ErrorLogDto::from).toList();
    }
}
