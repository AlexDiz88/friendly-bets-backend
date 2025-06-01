package net.friendly_bets.dto;

import lombok.*;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.validation.betcheckers.BetChecker;
import net.friendly_bets.validation.betcheckers.GameResultChecker;
import net.friendly_bets.validation.betcheckers.PlayoffChecker;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Getter
@AllArgsConstructor
public enum BetTitleCode {

    //========== 1. Обычный исход (gameResult) — коды 100–199 ==========
    RESULT_HOME_WIN((short) 100, new GameResultChecker(GameWinner.HOME)),          // «П1»
    RESULT_DRAW((short) 101),              // «Х»
    RESULT_AWAY_WIN((short) 102),          // «П2»
    DOUBLE_CHANCE_HOME_OR_DRAW((short) 103),   // «1Х»
    DOUBLE_CHANCE_HOME_OR_AWAY((short) 104),   // «12»
    DOUBLE_CHANCE_DRAW_OR_AWAY((short) 105),   // «Х2»

    //========== 2. П1 + Тотал меньше (Home win + Goals UNDER) — коды 200–299 ==========
    HOME_WIN_AND_UNDER_1_5((short) 200),   // «П1 + ТМ 1,5»
    HOME_WIN_AND_UNDER_2_0((short) 201),   // «П1 + ТМ 2»
    HOME_WIN_AND_UNDER_2_5((short) 202),   // «П1 + ТМ 2,5»
    HOME_WIN_AND_UNDER_3_0((short) 203),   // «П1 + ТМ 3»
    HOME_WIN_AND_UNDER_3_5((short) 204),   // «П1 + ТМ 3,5»
    HOME_WIN_AND_UNDER_4_0((short) 205),   // «П1 + ТМ 4»
    HOME_WIN_AND_UNDER_4_5((short) 206),   // «П1 + ТМ 4,5»
    HOME_WIN_AND_UNDER_5_0((short) 207),   // «П1 + ТМ 5»
    HOME_WIN_AND_UNDER_5_5((short) 208),   // «П1 + ТМ 5,5»
    HOME_WIN_AND_UNDER_6_0((short) 209),   // «П1 + ТМ 6»

    //========== 3. П1 + Тотал больше (Home win + Goals OVER) — коды 300–399 ==========
    HOME_WIN_AND_OVER_1_5((short) 300),    // «П1 + ТБ 1,5»
    HOME_WIN_AND_OVER_2_0((short) 301),    // «П1 + ТБ 2»
    HOME_WIN_AND_OVER_2_5((short) 302),    // «П1 + ТБ 2,5»
    HOME_WIN_AND_OVER_3_0((short) 303),    // «П1 + ТБ 3»
    HOME_WIN_AND_OVER_3_5((short) 304),    // «П1 + ТБ 3,5»
    HOME_WIN_AND_OVER_4_0((short) 305),    // «П1 + ТБ 4»
    HOME_WIN_AND_OVER_4_5((short) 306),    // «П1 + ТБ 4,5»
    HOME_WIN_AND_OVER_5_0((short) 307),    // «П1 + ТБ 5»
    HOME_WIN_AND_OVER_5_5((short) 308),    // «П1 + ТБ 5,5»
    HOME_WIN_AND_OVER_6_0((short) 309),    // «П1 + ТБ 6»

    //========== 4. 1Х + Тотал меньше (Home win or Draw (1X) + UNDER) — коды 400–499 ==========
    HOME_OR_DRAW_AND_UNDER_1_5((short) 400), // «1Х + ТМ 1,5»
    HOME_OR_DRAW_AND_UNDER_2_0((short) 401), // «1Х + ТМ 2»
    HOME_OR_DRAW_AND_UNDER_2_5((short) 402), // «1Х + ТМ 2,5»
    HOME_OR_DRAW_AND_UNDER_3_0((short) 403), // «1Х + ТМ 3»
    HOME_OR_DRAW_AND_UNDER_3_5((short) 404), // «1Х + ТМ 3,5»
    HOME_OR_DRAW_AND_UNDER_4_0((short) 405), // «1Х + ТМ 4»
    HOME_OR_DRAW_AND_UNDER_4_5((short) 406), // «1Х + ТМ 4,5»
    HOME_OR_DRAW_AND_UNDER_5_0((short) 407), // «1Х + ТМ 5»
    HOME_OR_DRAW_AND_UNDER_5_5((short) 408), // «1Х + ТМ 5,5»
    HOME_OR_DRAW_AND_UNDER_6_0((short) 409), // «1Х + ТМ 6»

    //========== 5. 1Х + Тотал больше (Home win or Draw (1X) + OVER) — коды 500–599 ==========
    HOME_OR_DRAW_AND_OVER_1_5((short) 500),  // «1Х + ТБ 1,5»
    HOME_OR_DRAW_AND_OVER_2_0((short) 501),  // «1Х + ТБ 2»
    HOME_OR_DRAW_AND_OVER_2_5((short) 502),  // «1Х + ТБ 2,5»
    HOME_OR_DRAW_AND_OVER_3_0((short) 503),  // «1Х + ТБ 3»
    HOME_OR_DRAW_AND_OVER_3_5((short) 504),  // «1Х + ТБ 3,5»
    HOME_OR_DRAW_AND_OVER_4_0((short) 505),  // «1Х + ТБ 4»
    HOME_OR_DRAW_AND_OVER_4_5((short) 506),  // «1Х + ТБ 4,5»
    HOME_OR_DRAW_AND_OVER_5_0((short) 507),  // «1Х + ТБ 5»
    HOME_OR_DRAW_AND_OVER_5_5((short) 508),  // «1Х + ТБ 5,5»
    HOME_OR_DRAW_AND_OVER_6_0((short) 509),  // «1Х + ТБ 6»

    //========== 6. Х + Тотал голов (Draw (X) + goals total) — коды 600–699 ==========
    DRAW_AND_UNDER_1_0((short) 600),  // «Х + ТМ 1»
    DRAW_AND_UNDER_1_5((short) 601),  // «Х + ТМ 1,5»
    DRAW_AND_UNDER_2_0((short) 602),  // «Х + ТМ 2»
    DRAW_AND_UNDER_2_5((short) 603),  // «Х + ТМ 2,5»
    DRAW_AND_UNDER_3_0((short) 604),  // «Х + ТМ 3»
    DRAW_AND_UNDER_3_5((short) 605),  // «Х + ТМ 3,5»
    DRAW_AND_UNDER_4_0((short) 606),  // «Х + ТМ 4»
    DRAW_AND_UNDER_4_5((short) 607),  // «Х + ТМ 4,5»
    DRAW_AND_UNDER_5_0((short) 608),  // «Х + ТМ 5»
    DRAW_AND_UNDER_5_5((short) 609),  // «Х + ТМ 5,5»
    DRAW_AND_UNDER_6_0((short) 610),  // «Х + ТМ 6»

    DRAW_AND_OVER_1_0((short) 650),  // «Х + ТБ 1»
    DRAW_AND_OVER_1_5((short) 651),  // «Х + ТБ 1,5»
    DRAW_AND_OVER_2_0((short) 652),  // «Х + ТБ 2»
    DRAW_AND_OVER_2_5((short) 653),  // «Х + ТБ 2,5»
    DRAW_AND_OVER_3_0((short) 654),  // «Х + ТБ 3»
    DRAW_AND_OVER_3_5((short) 655),  // «Х + ТБ 3,5»
    DRAW_AND_OVER_4_0((short) 656),  // «Х + ТБ 4»
    DRAW_AND_OVER_4_5((short) 657),  // «Х + ТБ 4,5»
    DRAW_AND_OVER_5_0((short) 658),  // «Х + ТБ 5»
    DRAW_AND_OVER_5_5((short) 659),  // «Х + ТБ 5,5»
    DRAW_AND_OVER_6_0((short) 660),  // «Х + ТБ 6»

    //========== 7. П2 + Тотал меньше (Away win + Goals UNDER) — коды 700–799 ==========
    AWAY_WIN_AND_UNDER_1_5((short) 700),   // «П2 + ТМ 1,5»
    AWAY_WIN_AND_UNDER_2_0((short) 701),   // «П2 + ТМ 2»
    AWAY_WIN_AND_UNDER_2_5((short) 702),   // «П2 + ТМ 2,5»
    AWAY_WIN_AND_UNDER_3_0((short) 703),   // «П2 + ТМ 3»
    AWAY_WIN_AND_UNDER_3_5((short) 704),   // «П2 + ТМ 3,5»
    AWAY_WIN_AND_UNDER_4_0((short) 705),   // «П2 + ТМ 4»
    AWAY_WIN_AND_UNDER_4_5((short) 706),   // «П2 + ТМ 4,5»
    AWAY_WIN_AND_UNDER_5_0((short) 707),   // «П2 + ТМ 5»
    AWAY_WIN_AND_UNDER_5_5((short) 708),   // «П2 + ТМ 5,5»
    AWAY_WIN_AND_UNDER_6_0((short) 709),   // «П2 + ТМ 6»

    //========== 8. П2 + Тотал больше (Away win + Goals OVER) — коды 800–899 ==========
    AWAY_WIN_AND_OVER_1_5((short) 800),    // «П2 + ТБ 1,5»
    AWAY_WIN_AND_OVER_2_0((short) 801),    // «П2 + ТБ 2»
    AWAY_WIN_AND_OVER_2_5((short) 802),    // «П2 + ТБ 2,5»
    AWAY_WIN_AND_OVER_3_0((short) 803),    // «П2 + ТБ 3»
    AWAY_WIN_AND_OVER_3_5((short) 804),    // «П2 + ТБ 3,5»
    AWAY_WIN_AND_OVER_4_0((short) 805),    // «П2 + ТБ 4»
    AWAY_WIN_AND_OVER_4_5((short) 806),    // «П2 + ТБ 4,5»
    AWAY_WIN_AND_OVER_5_0((short) 807),    // «П2 + ТБ 5»
    AWAY_WIN_AND_OVER_5_5((short) 808),    // «П2 + ТБ 5,5»
    AWAY_WIN_AND_OVER_6_0((short) 809),    // «П2 + ТБ 6»

    //========== 9. Х2 + Тотал меньше (Draw or Away win + UNDER) — коды 900–999 ==========
    DRAW_OR_AWAY_AND_UNDER_1_5((short) 900), // «Х2 + ТМ 1,5»
    DRAW_OR_AWAY_AND_UNDER_2_0((short) 901), // «Х2 + ТМ 2»
    DRAW_OR_AWAY_AND_UNDER_2_5((short) 902), // «Х2 + ТМ 2,5»
    DRAW_OR_AWAY_AND_UNDER_3_0((short) 903), // «Х2 + ТМ 3»
    DRAW_OR_AWAY_AND_UNDER_3_5((short) 904), // «Х2 + ТМ 3,5»
    DRAW_OR_AWAY_AND_UNDER_4_0((short) 905), // «Х2 + ТМ 4»
    DRAW_OR_AWAY_AND_UNDER_4_5((short) 906), // «Х2 + ТМ 4,5»
    DRAW_OR_AWAY_AND_UNDER_5_0((short) 907), // «Х2 + ТМ 5»
    DRAW_OR_AWAY_AND_UNDER_5_5((short) 908), // «Х2 + ТМ 5,5»
    DRAW_OR_AWAY_AND_UNDER_6_0((short) 909), // «Х2 + ТМ 6»

    //========== 10. Х2 + Тотал больше (Draw or Away win + OVER) — коды 1000–1099 ==========
    DRAW_OR_AWAY_AND_OVER_1_5((short) 1000),  // «Х2 + ТБ 1,5»
    DRAW_OR_AWAY_AND_OVER_2_0((short) 1001),  // «Х2 + ТБ 2»
    DRAW_OR_AWAY_AND_OVER_2_5((short) 1002),  // «Х2 + ТБ 2,5»
    DRAW_OR_AWAY_AND_OVER_3_0((short) 1003),  // «Х2 + ТБ 3»
    DRAW_OR_AWAY_AND_OVER_3_5((short) 1004),  // «Х2 + ТБ 3,5»
    DRAW_OR_AWAY_AND_OVER_4_0((short) 1005),  // «Х2 + ТБ 4»
    DRAW_OR_AWAY_AND_OVER_4_5((short) 1006),  // «Х2 + ТБ 4,5»
    DRAW_OR_AWAY_AND_OVER_5_0((short) 1007),  // «Х2 + ТБ 5»
    DRAW_OR_AWAY_AND_OVER_5_5((short) 1008),  // «Х2 + ТБ 5,5»
    DRAW_OR_AWAY_AND_OVER_6_0((short) 1009),  // «Х2 + ТБ 6»

    //========== 11. 12 + Тотал меньше (Home or Away win + UNDER) — коды 1100–1199 ==========
    HOME_OR_AWAY_AND_UNDER_1_5((short) 1100), // «12 + ТМ 1,5»
    HOME_OR_AWAY_AND_UNDER_2_0((short) 1101), // «12 + ТМ 2»
    HOME_OR_AWAY_AND_UNDER_2_5((short) 1102), // «12 + ТМ 2,5»
    HOME_OR_AWAY_AND_UNDER_3_0((short) 1103), // «12 + ТМ 3»
    HOME_OR_AWAY_AND_UNDER_3_5((short) 1104), // «12 + ТМ 3,5»
    HOME_OR_AWAY_AND_UNDER_4_0((short) 1105), // «12 + ТМ 4»
    HOME_OR_AWAY_AND_UNDER_4_5((short) 1106), // «12 + ТМ 4,5»
    HOME_OR_AWAY_AND_UNDER_5_0((short) 1107), // «12 + ТМ 5»
    HOME_OR_AWAY_AND_UNDER_5_5((short) 1108), // «12 + ТМ 5,5»
    HOME_OR_AWAY_AND_UNDER_6_0((short) 1109), // «12 + ТМ 6»

    //========== 12. 12 + Тотал больше (Home or Away win + OVER) — коды 1200–1299 ==========
    HOME_OR_AWAY_AND_OVER_1_5((short) 1200),  // «12 + ТБ 1,5»
    HOME_OR_AWAY_AND_OVER_2_0((short) 1201),  // «12 + ТБ 2»
    HOME_OR_AWAY_AND_OVER_2_5((short) 1202),  // «12 + ТБ 2,5»
    HOME_OR_AWAY_AND_OVER_3_0((short) 1203),  // «12 + ТБ 3»
    HOME_OR_AWAY_AND_OVER_3_5((short) 1204),  // «12 + ТБ 3,5»
    HOME_OR_AWAY_AND_OVER_4_0((short) 1205),  // «12 + ТБ 4»
    HOME_OR_AWAY_AND_OVER_4_5((short) 1206),  // «12 + ТБ 4,5»
    HOME_OR_AWAY_AND_OVER_5_0((short) 1207),  // «12 + ТБ 5»
    HOME_OR_AWAY_AND_OVER_5_5((short) 1208),  // «12 + ТБ 5,5»
    HOME_OR_AWAY_AND_OVER_6_0((short) 1209),  // «12 + ТБ 6»

    //========== 13. Тотал голов Меньше (Total goals under) — коды 1300–1399 ==========
    TOTAL_UNDER_1_0((short) 1300),        // «ТМ 1»
    TOTAL_UNDER_1_5((short) 1301),        // «ТМ 1,5»
    TOTAL_UNDER_2_0((short) 1302),        // «ТМ 2»
    TOTAL_UNDER_2_5((short) 1303),        // «ТМ 2,5»
    TOTAL_UNDER_3_0((short) 1304),        // «ТМ 3»
    TOTAL_UNDER_3_5((short) 1305),        // «ТМ 3,5»
    TOTAL_UNDER_4_0((short) 1306),        // «ТМ 4»
    TOTAL_UNDER_4_5((short) 1307),        // «ТМ 4,5»
    TOTAL_UNDER_5_0((short) 1308),        // «ТМ 5»
    TOTAL_UNDER_5_5((short) 1309),        // «ТМ 5,5»
    TOTAL_UNDER_6_0((short) 1310),        // «ТМ 6»
    TOTAL_UNDER_6_5((short) 1311),        // «ТМ 6,5»

    //========== 14. Тотал голов Больше (Total goals over) — коды 1400–1499 ==========
    TOTAL_OVER_1_0((short) 1400),         // «ТБ 1»
    TOTAL_OVER_1_5((short) 1401),         // «ТБ 1,5»
    TOTAL_OVER_2_0((short) 1402),         // «ТБ 2»
    TOTAL_OVER_2_5((short) 1403),         // «ТБ 2,5»
    TOTAL_OVER_3_0((short) 1404),         // «ТБ 3»
    TOTAL_OVER_3_5((short) 1405),         // «ТБ 3,5»
    TOTAL_OVER_4_0((short) 1406),         // «ТБ 4»
    TOTAL_OVER_4_5((short) 1407),         // «ТБ 4,5»
    TOTAL_OVER_5_0((short) 1408),         // «ТБ 5»
    TOTAL_OVER_5_5((short) 1409),         // «ТБ 5,5»
    TOTAL_OVER_6_0((short) 1410),         // «ТБ 6»
    TOTAL_OVER_6_5((short) 1411),         // «ТБ 6,5»

    //========== 15. Хозяева ИТМ (Home team Under) — коды 1500–1599 ==========
    HOME_TEAM_UNDER_1_0((short) 1500),    // «Хозяева ИТМ 1»
    HOME_TEAM_UNDER_1_5((short) 1501),    // «Хозяева ИТМ 1,5»
    HOME_TEAM_UNDER_2_0((short) 1502),    // «Хозяева ИТМ 2»
    HOME_TEAM_UNDER_2_5((short) 1503),    // «Хозяева ИТМ 2,5»
    HOME_TEAM_UNDER_3_0((short) 1504),    // «Хозяева ИТМ 3»
    HOME_TEAM_UNDER_3_5((short) 1505),    // «Хозяева ИТМ 3,5»
    HOME_TEAM_UNDER_4_0((short) 1506),    // «Хозяева ИТМ 4»
    HOME_TEAM_UNDER_4_5((short) 1507),    // «Хозяева ИТМ 4,5»
    HOME_TEAM_UNDER_5_0((short) 1508),    // «Хозяева ИТМ 5»
    HOME_TEAM_UNDER_5_5((short) 1509),    // «Хозяева ИТМ 5,5»
    HOME_TEAM_UNDER_6_0((short) 1510),    // «Хозяева ИТМ 6»
    HOME_TEAM_UNDER_6_5((short) 1511),    // «Хозяева ИТМ 6,5»

    //========== 16. Хозяева ИТБ (Home team Over) — коды 1600–1699 ==========
    HOME_TEAM_OVER_1_0((short) 1600),     // «Хозяева ИТБ 1»
    HOME_TEAM_OVER_1_5((short) 1601),     // «Хозяева ИТБ 1,5»
    HOME_TEAM_OVER_2_0((short) 1602),     // «Хозяева ИТБ 2»
    HOME_TEAM_OVER_2_5((short) 1603),     // «Хозяева ИТБ 2,5»
    HOME_TEAM_OVER_3_0((short) 1604),     // «Хозяева ИТБ 3»
    HOME_TEAM_OVER_3_5((short) 1605),     // «Хозяева ИТБ 3,5»
    HOME_TEAM_OVER_4_0((short) 1606),     // «Хозяева ИТБ 4»
    HOME_TEAM_OVER_4_5((short) 1607),     // «Хозяева ИТБ 4,5»
    HOME_TEAM_OVER_5_0((short) 1608),     // «Хозяева ИТБ 5»
    HOME_TEAM_OVER_5_5((short) 1609),     // «Хозяева ИТБ 5,5»
    HOME_TEAM_OVER_6_0((short) 1610),     // «Хозяева ИТБ 6»
    HOME_TEAM_OVER_6_5((short) 1611),     // «Хозяева ИТБ 6,5»

    //========== 17. Гости ИТМ (Away team Under) — коды 1700–1799 ==========
    AWAY_TEAM_UNDER_1_0((short) 1700),    // «Гости ИТМ 1»
    AWAY_TEAM_UNDER_1_5((short) 1701),    // «Гости ИТМ 1,5»
    AWAY_TEAM_UNDER_2_0((short) 1702),    // «Гости ИТМ 2»
    AWAY_TEAM_UNDER_2_5((short) 1703),    // «Гости ИТМ 2,5»
    AWAY_TEAM_UNDER_3_0((short) 1704),    // «Гости ИТМ 3»
    AWAY_TEAM_UNDER_3_5((short) 1705),    // «Гости ИТМ 3,5»
    AWAY_TEAM_UNDER_4_0((short) 1706),    // «Гости ИТМ 4»
    AWAY_TEAM_UNDER_4_5((short) 1707),    // «Гости ИТМ 4,5»
    AWAY_TEAM_UNDER_5_0((short) 1708),    // «Гости ИТМ 5»
    AWAY_TEAM_UNDER_5_5((short) 1709),    // «Гости ИТМ 5,5»
    AWAY_TEAM_UNDER_6_0((short) 1710),    // «Гости ИТМ 6»
    AWAY_TEAM_UNDER_6_5((short) 1711),    // «Гости ИТМ 6,5»

    //========== 18. Гости ИТБ (Away team Over) — коды 1800–1899 ==========
    AWAY_TEAM_OVER_1_0((short) 1800),     // «Гости ИТБ 1»
    AWAY_TEAM_OVER_1_5((short) 1801),     // «Гости ИТБ 1,5»
    AWAY_TEAM_OVER_2_0((short) 1802),     // «Гости ИТБ 2»
    AWAY_TEAM_OVER_2_5((short) 1803),     // «Гости ИТБ 2,5»
    AWAY_TEAM_OVER_3_0((short) 1804),     // «Гости ИТБ 3»
    AWAY_TEAM_OVER_3_5((short) 1805),     // «Гости ИТБ 3,5»
    AWAY_TEAM_OVER_4_0((short) 1806),     // «Гости ИТБ 4»
    AWAY_TEAM_OVER_4_5((short) 1807),     // «Гости ИТБ 4,5»
    AWAY_TEAM_OVER_5_0((short) 1808),     // «Гости ИТБ 5»
    AWAY_TEAM_OVER_5_5((short) 1809),     // «Гости ИТБ 5,5»
    AWAY_TEAM_OVER_6_0((short) 1810),     // «Гости ИТБ 6»
    AWAY_TEAM_OVER_6_5((short) 1811),     // «Гости ИТБ 6,5»

    //========== 19. Фора хозяев (Home team handicap) — коды 1900–1999 ==========
    HANDICAP_HOME_0((short) 1900),        // «Ф1(0)»
    HANDICAP_HOME_MINUS_1((short) 1901),  // «Ф1(-1)»
    HANDICAP_HOME_PLUS_1((short) 1902),   // «Ф1(+1)»
    HANDICAP_HOME_MINUS_1_5((short) 1903),// «Ф1(-1,5)»
    HANDICAP_HOME_PLUS_1_5((short) 1904), // «Ф1(+1,5)»
    HANDICAP_HOME_MINUS_2((short) 1905),  // «Ф1(-2)»
    HANDICAP_HOME_PLUS_2((short) 1906),   // «Ф1(+2)»
    HANDICAP_HOME_MINUS_2_5((short) 1907),// «Ф1(-2,5)»
    HANDICAP_HOME_PLUS_2_5((short) 1908), // «Ф1(+2,5)»
    HANDICAP_HOME_MINUS_3((short) 1909),  // «Ф1(-3)»
    HANDICAP_HOME_PLUS_3((short) 1910),   // «Ф1(+3)»
    HANDICAP_HOME_MINUS_3_5((short) 1911),// «Ф1(-3,5)»
    HANDICAP_HOME_PLUS_3_5((short) 1912), // «Ф1(+3,5)»
    HANDICAP_HOME_MINUS_4((short) 1913),  // «Ф1(-4)»
    HANDICAP_HOME_PLUS_4((short) 1914),   // «Ф1(+4)»
    HANDICAP_HOME_MINUS_4_5((short) 1915),// «Ф1(-4,5)»
    HANDICAP_HOME_PLUS_4_5((short) 1916), // «Ф1(+4,5)»
    HANDICAP_HOME_MINUS_5((short) 1917),  // «Ф1(-5)»
    HANDICAP_HOME_PLUS_5((short) 1918),   // «Ф1(+5)»
    HANDICAP_HOME_MINUS_5_5((short) 1919),// «Ф1(-5,5)»
    HANDICAP_HOME_PLUS_5_5((short) 1920), // «Ф1(+5,5)»
    HANDICAP_HOME_MINUS_6((short) 1921),  // «Ф1(-6)»
    HANDICAP_HOME_PLUS_6((short) 1922),   // «Ф1(+6)»

    //========== 20. Фора гостей (Away team handicap) — коды 2000–2099 ==========
    HANDICAP_AWAY_0((short) 2000),        // «Ф2(0)»
    HANDICAP_AWAY_MINUS_1((short) 2001),  // «Ф2(-1)»
    HANDICAP_AWAY_PLUS_1((short) 2002),   // «Ф2(+1)»
    HANDICAP_AWAY_MINUS_1_5((short) 2003),// «Ф2(-1.5)»
    HANDICAP_AWAY_PLUS_1_5((short) 2004), // «Ф2(+1.5)»
    HANDICAP_AWAY_MINUS_2((short) 2005),  // «Ф2(-2)»
    HANDICAP_AWAY_PLUS_2((short) 2006),   // «Ф2(+2)»
    HANDICAP_AWAY_MINUS_2_5((short) 2007),// «Ф2(-2.5)»
    HANDICAP_AWAY_PLUS_2_5((short) 2008), // «Ф2(+2.5)»
    HANDICAP_AWAY_MINUS_3((short) 2009),  // «Ф2(-3)»
    HANDICAP_AWAY_PLUS_3((short) 2010),   // «Ф2(+3)»
    HANDICAP_AWAY_MINUS_3_5((short) 2011),// «Ф2(-3.5)»
    HANDICAP_AWAY_PLUS_3_5((short) 2012), // «Ф2(+3.5)»
    HANDICAP_AWAY_MINUS_4((short) 2013),  // «Ф2(-4)»
    HANDICAP_AWAY_PLUS_4((short) 2014),   // «Ф2(+4)»
    HANDICAP_AWAY_MINUS_4_5((short) 2015),// «Ф2(-4.5)»
    HANDICAP_AWAY_PLUS_4_5((short) 2016), // «Ф2(+4.5)»
    HANDICAP_AWAY_MINUS_5((short) 2017),  // «Ф2(-5)»
    HANDICAP_AWAY_PLUS_5((short) 2018),   // «Ф2(+5)»
    HANDICAP_AWAY_MINUS_5_5((short) 2019),// «Ф2(-5.5)»
    HANDICAP_AWAY_PLUS_5_5((short) 2020), // «Ф2(+5.5)»
    HANDICAP_AWAY_MINUS_6((short) 2021),  // «Ф2(-6)»
    HANDICAP_AWAY_PLUS_6((short) 2022),   // «Ф2(+6)»

    //========== 21. Счёт игры (Game Score) — коды 2100–2199 ==========
    GAME_SCORE_0_0((short) 2100),  // «Счёт 0:0»
    GAME_SCORE_1_0((short) 2101),  // «Счёт 1:0»
    GAME_SCORE_2_0((short) 2102),  // «Счёт 2:0»
    GAME_SCORE_3_0((short) 2103),  // «Счёт 3:0»
    GAME_SCORE_0_1((short) 2104),  // «Счёт 0:1»
    GAME_SCORE_1_1((short) 2105),  // «Счёт 1:1»
    GAME_SCORE_2_1((short) 2106),  // «Счёт 2:1»
    GAME_SCORE_3_1((short) 2107),  // «Счёт 3:1»
    GAME_SCORE_0_2((short) 2108),  // «Счёт 0:2»
    GAME_SCORE_1_2((short) 2109),  // «Счёт 1:2»
    GAME_SCORE_2_2((short) 2110),  // «Счёт 2:2»
    GAME_SCORE_3_2((short) 2111),  // «Счёт 3:2»
    GAME_SCORE_0_3((short) 2112),  // «Счёт 0:3»
    GAME_SCORE_1_3((short) 2113),  // «Счёт 1:3»
    GAME_SCORE_2_3((short) 2114),  // «Счёт 2:3»
    GAME_SCORE_3_3((short) 2115),  // «Счёт 3:3»

    GAME_SCORE_0_4((short) 2150),  // «Счёт 0:4»
    GAME_SCORE_4_0((short) 2151),  // «Счёт 4:0»
    GAME_SCORE_0_5((short) 2152),  // «Счёт 0:5»
    GAME_SCORE_5_0((short) 2153),  // «Счёт 5:0»
    GAME_SCORE_1_4((short) 2154),  // «Счёт 1:4»
    GAME_SCORE_4_1((short) 2155),  // «Счёт 4:1»
    GAME_SCORE_1_5((short) 2156),  // «Счёт 1:5»
    GAME_SCORE_5_1((short) 2157),  // «Счёт 5:1»
    GAME_SCORE_2_4((short) 2158),  // «Счёт 2:4»
    GAME_SCORE_4_2((short) 2159),  // «Счёт 4:2»
    GAME_SCORE_2_5((short) 2160),  // «Счёт 2:5»
    GAME_SCORE_5_2((short) 2161),  // «Счёт 5:2»
    GAME_SCORE_3_4((short) 2162),  // «Счёт 3:4»
    GAME_SCORE_4_3((short) 2163),  // «Счёт 4:3»
    GAME_SCORE_3_5((short) 2164),  // «Счёт 3:5»
    GAME_SCORE_5_3((short) 2165),  // «Счёт 5:3»
    GAME_SCORE_4_4((short) 2166),  // «Счёт 4:4»
    GAME_SCORE_4_5((short) 2167),  // «Счёт 4:5»
    GAME_SCORE_5_4((short) 2168),  // «Счёт 5:4»
    GAME_SCORE_5_5((short) 2169),  // «Счёт 5:5»

    //========== 22. Голы (Goals) — коды 2200–2299 ==========
    BOTH_TEAMS_SCORE((short) 2200),          // «Обе забьют»
    HOME_TEAM_SCORES((short) 2201),          // «Хозяева забьют»
    AWAY_TEAM_SCORES((short) 2202),          // «Гости забьют»

    //========== 23. Голы по таймам (Goals by halftimes) — коды 2300–2399 ==========
    HOME_SCORES_1ST_HALF((short) 2300),         // «Хозяева забьют в 1 тайме»
    HOME_SCORES_2ND_HALF((short) 2301),         // «Хозяева забьют во 2 тайме»
    AWAY_SCORES_1ST_HALF((short) 2302),         // «Гости забьют в 1 тайме»
    AWAY_SCORES_2ND_HALF((short) 2303),         // «Гости забьют во 2 тайме»
    HOME_SCORES_BOTH_HALVES((short) 2304),      // «Хозяева забьют в обоих таймах»
    AWAY_SCORES_BOTH_HALVES((short) 2305),      // «Гости забьют в обоих таймах»
    BOTH_TEAMS_SCORE_1ST_HALF((short) 2306),    // «Обе забьют в 1 тайме»
    BOTH_TEAMS_SCORE_2ND_HALF((short) 2307),    // «Обе забьют во 2 тайме»
    BOTH_TEAMS_SCORE_BOTH_HALVES((short) 2308), // «Обе забьют в обоих таймах»
    GOALS_IN_BOTH_HALVES((short) 2309),         // «Голы в обоих таймах»

    //========== 24. Результат матча + Обе забьют (Game result + Both team score) — коды 2400–2499 ==========
    HOME_WIN_AND_BOTH_TEAMS_SCORE((short) 2400),     // «П1 + Обе забьют»
    AWAY_WIN_AND_BOTH_TEAMS_SCORE((short) 2401),     // «П2 + Обе забьют»
    HOME_OR_DRAW_AND_BOTH_TEAMS_SCORE((short) 2402), // «1Х + Обе забьют»
    AWAY_OR_DRAW_AND_BOTH_TEAMS_SCORE((short) 2403), // «Х2 + Обе забьют»
    DRAW_AND_BOTH_TEAMS_SCORE((short) 2404),         // «Х + Обе забьют»
    HOME_OR_AWAY_AND_BOTH_TEAMS_SCORE((short) 2405), // «12 + Обе забьют»

    //========== 25. Обе забьют + тотал меньше (Both Team Score + Goals Amount (Under)) — коды 2500–2599 ==========
    BOTH_TEAMS_SCORE_AND_UNDER_1_5((short) 2500),  // «ОЗ +  ТМ 1,5»
    BOTH_TEAMS_SCORE_AND_UNDER_2((short) 2501),    // «ОЗ +  ТМ 2»
    BOTH_TEAMS_SCORE_AND_UNDER_2_5((short) 2502),  // «ОЗ +  ТМ 2,5»
    BOTH_TEAMS_SCORE_AND_UNDER_3((short) 2503),    // «ОЗ +  ТМ 3»
    BOTH_TEAMS_SCORE_AND_UNDER_3_5((short) 2504),  // «ОЗ +  ТМ 3,5»
    BOTH_TEAMS_SCORE_AND_UNDER_4((short) 2505),    // «ОЗ +  ТМ 4»
    BOTH_TEAMS_SCORE_AND_UNDER_4_5((short) 2506),  // «ОЗ +  ТМ 4,5»
    BOTH_TEAMS_SCORE_AND_UNDER_5((short) 2507),    // «ОЗ +  ТМ 5»
    BOTH_TEAMS_SCORE_AND_UNDER_5_5((short) 2508),  // «ОЗ +  ТМ 5,5»

    //========== 26. Обе забьют + тотал больше (Both Team Score + Goals Amount (Over)) — коды 2600–2699 ==========
    BOTH_TEAMS_SCORE_AND_OVER_1_5((short) 2600),   // «ОЗ +  ТБ 1,5»
    BOTH_TEAMS_SCORE_AND_OVER_2((short) 2601),     // «ОЗ +  ТБ 2»
    BOTH_TEAMS_SCORE_AND_OVER_2_5((short) 2602),   // «ОЗ +  ТБ 2,5»
    BOTH_TEAMS_SCORE_AND_OVER_3((short) 2603),     // «ОЗ +  ТБ 3»
    BOTH_TEAMS_SCORE_AND_OVER_3_5((short) 2604),   // «ОЗ +  ТБ 3,5»
    BOTH_TEAMS_SCORE_AND_OVER_4((short) 2605),     // «ОЗ +  ТБ 4»
    BOTH_TEAMS_SCORE_AND_OVER_4_5((short) 2606),   // «ОЗ +  ТБ 4,5»
    BOTH_TEAMS_SCORE_AND_OVER_5((short) 2607),     // «ОЗ +  ТБ 5»
    BOTH_TEAMS_SCORE_AND_OVER_5_5((short) 2608),   // «ОЗ +  ТБ 5,5»

    //========== 27. Любая забьет больше чем (Scores More Than) — коды 2700–2799 ==========
    ANY_TEAM_SCORES_2_OR_MORE((short) 2700), // «Любая команда забьет 2 и больше голов»
    ANY_TEAM_SCORES_3_OR_MORE((short) 2701), // «Любая команда забьет 3 и больше голов»
    ANY_TEAM_SCORES_4_OR_MORE((short) 2702), // «Любая команда забьет 4 и больше голов»
    ANY_TEAM_SCORES_5_OR_MORE((short) 2703), // «Любая команда забьет 5 и больше голов»

    // =================================================================================================
    // Ставки на таймы являются отдельной крупной группой,
    // поэтому под них выделен особый диапазон кодов, начиная с 3000
    // =================================================================================================

    //========== 30. Результаты по таймам (Half-time Results) — коды 3000–3099 ==========
    FIRST_HALF_HOME_WIN((short) 3000),          // «1й тайм: П1»
    FIRST_HALF_DRAW((short) 3001),              // «1й тайм: Х»
    FIRST_HALF_AWAY_WIN((short) 3002),          // «1й тайм: П2»
    FIRST_HALF_HOME_WIN_OR_DRAW((short) 3003),  // «1й тайм: 1Х»
    FIRST_HALF_HOME_OR_AWAY_WIN((short) 3004),  // «1й тайм: 12»
    FIRST_HALF_AWAY_WIN_OR_DRAW((short) 3005),  // «1й тайм: Х2»
    SECOND_HALF_HOME_WIN((short) 3006),         // «2й тайм: П1»
    SECOND_HALF_DRAW((short) 3007),             // «2й тайм: Х»
    SECOND_HALF_AWAY_WIN((short) 3008),         // «2й тайм: П2»
    SECOND_HALF_HOME_WIN_OR_DRAW((short) 3009), // «2й тайм: 1Х»
    SECOND_HALF_HOME_OR_AWAY_WIN((short) 3010), // «2й тайм: 12»
    SECOND_HALF_AWAY_WIN_OR_DRAW((short) 3011), // «2й тайм: Х2»
    ANY_HALF_HOME_WIN((short) 3012),            // «Любой тайм: П1»
    ANY_HALF_DRAW((short) 3013),                // «Любой тайм: Х»
    ANY_HALF_AWAY_WIN((short) 3014),            // «Любой тайм: П2»

    //========== 31. Тайм/Матч (Half Time / Full Time) — коды 3100–3199 ==========
    HALF_FULL_HOME_HOME((short) 3100),     // «Тайм/Матч: П1 / П1»
    HALF_FULL_HOME_DRAW((short) 3101),     // «Тайм/Матч: П1 / Х»
    HALF_FULL_HOME_AWAY((short) 3102),     // «Тайм/Матч: П1 / П2»
    HALF_FULL_DRAW_HOME((short) 3103),     // «Тайм/Матч: Х / П1»
    HALF_FULL_DRAW_DRAW((short) 3104),     // «Тайм/Матч: Х / Х»
    HALF_FULL_DRAW_AWAY((short) 3105),     // «Тайм/Матч: Х / П2»
    HALF_FULL_AWAY_HOME((short) 3106),     // «Тайм/Матч: П2 / П1»
    HALF_FULL_AWAY_DRAW((short) 3107),     // «Тайм/Матч: П2 / Х»
    HALF_FULL_AWAY_AWAY((short) 3108),     // «Тайм/Матч: П2 / П2»

    //========== 32. 1й/2й Тайм (1st Half / 2nd Half) — коды 3200–3299 ==========
    FIRST_SECOND_HOME_HOME((short) 3200),     // «1й/2й тайм: П1 / П1»
    FIRST_SECOND_HOME_DRAW((short) 3201),     // «1й/2й тайм: П1 / Х»
    FIRST_SECOND_HOME_AWAY((short) 3202),     // «1й/2й тайм: П1 / П2»
    FIRST_SECOND_DRAW_HOME((short) 3203),     // «1й/2й тайм: Х / П1»
    FIRST_SECOND_DRAW_DRAW((short) 3204),     // «1й/2й тайм: Х / Х»
    FIRST_SECOND_DRAW_AWAY((short) 3205),     // «1й/2й тайм: Х / П2»
    FIRST_SECOND_AWAY_HOME((short) 3206),     // «1й/2й тайм: П2 / П1»
    FIRST_SECOND_AWAY_DRAW((short) 3207),     // «1й/2й тайм: П2 / Х»
    FIRST_SECOND_AWAY_AWAY((short) 3208),     // «1й/2й тайм: П2 / П2»

    //========== 33. Счёт 1-го тайма (1st Half Score) — коды 3300–3399 ==========
    FIRST_HALF_SCORE_0_0((short) 3300),  // «1й тайм: 0:0»
    FIRST_HALF_SCORE_1_0((short) 3301),  // «1й тайм: 1:0»
    FIRST_HALF_SCORE_2_0((short) 3302),  // «1й тайм: 2:0»
    FIRST_HALF_SCORE_3_0((short) 3303),  // «1й тайм: 3:0»
    FIRST_HALF_SCORE_0_1((short) 3304),  // «1й тайм: 0:1»
    FIRST_HALF_SCORE_1_1((short) 3305),  // «1й тайм: 1:1»
    FIRST_HALF_SCORE_2_1((short) 3306),  // «1й тайм: 2:1»
    FIRST_HALF_SCORE_3_1((short) 3307),  // «1й тайм: 3:1»
    FIRST_HALF_SCORE_0_2((short) 3308),  // «1й тайм: 0:2»
    FIRST_HALF_SCORE_1_2((short) 3309),  // «1й тайм: 1:2»
    FIRST_HALF_SCORE_2_2((short) 3310),  // «1й тайм: 2:2»
    FIRST_HALF_SCORE_3_2((short) 3311),  // «1й тайм: 3:2»
    FIRST_HALF_SCORE_0_3((short) 3312),  // «1й тайм: 0:3»
    FIRST_HALF_SCORE_1_3((short) 3313),  // «1й тайм: 1:3»
    FIRST_HALF_SCORE_2_3((short) 3314),  // «1й тайм: 2:3»
    FIRST_HALF_SCORE_3_3((short) 3315),  // «1й тайм: 3:3»

    //========== 34. Счёт 2-го тайма (2nd Half Score) — коды 3400–3499 ==========
    SECOND_HALF_SCORE_0_0((short) 3400),  // «2й тайм: 0:0»
    SECOND_HALF_SCORE_1_0((short) 3401),  // «2й тайм: 1:0»
    SECOND_HALF_SCORE_2_0((short) 3402),  // «2й тайм: 2:0»
    SECOND_HALF_SCORE_3_0((short) 3403),  // «2й тайм: 3:0»
    SECOND_HALF_SCORE_0_1((short) 3404),  // «2й тайм: 0:1»
    SECOND_HALF_SCORE_1_1((short) 3405),  // «2й тайм: 1:1»
    SECOND_HALF_SCORE_2_1((short) 3406),  // «2й тайм: 2:1»
    SECOND_HALF_SCORE_3_1((short) 3407),  // «2й тайм: 3:1»
    SECOND_HALF_SCORE_0_2((short) 3408),  // «2й тайм: 0:2»
    SECOND_HALF_SCORE_1_2((short) 3409),  // «2й тайм: 1:2»
    SECOND_HALF_SCORE_2_2((short) 3410),  // «2й тайм: 2:2»
    SECOND_HALF_SCORE_3_2((short) 3411),  // «2й тайм: 3:2»
    SECOND_HALF_SCORE_0_3((short) 3412),  // «2й тайм: 0:3»
    SECOND_HALF_SCORE_1_3((short) 3413),  // «2й тайм: 1:3»
    SECOND_HALF_SCORE_2_3((short) 3414),  // «2й тайм: 2:3»
    SECOND_HALF_SCORE_3_3((short) 3415),  // «2й тайм: 3:3»

    //========== 35. Обе забьют + исход по таймам (Both Teams to Score + Half-time Result) — коды 3500–3599 ==========
    FIRST_HALF_BOTH_SCORE_AND_HOME_WIN((short) 3500), // «1й тайм: ОЗ + П1»
    FIRST_HALF_BOTH_SCORE_AND_DRAW((short) 3501),      // «1й тайм: ОЗ + Х»
    FIRST_HALF_BOTH_SCORE_AND_AWAY_WIN((short) 3502),  // «1й тайм: ОЗ + П2»
    FIRST_HALF_BOTH_SCORE_AND_HOME_OR_DRAW((short) 3503), // «1й тайм: ОЗ + 1Х»
    FIRST_HALF_BOTH_SCORE_AND_HOME_OR_AWAY((short) 3504), // «1й тайм: ОЗ + 12»
    FIRST_HALF_BOTH_SCORE_AND_AWAY_OR_DRAW((short) 3505), // «1й тайм: ОЗ + Х2»

    SECOND_HALF_BOTH_SCORE_AND_HOME_WIN((short) 3506), // «2й тайм: ОЗ + П1»
    SECOND_HALF_BOTH_SCORE_AND_DRAW((short) 3507),      // «2й тайм: ОЗ + Х»
    SECOND_HALF_BOTH_SCORE_AND_AWAY_WIN((short) 3508),  // «2й тайм: ОЗ + П2»
    SECOND_HALF_BOTH_SCORE_AND_HOME_OR_DRAW((short) 3509), // «2й тайм: ОЗ + 1Х»
    SECOND_HALF_BOTH_SCORE_AND_HOME_OR_AWAY((short) 3510), // «2й тайм: ОЗ + 12»
    SECOND_HALF_BOTH_SCORE_AND_AWAY_OR_DRAW((short) 3511), // «2й тайм: ОЗ + Х2»

    //========== 36. Фора 1й тайм (First Half Handicap) — коды 3600–3699 ==========
    FIRST_HALF_HANDICAP_HOME_0((short) 3600),        // «1й тайм: Ф1(0)»
    FIRST_HALF_HANDICAP_HOME_MINUS_1((short) 3601),  // «1й тайм: Ф1(-1)»
    FIRST_HALF_HANDICAP_HOME_PLUS_1((short) 3602),   // «1й тайм: Ф1(+1)»
    FIRST_HALF_HANDICAP_HOME_MINUS_1_5((short) 3603),// «1й тайм: Ф1(-1,5)»
    FIRST_HALF_HANDICAP_HOME_PLUS_1_5((short) 3604), // «1й тайм: Ф1(+1,5)»
    FIRST_HALF_HANDICAP_HOME_MINUS_2((short) 3605),  // «1й тайм: Ф1(-2)»
    FIRST_HALF_HANDICAP_HOME_PLUS_2((short) 3606),   // «1й тайм: Ф1(+2)»
    FIRST_HALF_HANDICAP_HOME_MINUS_2_5((short) 3607),// «1й тайм: Ф1(-2,5)»
    FIRST_HALF_HANDICAP_HOME_PLUS_2_5((short) 3608), // «1й тайм: Ф1(+2,5)»
    FIRST_HALF_HANDICAP_HOME_MINUS_3((short) 3609),  // «1й тайм: Ф1(-3)»
    FIRST_HALF_HANDICAP_HOME_PLUS_3((short) 3610),   // «1й тайм: Ф1(+3)»
    FIRST_HALF_HANDICAP_HOME_MINUS_3_5((short) 3611),// «1й тайм: Ф1(-3,5)»
    FIRST_HALF_HANDICAP_HOME_PLUS_3_5((short) 3612), // «1й тайм: Ф1(+3,5)»

    FIRST_HALF_HANDICAP_AWAY_0((short) 3650),        // «1й тайм: Ф2(0)»
    FIRST_HALF_HANDICAP_AWAY_MINUS_1((short) 3651),  // «1й тайм: Ф2(-1)»
    FIRST_HALF_HANDICAP_AWAY_PLUS_1((short) 3652),   // «1й тайм: Ф2(+1)»
    FIRST_HALF_HANDICAP_AWAY_MINUS_1_5((short) 3653),// «1й тайм: Ф2(-1,5)»
    FIRST_HALF_HANDICAP_AWAY_PLUS_1_5((short) 3654), // «1й тайм: Ф2(+1,5)»
    FIRST_HALF_HANDICAP_AWAY_MINUS_2((short) 3655),  // «1й тайм: Ф2(-2)»
    FIRST_HALF_HANDICAP_AWAY_PLUS_2((short) 3656),   // «1й тайм: Ф2(+2)»
    FIRST_HALF_HANDICAP_AWAY_MINUS_2_5((short) 3657),// «1й тайм: Ф2(-2,5)»
    FIRST_HALF_HANDICAP_AWAY_PLUS_2_5((short) 3658), // «1й тайм: Ф2(+2,5)»
    FIRST_HALF_HANDICAP_AWAY_MINUS_3((short) 3659),  // «1й тайм: Ф2(-3)»
    FIRST_HALF_HANDICAP_AWAY_PLUS_3((short) 3660),   // «1й тайм: Ф2(+3)»
    FIRST_HALF_HANDICAP_AWAY_MINUS_3_5((short) 3661),// «1й тайм: Ф2(-3,5)»
    FIRST_HALF_HANDICAP_AWAY_PLUS_3_5((short) 3662), // «1й тайм: Ф2(+3,5)»

    //========== 37. Фора 2й тайм (Second Half Handicap) — коды 3700–3799 ==========
    SECOND_HALF_HANDICAP_HOME_0((short) 3700),        // «2й тайм: Ф1(0)»
    SECOND_HALF_HANDICAP_HOME_MINUS_1((short) 3701),  // «2й тайм: Ф1(-1)»
    SECOND_HALF_HANDICAP_HOME_PLUS_1((short) 3702),   // «2й тайм: Ф1(+1)»
    SECOND_HALF_HANDICAP_HOME_MINUS_1_5((short) 3703),// «2й тайм: Ф1(-1,5)»
    SECOND_HALF_HANDICAP_HOME_PLUS_1_5((short) 3704), // «2й тайм: Ф1(+1,5)»
    SECOND_HALF_HANDICAP_HOME_MINUS_2((short) 3705),  // «2й тайм: Ф1(-2)»
    SECOND_HALF_HANDICAP_HOME_PLUS_2((short) 3706),   // «2й тайм: Ф1(+2)»
    SECOND_HALF_HANDICAP_HOME_MINUS_2_5((short) 3707),// «2й тайм: Ф1(-2,5)»
    SECOND_HALF_HANDICAP_HOME_PLUS_2_5((short) 3708), // «2й тайм: Ф1(+2,5)»
    SECOND_HALF_HANDICAP_HOME_MINUS_3((short) 3709),  // «2й тайм: Ф1(-3)»
    SECOND_HALF_HANDICAP_HOME_PLUS_3((short) 3710),   // «2й тайм: Ф1(+3)»
    SECOND_HALF_HANDICAP_HOME_MINUS_3_5((short) 3711),// «2й тайм: Ф1(-3,5)»
    SECOND_HALF_HANDICAP_HOME_PLUS_3_5((short) 3712), // «2й тайм: Ф1(+3,5)»

    SECOND_HALF_HANDICAP_AWAY_0((short) 3750),        // «2й тайм: Ф2(0)»
    SECOND_HALF_HANDICAP_AWAY_MINUS_1((short) 3751),  // «2й тайм: Ф2(-1)»
    SECOND_HALF_HANDICAP_AWAY_PLUS_1((short) 3752),   // «2й тайм: Ф2(+1)»
    SECOND_HALF_HANDICAP_AWAY_MINUS_1_5((short) 3753),// «2й тайм: Ф2(-1,5)»
    SECOND_HALF_HANDICAP_AWAY_PLUS_1_5((short) 3754), // «2й тайм: Ф2(+1,5)»
    SECOND_HALF_HANDICAP_AWAY_MINUS_2((short) 3755),  // «2й тайм: Ф2(-2)»
    SECOND_HALF_HANDICAP_AWAY_PLUS_2((short) 3756),   // «2й тайм: Ф2(+2)»
    SECOND_HALF_HANDICAP_AWAY_MINUS_2_5((short) 3757),// «2й тайм: Ф2(-2,5)»
    SECOND_HALF_HANDICAP_AWAY_PLUS_2_5((short) 3758), // «2й тайм: Ф2(+2,5)»
    SECOND_HALF_HANDICAP_AWAY_MINUS_3((short) 3759),  // «2й тайм: Ф2(-3)»
    SECOND_HALF_HANDICAP_AWAY_PLUS_3((short) 3760),   // «2й тайм: Ф2(+3)»
    SECOND_HALF_HANDICAP_AWAY_MINUS_3_5((short) 3761),// «2й тайм: Ф2(-3,5)»
    SECOND_HALF_HANDICAP_AWAY_PLUS_3_5((short) 3762), // «2й тайм: Ф2(+3,5)»

    //========== 38. Тотал голов 1й тайм (First Half Goals Amount) — коды 3800–3899 ==========
    FIRST_HALF_TOTAL_UNDER_0_5((short) 3800),   // «1й тайм: ТМ 0,5»
    FIRST_HALF_TOTAL_UNDER_1((short) 3801),     // «1й тайм: ТМ 1»
    FIRST_HALF_TOTAL_UNDER_1_5((short) 3802),   // «1й тайм: ТМ 1,5»
    FIRST_HALF_TOTAL_UNDER_2((short) 3803),     // «1й тайм: ТМ 2»
    FIRST_HALF_TOTAL_UNDER_2_5((short) 3804),   // «1й тайм: ТМ 2,5»
    FIRST_HALF_TOTAL_UNDER_3((short) 3805),     // «1й тайм: ТМ 3»
    FIRST_HALF_TOTAL_UNDER_3_5((short) 3806),   // «1й тайм: ТМ 3,5»
    FIRST_HALF_TOTAL_UNDER_4((short) 3807),     // «1й тайм: ТМ 4»
    FIRST_HALF_TOTAL_UNDER_4_5((short) 3808),   // «1й тайм: ТМ 4,5»

    FIRST_HALF_TOTAL_OVER_0_5((short) 3850),    // «1й тайм: ТБ 0,5»
    FIRST_HALF_TOTAL_OVER_1((short) 3851),      // «1й тайм: ТБ 1»
    FIRST_HALF_TOTAL_OVER_1_5((short) 3852),    // «1й тайм: ТБ 1,5»
    FIRST_HALF_TOTAL_OVER_2((short) 3853),      // «1й тайм: ТБ 2»
    FIRST_HALF_TOTAL_OVER_2_5((short) 3854),    // «1й тайм: ТБ 2,5»
    FIRST_HALF_TOTAL_OVER_3((short) 3855),      // «1й тайм: ТБ 3»
    FIRST_HALF_TOTAL_OVER_3_5((short) 3856),    // «1й тайм: ТБ 3,5»
    FIRST_HALF_TOTAL_OVER_4((short) 3857),      // «1й тайм: ТБ 4»
    FIRST_HALF_TOTAL_OVER_4_5((short) 3858),    // «1й тайм: ТБ 4,5»

    //========== 39. Тотал голов 2й тайм (Second Half Goals Amount) — коды 3900–3999 ==========
    SECOND_HALF_TOTAL_UNDER_0_5((short) 3900),   // «2й тайм: ТМ 0,5»
    SECOND_HALF_TOTAL_UNDER_1((short) 3901),     // «2й тайм: ТМ 1»
    SECOND_HALF_TOTAL_UNDER_1_5((short) 3902),   // «2й тайм: ТМ 1,5»
    SECOND_HALF_TOTAL_UNDER_2((short) 3903),     // «2й тайм: ТМ 2»
    SECOND_HALF_TOTAL_UNDER_2_5((short) 3904),   // «2й тайм: ТМ 2,5»
    SECOND_HALF_TOTAL_UNDER_3((short) 3905),     // «2й тайм: ТМ 3»
    SECOND_HALF_TOTAL_UNDER_3_5((short) 3906),   // «2й тайм: ТМ 3,5»
    SECOND_HALF_TOTAL_UNDER_4((short) 3907),     // «2й тайм: ТМ 4»
    SECOND_HALF_TOTAL_UNDER_4_5((short) 3908),   // «2й тайм: ТМ 4,5»

    SECOND_HALF_TOTAL_OVER_0_5((short) 3950),    // «2й тайм: ТБ 0,5»
    SECOND_HALF_TOTAL_OVER_1((short) 3951),      // «2й тайм: ТБ 1»
    SECOND_HALF_TOTAL_OVER_1_5((short) 3952),    // «2й тайм: ТБ 1,5»
    SECOND_HALF_TOTAL_OVER_2((short) 3953),      // «2й тайм: ТБ 2»
    SECOND_HALF_TOTAL_OVER_2_5((short) 3954),    // «2й тайм: ТБ 2,5»
    SECOND_HALF_TOTAL_OVER_3((short) 3955),      // «2й тайм: ТБ 3»
    SECOND_HALF_TOTAL_OVER_3_5((short) 3956),    // «2й тайм: ТБ 3,5»
    SECOND_HALF_TOTAL_OVER_4((short) 3957),      // «2й тайм: ТБ 4»
    SECOND_HALF_TOTAL_OVER_4_5((short) 3958),    // «2й тайм: ТБ 4,5»

    //========== 40. ИТ Хозяев 1й тайм (First Half Home Team Goals Amount) — коды 4000–4099 ==========
    FIRST_HALF_HOME_TEAM_TOTAL_UNDER_0_5((short) 4000),  // «1й тайм: Хозяева ИТМ 0,5»
    FIRST_HALF_HOME_TEAM_TOTAL_UNDER_1((short) 4001),    // «1й тайм: Хозяева ИТМ 1»
    FIRST_HALF_HOME_TEAM_TOTAL_UNDER_1_5((short) 4002),  // «1й тайм: Хозяева ИТМ 1,5»
    FIRST_HALF_HOME_TEAM_TOTAL_UNDER_2((short) 4003),    // «1й тайм: Хозяева ИТМ 2»
    FIRST_HALF_HOME_TEAM_TOTAL_UNDER_2_5((short) 4004),  // «1й тайм: Хозяева ИТМ 2,5»
    FIRST_HALF_HOME_TEAM_TOTAL_UNDER_3((short) 4005),    // «1й тайм: Хозяева ИТМ 3»

    FIRST_HALF_HOME_TEAM_TOTAL_OVER_0_5((short) 4050),   // «1й тайм: Хозяева ИТБ 0,5»
    FIRST_HALF_HOME_TEAM_TOTAL_OVER_1((short) 4051),     // «1й тайм: Хозяева ИТБ 1»
    FIRST_HALF_HOME_TEAM_TOTAL_OVER_1_5((short) 4052),   // «1й тайм: Хозяева ИТБ 1,5»
    FIRST_HALF_HOME_TEAM_TOTAL_OVER_2((short) 4053),     // «1й тайм: Хозяева ИТБ 2»
    FIRST_HALF_HOME_TEAM_TOTAL_OVER_2_5((short) 4054),   // «1й тайм: Хозяева ИТБ 2,5»
    FIRST_HALF_HOME_TEAM_TOTAL_OVER_3((short) 4055),     // «1й тайм: Хозяева ИТБ 3»

    //========== 41. ИТ Хозяев 2й тайм (Second Half Home Team Goals Amount) — коды 4100–4199 ==========
    SECOND_HALF_HOME_TEAM_TOTAL_UNDER_0_5((short) 4100),  // «2й тайм: Хозяева ИТМ 0,5»
    SECOND_HALF_HOME_TEAM_TOTAL_UNDER_1((short) 4101),    // «2й тайм: Хозяева ИТМ 1»
    SECOND_HALF_HOME_TEAM_TOTAL_UNDER_1_5((short) 4102),  // «2й тайм: Хозяева ИТМ 1,5»
    SECOND_HALF_HOME_TEAM_TOTAL_UNDER_2((short) 4103),    // «2й тайм: Хозяева ИТМ 2»
    SECOND_HALF_HOME_TEAM_TOTAL_UNDER_2_5((short) 4104),  // «2й тайм: Хозяева ИТМ 2,5»
    SECOND_HALF_HOME_TEAM_TOTAL_UNDER_3((short) 4105),    // «2й тайм: Хозяева ИТМ 3»

    SECOND_HALF_HOME_TEAM_TOTAL_OVER_0_5((short) 4150),   // «2й тайм: Хозяева ИТБ 0,5»
    SECOND_HALF_HOME_TEAM_TOTAL_OVER_1((short) 4151),     // «2й тайм: Хозяева ИТБ 1»
    SECOND_HALF_HOME_TEAM_TOTAL_OVER_1_5((short) 4152),   // «2й тайм: Хозяева ИТБ 1,5»
    SECOND_HALF_HOME_TEAM_TOTAL_OVER_2((short) 4153),     // «2й тайм: Хозяева ИТБ 2»
    SECOND_HALF_HOME_TEAM_TOTAL_OVER_2_5((short) 4154),   // «2й тайм: Хозяева ИТБ 2,5»
    SECOND_HALF_HOME_TEAM_TOTAL_OVER_3((short) 4155),     // «2й тайм: Хозяева ИТБ 3»

    //========== 42. ИТ Гостей 1й тайм (First Half Away Team Goals Amount) — коды 4200–4299 ==========
    FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_0_5((short) 4200),  // «1й тайм: Гости ИТМ 0,5»
    FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_1((short) 4201),    // «1й тайм: Гости ИТМ 1»
    FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_1_5((short) 4202),  // «1й тайм: Гости ИТМ 1,5»
    FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_2((short) 4203),    // «1й тайм: Гости ИТМ 2»
    FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_2_5((short) 4204),  // «1й тайм: Гости ИТМ 2,5»
    FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_3((short) 4205),    // «1й тайм: Гости ИТМ 3»

    FIRST_HALF_AWAY_TEAM_TOTAL_OVER_0_5((short) 4250),   // «1й тайм: Гости ИТБ 0,5»
    FIRST_HALF_AWAY_TEAM_TOTAL_OVER_1((short) 4251),     // «1й тайм: Гости ИТБ 1»
    FIRST_HALF_AWAY_TEAM_TOTAL_OVER_1_5((short) 4252),   // «1й тайм: Гости ИТБ 1,5»
    FIRST_HALF_AWAY_TEAM_TOTAL_OVER_2((short) 4253),     // «1й тайм: Гости ИТБ 2»
    FIRST_HALF_AWAY_TEAM_TOTAL_OVER_2_5((short) 4254),   // «1й тайм: Гости ИТБ 2,5»
    FIRST_HALF_AWAY_TEAM_TOTAL_OVER_3((short) 4255),     // «1й тайм: Гости ИТБ 3»

    //========== 43. ИТ Гостей 2й тайм (Second Half Away Team Goals Amount) — коды 4300–4399 ==========
    SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_0_5((short) 4300),  // «2й тайм: Гости ИТМ 0,5»
    SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_1((short) 4301),    // «2й тайм: Гости ИТМ 1»
    SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_1_5((short) 4302),  // «2й тайм: Гости ИТМ 1,5»
    SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_2((short) 4303),    // «2й тайм: Гости ИТМ 2»
    SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_2_5((short) 4304),  // «2й тайм: Гости ИТМ 2,5»
    SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_3((short) 4305),    // «2й тайм: Гости ИТМ 3»

    SECOND_HALF_AWAY_TEAM_TOTAL_OVER_0_5((short) 4350),   // «2й тайм: Гости ИТБ 0,5»
    SECOND_HALF_AWAY_TEAM_TOTAL_OVER_1((short) 4351),     // «2й тайм: Гости ИТБ 1»
    SECOND_HALF_AWAY_TEAM_TOTAL_OVER_1_5((short) 4352),   // «2й тайм: Гости ИТБ 1,5»
    SECOND_HALF_AWAY_TEAM_TOTAL_OVER_2((short) 4353),     // «2й тайм: Гости ИТБ 2»
    SECOND_HALF_AWAY_TEAM_TOTAL_OVER_2_5((short) 4354),   // «2й тайм: Гости ИТБ 2,5»
    SECOND_HALF_AWAY_TEAM_TOTAL_OVER_3((short) 4355),     // «2й тайм: Гости ИТБ 3»

    //========== 44. 1й тайм: Исход + Тотал Меньше (First Half Result + Total Under) — коды 4400–4499 ==========
    FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_1_5((short) 4400),   // «1й тайм: П1 + ТМ 1,5»
    FIRST_HALF_DRAW_AND_TOTAL_UNDER_1_5((short) 4401),       // «1й тайм: Х + ТМ 1,5»
    FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_1_5((short) 4402),   // «1й тайм: П2 + ТМ 1,5»
    FIRST_HALF_1X_AND_TOTAL_UNDER_1_5((short) 4403),         // «1й тайм: 1Х + ТМ 1,5»
    FIRST_HALF_12_AND_TOTAL_UNDER_1_5((short) 4404),         // «1й тайм: 12 + ТМ 1,5»
    FIRST_HALF_X2_AND_TOTAL_UNDER_1_5((short) 4405),         // «1й тайм: Х2 + ТМ 1,5»

    FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_2((short) 4410),     // «1й тайм: П1 + ТМ 2»
    FIRST_HALF_DRAW_AND_TOTAL_UNDER_2((short) 4411),         // «1й тайм: Х + ТМ 2»
    FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_2((short) 4412),     // «1й тайм: П2 + ТМ 2»
    FIRST_HALF_1X_AND_TOTAL_UNDER_2((short) 4413),           // «1й тайм: 1Х + ТМ 2»
    FIRST_HALF_12_AND_TOTAL_UNDER_2((short) 4414),           // «1й тайм: 12 + ТМ 2»
    FIRST_HALF_X2_AND_TOTAL_UNDER_2((short) 4415),           // «1й тайм: Х2 + ТМ 2»

    FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_2_5((short) 4420),   // «1й тайм: П1 + ТМ 2,5»
    FIRST_HALF_DRAW_AND_TOTAL_UNDER_2_5((short) 4421),       // «1й тайм: Х + ТМ 2,5»
    FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_2_5((short) 4422),   // «1й тайм: П2 + ТМ 2,5»
    FIRST_HALF_1X_AND_TOTAL_UNDER_2_5((short) 4423),         // «1й тайм: 1Х + ТМ 2,5»
    FIRST_HALF_12_AND_TOTAL_UNDER_2_5((short) 4424),         // «1й тайм: 12 + ТМ 2,5»
    FIRST_HALF_X2_AND_TOTAL_UNDER_2_5((short) 4425),         // «1й тайм: Х2 + ТМ 2,5»

    FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_3((short) 4430),     // «1й тайм: П1 + ТМ 3»
    FIRST_HALF_DRAW_AND_TOTAL_UNDER_3((short) 4431),         // «1й тайм: Х + ТМ 3»
    FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_3((short) 4432),     // «1й тайм: П2 + ТМ 3»
    FIRST_HALF_1X_AND_TOTAL_UNDER_3((short) 4433),           // «1й тайм: 1Х + ТМ 3»
    FIRST_HALF_12_AND_TOTAL_UNDER_3((short) 4434),           // «1й тайм: 12 + ТМ 3»
    FIRST_HALF_X2_AND_TOTAL_UNDER_3((short) 4435),           // «1й тайм: Х2 + ТМ 3»

    FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_3_5((short) 4440),     // «1й тайм: П1 + ТМ 3,5»
    FIRST_HALF_DRAW_AND_TOTAL_UNDER_3_5((short) 4441),         // «1й тайм: Х + ТМ 3,5»
    FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_3_5((short) 4442),     // «1й тайм: П2 + ТМ 3,5»
    FIRST_HALF_1X_AND_TOTAL_UNDER_3_5((short) 4443),           // «1й тайм: 1Х + ТМ 3,5»
    FIRST_HALF_12_AND_TOTAL_UNDER_3_5((short) 4444),           // «1й тайм: 12 + ТМ 3,5»
    FIRST_HALF_X2_AND_TOTAL_UNDER_3_5((short) 4445),           // «1й тайм: Х2 + ТМ 3,5»

    //========== 45. 1й тайм: Исход + Тотал Больше (First Half Result + Total Over) — коды 4500–4599 ==========
    FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_1_5((short) 4500),   // «1й тайм: П1 + ТБ 1,5»
    FIRST_HALF_DRAW_AND_TOTAL_OVER_1_5((short) 4501),       // «1й тайм: Х + ТБ 1,5»
    FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_1_5((short) 4502),   // «1й тайм: П2 + ТБ 1,5»
    FIRST_HALF_1X_AND_TOTAL_OVER_1_5((short) 4503),         // «1й тайм: 1Х + ТБ 1,5»
    FIRST_HALF_12_AND_TOTAL_OVER_1_5((short) 4504),         // «1й тайм: 12 + ТБ 1,5»
    FIRST_HALF_X2_AND_TOTAL_OVER_1_5((short) 4505),         // «1й тайм: Х2 + ТБ 1,5»

    FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_2((short) 4510),     // «1й тайм: П1 + ТБ 2»
    FIRST_HALF_DRAW_AND_TOTAL_OVER_2((short) 4511),         // «1й тайм: Х + ТБ 2»
    FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_2((short) 4512),     // «1й тайм: П2 + ТБ 2»
    FIRST_HALF_1X_AND_TOTAL_OVER_2((short) 4513),           // «1й тайм: 1Х + ТБ 2»
    FIRST_HALF_12_AND_TOTAL_OVER_2((short) 4514),           // «1й тайм: 12 + ТБ 2»
    FIRST_HALF_X2_AND_TOTAL_OVER_2((short) 4515),           // «1й тайм: Х2 + ТБ 2»

    FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_2_5((short) 4520),   // «1й тайм: П1 + ТБ 2,5»
    FIRST_HALF_DRAW_AND_TOTAL_OVER_2_5((short) 4521),       // «1й тайм: Х + ТБ 2,5»
    FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_2_5((short) 4522),   // «1й тайм: П2 + ТБ 2,5»
    FIRST_HALF_1X_AND_TOTAL_OVER_2_5((short) 4523),         // «1й тайм: 1Х + ТБ 2,5»
    FIRST_HALF_12_AND_TOTAL_OVER_2_5((short) 4524),         // «1й тайм: 12 + ТБ 2,5»
    FIRST_HALF_X2_AND_TOTAL_OVER_2_5((short) 4525),         // «1й тайм: Х2 + ТБ 2,5»

    FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_3((short) 4530),     // «1й тайм: П1 + ТБ 3»
    FIRST_HALF_DRAW_AND_TOTAL_OVER_3((short) 4531),         // «1й тайм: Х + ТБ 3»
    FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_3((short) 4532),     // «1й тайм: П2 + ТБ 3»
    FIRST_HALF_1X_AND_TOTAL_OVER_3((short) 4533),           // «1й тайм: 1Х + ТБ 3»
    FIRST_HALF_12_AND_TOTAL_OVER_3((short) 4534),           // «1й тайм: 12 + ТБ 3»
    FIRST_HALF_X2_AND_TOTAL_OVER_3((short) 4535),           // «1й тайм: Х2 + ТБ 3»

    FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_3_5((short) 4540),   // «1й тайм: П1 + ТБ 3,5»
    FIRST_HALF_DRAW_AND_TOTAL_OVER_3_5((short) 4541),       // «1й тайм: Х + ТБ 3,5»
    FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_3_5((short) 4542),   // «1й тайм: П2 + ТБ 3,5»
    FIRST_HALF_1X_AND_TOTAL_OVER_3_5((short) 4543),         // «1й тайм: 1Х + ТБ 3,5»
    FIRST_HALF_12_AND_TOTAL_OVER_3_5((short) 4544),         // «1й тайм: 12 + ТБ 3,5»
    FIRST_HALF_X2_AND_TOTAL_OVER_3_5((short) 4545),         // «1й тайм: Х2 + ТБ 3,5»

    //========== 46. 2й тайм: Исход + Тотал Меньше (Second Half Result + Total Under) — коды 4600–4699 ==========
    SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_1_5((short) 4600),   // «2й тайм: П1 + ТМ 1,5»
    SECOND_HALF_DRAW_AND_TOTAL_UNDER_1_5((short) 4601),       // «2й тайм: Х + ТМ 1,5»
    SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_1_5((short) 4602),   // «2й тайм: П2 + ТМ 1,5»
    SECOND_HALF_1X_AND_TOTAL_UNDER_1_5((short) 4603),         // «2й тайм: 1Х + ТМ 1,5»
    SECOND_HALF_12_AND_TOTAL_UNDER_1_5((short) 4604),         // «2й тайм: 12 + ТМ 1,5»
    SECOND_HALF_X2_AND_TOTAL_UNDER_1_5((short) 4605),         // «2й тайм: Х2 + ТМ 1,5»

    SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_2((short) 4610),     // «2й тайм: П1 + ТМ 2»
    SECOND_HALF_DRAW_AND_TOTAL_UNDER_2((short) 4611),         // «2й тайм: Х + ТМ 2»
    SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_2((short) 4612),     // «2й тайм: П2 + ТМ 2»
    SECOND_HALF_1X_AND_TOTAL_UNDER_2((short) 4613),           // «2й тайм: 1Х + ТМ 2»
    SECOND_HALF_12_AND_TOTAL_UNDER_2((short) 4614),           // «2й тайм: 12 + ТМ 2»
    SECOND_HALF_X2_AND_TOTAL_UNDER_2((short) 4615),           // «2й тайм: Х2 + ТМ 2»

    SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_2_5((short) 4620),   // «2й тайм: П1 + ТМ 2,5»
    SECOND_HALF_DRAW_AND_TOTAL_UNDER_2_5((short) 4621),       // «2й тайм: Х + ТМ 2,5»
    SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_2_5((short) 4622),   // «2й тайм: П2 + ТМ 2,5»
    SECOND_HALF_1X_AND_TOTAL_UNDER_2_5((short) 4623),         // «2й тайм: 1Х + ТМ 2,5»
    SECOND_HALF_12_AND_TOTAL_UNDER_2_5((short) 4624),         // «2й тайм: 12 + ТМ 2,5»
    SECOND_HALF_X2_AND_TOTAL_UNDER_2_5((short) 4625),         // «2й тайм: Х2 + ТМ 2,5»

    SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_3((short) 4630),     // «2й тайм: П1 + ТМ 3»
    SECOND_HALF_DRAW_AND_TOTAL_UNDER_3((short) 4631),         // «2й тайм: Х + ТМ 3»
    SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_3((short) 4632),     // «2й тайм: П2 + ТМ 3»
    SECOND_HALF_1X_AND_TOTAL_UNDER_3((short) 4633),           // «2й тайм: 1Х + ТМ 3»
    SECOND_HALF_12_AND_TOTAL_UNDER_3((short) 4634),           // «2й тайм: 12 + ТМ 3»
    SECOND_HALF_X2_AND_TOTAL_UNDER_3((short) 4635),           // «2й тайм: Х2 + ТМ 3»

    SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_3_5((short) 4640),   // «2й тайм: П1 + ТМ 3,5»
    SECOND_HALF_DRAW_AND_TOTAL_UNDER_3_5((short) 4641),       // «2й тайм: Х + ТМ 3,5»
    SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_3_5((short) 4642),   // «2й тайм: П2 + ТМ 3,5»
    SECOND_HALF_1X_AND_TOTAL_UNDER_3_5((short) 4643),         // «2й тайм: 1Х + ТМ 3,5»
    SECOND_HALF_12_AND_TOTAL_UNDER_3_5((short) 4644),         // «2й тайм: 12 + ТМ 3,5»
    SECOND_HALF_X2_AND_TOTAL_UNDER_3_5((short) 4645),         // «2й тайм: Х2 + ТМ 3,5»

    //========== 47. 2й тайм: Исход + Тотал Больше (Second Half Result + Total Over) — коды 4700–4799 ==========
    SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_1_5((short) 4700),    // «2й тайм: П1 + ТБ 1,5»
    SECOND_HALF_DRAW_AND_TOTAL_OVER_1_5((short) 4701),        // «2й тайм: Х + ТБ 1,5»
    SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_1_5((short) 4702),    // «2й тайм: П2 + ТБ 1,5»
    SECOND_HALF_1X_AND_TOTAL_OVER_1_5((short) 4703),          // «2й тайм: 1Х + ТБ 1,5»
    SECOND_HALF_12_AND_TOTAL_OVER_1_5((short) 4704),          // «2й тайм: 12 + ТБ 1,5»
    SECOND_HALF_X2_AND_TOTAL_OVER_1_5((short) 4705),          // «2й тайм: Х2 + ТБ 1,5»

    SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_2((short) 4710),      // «2й тайм: П1 + ТБ 2»
    SECOND_HALF_DRAW_AND_TOTAL_OVER_2((short) 4711),          // «2й тайм: Х + ТБ 2»
    SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_2((short) 4712),      // «2й тайм: П2 + ТБ 2»
    SECOND_HALF_1X_AND_TOTAL_OVER_2((short) 4713),            // «2й тайм: 1Х + ТБ 2»
    SECOND_HALF_12_AND_TOTAL_OVER_2((short) 4714),            // «2й тайм: 12 + ТБ 2»
    SECOND_HALF_X2_AND_TOTAL_OVER_2((short) 4715),            // «2й тайм: Х2 + ТБ 2»

    SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_2_5((short) 4720),    // «2й тайм: П1 + ТБ 2,5»
    SECOND_HALF_DRAW_AND_TOTAL_OVER_2_5((short) 4721),        // «2й тайм: Х + ТБ 2,5»
    SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_2_5((short) 4722),    // «2й тайм: П2 + ТБ 2,5»
    SECOND_HALF_1X_AND_TOTAL_OVER_2_5((short) 4723),          // «2й тайм: 1Х + ТБ 2,5»
    SECOND_HALF_12_AND_TOTAL_OVER_2_5((short) 4724),          // «2й тайм: 12 + ТБ 2,5»
    SECOND_HALF_X2_AND_TOTAL_OVER_2_5((short) 4725),          // «2й тайм: Х2 + ТБ 2,5»

    SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_3((short) 4730),      // «2й тайм: П1 + ТБ 3»
    SECOND_HALF_DRAW_AND_TOTAL_OVER_3((short) 4731),          // «2й тайм: Х + ТБ 3»
    SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_3((short) 4732),      // «2й тайм: П2 + ТБ 3»
    SECOND_HALF_1X_AND_TOTAL_OVER_3((short) 4733),            // «2й тайм: 1Х + ТБ 3»
    SECOND_HALF_12_AND_TOTAL_OVER_3((short) 4734),            // «2й тайм: 12 + ТБ 3»
    SECOND_HALF_X2_AND_TOTAL_OVER_3((short) 4735),            // «2й тайм: Х2 + ТБ 3»

    SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_3_5((short) 4740),    // «2й тайм: П1 + ТБ 3,5»
    SECOND_HALF_DRAW_AND_TOTAL_OVER_3_5((short) 4741),        // «2й тайм: Х + ТБ 3,5»
    SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_3_5((short) 4742),    // «2й тайм: П2 + ТБ 3,5»
    SECOND_HALF_1X_AND_TOTAL_OVER_3_5((short) 4743),          // «2й тайм: 1Х + ТБ 3,5»
    SECOND_HALF_12_AND_TOTAL_OVER_3_5((short) 4744),          // «2й тайм: 12 + ТБ 3,5»
    SECOND_HALF_X2_AND_TOTAL_OVER_3_5((short) 4745),          // «2й тайм: Х2 + ТБ 3,5»

    //========== 48. Тайм с большим количеством голов (Half With More Goals) — коды 4800–4899 ==========
    TOTAL_GOALS_FIRST_HALF_MORE_THAN_SECOND((short) 4800),    // «Тотал голов: 1й > 2й тайм»
    TOTAL_GOALS_EQUAL_IN_BOTH_HALVES((short) 4801),           // «Тотал голов: Поровну»
    TOTAL_GOALS_FIRST_HALF_LESS_THAN_SECOND((short) 4802),    // «Тотал голов: 1й < 2й тайм»
    HOME_GOALS_FIRST_HALF_MORE_THAN_SECOND((short) 4803),     // «Тотал голов: (Хозяева) 1й > 2й тайм»
    HOME_GOALS_EQUAL_IN_BOTH_HALVES((short) 4804),            // «Тотал голов: (Хозяева) Поровну»
    HOME_GOALS_FIRST_HALF_LESS_THAN_SECOND((short) 4805),     // «Тотал голов: (Хозяева) 1й < 2й тайм»
    AWAY_GOALS_FIRST_HALF_MORE_THAN_SECOND((short) 4806),     // «Тотал голов: (Гости) 1й > 2й тайм»
    AWAY_GOALS_EQUAL_IN_BOTH_HALVES((short) 4807),            // «Тотал голов: (Гости) Поровну»
    AWAY_GOALS_FIRST_HALF_LESS_THAN_SECOND((short) 4808),     // «Тотал голов: (Гости) 1й < 2й тайм»


    // =================================================================================================
    // Ставки на особые события являются отдельной группой,
    // поэтому под них выделен особый диапазон кодов, начиная с 9000
    // =================================================================================================

    //========== 90. Победа всухую (Clean Win) — коды 9000–9099 ==========
    CLEAN_WIN_HOME((short) 9000),           // «Хозяева - победа всухую»
    CLEAN_WIN_AWAY((short) 9001),           // «Гости - победа всухую»
    CLEAN_WIN_ANY((short) 9002),            // «Любая - победа всухую»

    //========== 91. Разница в голах (Goals Difference) — коды 9100–9199 ==========
    GOALS_DIFF_HOME_WIN_1((short) 9100),    // «П1 в 1 гол»
    GOALS_DIFF_AWAY_WIN_1((short) 9101),    // «П2 в 1 гол»
    GOALS_DIFF_HOME_WIN_2((short) 9102),    // «П1 в 2 гола»
    GOALS_DIFF_AWAY_WIN_2((short) 9103),    // «П2 в 2 гола»
    GOALS_DIFF_HOME_WIN_3((short) 9104),    // «П1 в 3 гола»
    GOALS_DIFF_AWAY_WIN_3((short) 9105),    // «П2 в 3 гола»
    GOALS_DIFF_HOME_OR_AWAY_WIN_1((short) 9110), // «П1 или П2 в 1 гол»
    GOALS_DIFF_HOME_OR_AWAY_WIN_2((short) 9111), // «П1 или П2 в 2 гола»
    GOALS_DIFF_HOME_OR_AWAY_WIN_3((short) 9112), // «П1 или П2 в 3 гола»

    //========== 92. Playoff Outcomes (Особые исходы playoff) — коды 9200–9299 ==========
    PLAYOFF_EXTRA_TIME((short) 9200),               // «Дополнительное время»
    PLAYOFF_PENALTIES((short) 9201),                // «Послематчевые пенальти»
    PLAYOFF_HOME_WIN_REGULAR((short) 9202),         // «П1 в осн.время»
    PLAYOFF_AWAY_WIN_REGULAR((short) 9203),         // «П2 в осн.время»
    PLAYOFF_HOME_OR_AWAY_REGULAR((short) 9204),     // «12 в осн.время»
    PLAYOFF_HOME_WIN_EXTRA((short) 9205),           // «П1 в доп.время»
    PLAYOFF_AWAY_WIN_EXTRA((short) 9206),           // «П2 в доп.время»
    PLAYOFF_HOME_OR_AWAY_EXTRA((short) 9207),       // «12 в доп.время»
    PLAYOFF_HOME_WIN_PENALTIES((short) 9208),       // «П1 по пенальти»
    PLAYOFF_AWAY_WIN_PENALTIES((short) 9209),       // «П2 по пенальти»
    PLAYOFF_HOME_OR_AWAY_PENALTIES((short) 9210),   // «12 по пенальти»
    PLAYOFF_HOME_ADVANCE_NEXT_STAGE((short) 9211),  // «Хозяева - выход в след.стадию»
    PLAYOFF_AWAY_ADVANCE_NEXT_STAGE((short) 9212),  // «Гости - выход в след.стадию»
    PLAYOFF_HOME_ADVANCE_FINAL((short) 9213),       // «Хозяева - выход в финал»
    PLAYOFF_AWAY_ADVANCE_FINAL((short) 9214),       // «Гости - выход в финал»
    PLAYOFF_HOME_WIN_TOURNAMENT((short) 9215),      // «Хозяева - победитель турнира»
    PLAYOFF_AWAY_WIN_TOURNAMENT((short) 9216, new PlayoffChecker());      // «Гости - победитель турнира»

    private final short code;
    private final BetChecker checker;

    /**
     * Найти enum по его числовому коду.
     *
     * @param code short-код ставки
     * @return соответствующий BetTitleCode или null, если не найден
     */
    public static BetTitleCode fromCode(short code) {
        for (BetTitleCode bt : values()) {
            if (bt.code == code) {
                return bt;
            }
        }
        return null;
    }

    public Bet.BetStatus evaluate(GameResult result) {
        if (checker == null) {
            throw new IllegalStateException("No BetChecker defined for code " + code);
        }
        return checker.check(result, code);
    }

    private static final Map<Short, BetTitleCode> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(BetTitleCode::getCode, Function.identity()));

    public static Bet.BetStatus evaluateByCode(short code, GameResult result) {
        BetTitleCode bet = CODE_MAP.get(code);
        if (bet == null) {
            throw new IllegalArgumentException("Unknown bet code: " + code);
        }
        return bet.evaluate(result);
    }

}
