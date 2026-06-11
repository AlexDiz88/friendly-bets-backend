package net.friendly_bets.fourscore;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class FourScorePreviewMatchDto {
    String section;
    String eventSlug;
    String eventPath;
    String homeTeamName;
    String awayTeamName;
    String statusText;
    Integer listHomeScore;
    Integer listAwayScore;
    String homeTeamTitle;
    String awayTeamTitle;
    boolean homeMapped;
    boolean awayMapped;
    String firstHalfScore;
    String secondHalfScore;
    String extraTimeScore;
    String penaltyScore;
    String fullTimeScore;
    boolean detailsLoaded;
    String detailsError;
    LocalDateTime fetchedAt;
}
