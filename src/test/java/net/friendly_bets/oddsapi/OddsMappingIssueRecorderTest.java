package net.friendly_bets.oddsapi;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.oddsapi.mapping.BetTitleKey;
import net.friendly_bets.oddsapi.mapping.OddsCrossBookmakerMismatch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OddsMappingIssueRecorderTest {

    @Test
    void formatsMismatchWithBetTitleLabel() {
        OddsCrossBookmakerMismatch mismatch = OddsCrossBookmakerMismatch.builder()
                .betTitleKey(BetTitleKey.from(BetTitle.builder()
                        .code(BetTitleCode.AWAY_WIN.getCode())
                        .label(BetTitleCode.AWAY_WIN.getLabel())
                        .isNot(false)
                        .build()))
                .betTitle(BetTitle.builder()
                        .code(BetTitleCode.AWAY_WIN.getCode())
                        .label(BetTitleCode.AWAY_WIN.getLabel())
                        .isNot(false)
                        .build())
                .bookmakerA("1xbet")
                .oddsA("60.000")
                .bookmakerB("Bet365")
                .oddsB("34.000")
                .build();

        assertEquals(
                "Есть возможные проблемы с кэфами: betTitle=103:0 (П2) 1xbet=60.000 Bet365=34.000",
                OddsMappingIssueRecorder.formatCrossBookmakerMismatchMessage(mismatch));
    }
}
