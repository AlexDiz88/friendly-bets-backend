package net.friendly_bets.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ErrorLogServiceContextSuffixTest {

    @Test
    void omitsAbsentMatchdayAndTeams() {
        String message = ErrorLogService.formatInformativeMessage(
                "sportsRuFetchFailed",
                "SCHEDULE",
                "sports.ru",
                "EPL",
                null,
                null,
                null
        );
        assertEquals(
                "sportsRuFetchFailed | слой: SCHEDULE; провайдер: sports.ru; лига: EPL",
                message
        );
        assertFalse(message.contains("тур"));
        assertFalse(message.contains("команд"));
        assertFalse(message.contains("недоступн"));
    }

    @Test
    void includesMatchdayAndTeamsWhenKnown() {
        assertEquals(
                "fullMatchNotFound | слой: FULL_MATCH; провайдер: flashscorekz.com; лига: EPL; тур: 6; команды: A — B",
                ErrorLogService.formatInformativeMessage(
                        "fullMatchNotFound",
                        "FULL_MATCH",
                        "flashscorekz.com",
                        "EPL",
                        6,
                        "A",
                        "B"
                )
        );
    }

    @Test
    void stripsLegacyUnavailablePlaceholdersAndRebuilds() {
        String legacy = "sportsRuFetchFailed | слой: SCHEDULE; провайдер: sports.ru; лига: CL; "
                + "данные о туре недоступны; информация о командах недоступна";
        assertEquals(
                "sportsRuFetchFailed | слой: SCHEDULE; провайдер: sports.ru; лига: CL",
                ErrorLogService.formatInformativeMessage(
                        legacy,
                        "SCHEDULE",
                        "sports.ru",
                        "CL",
                        null,
                        null,
                        null
                )
        );
    }

    @Test
    void stripContextSuffix_leavesMessageWithoutSuffixIntact() {
        assertEquals(
                "httpSuccess=0/1; SCHEDULE_PAGE:HTTP_ERROR",
                ErrorLogService.stripContextSuffix("httpSuccess=0/1; SCHEDULE_PAGE:HTTP_ERROR")
        );
    }
}
