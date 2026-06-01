package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OddsMappingTraceEntryDto {

    String bookmaker;
    String marketName;
    String rawRowJson;
    String category;
    String mappingStatus;
    String rejectReason;
    String rejectDetail;
    Short betTitleCode;
    Boolean betTitleIsNot;
    String betTitleLabel;
    String odds;
    String selectionCode;
    String line;
}
