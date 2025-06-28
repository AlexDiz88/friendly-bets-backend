package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.validation.betcheckers.*;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Getter
@AllArgsConstructor
public enum BetTitleCode {

    //========== 101–200. Обычный исход (gameResult) ==========
    RESULT_HOME_WIN((short) 101, new GameResultChecker()),          // «П1»
    RESULT_DRAW((short) 102, new GameResultChecker()),              // «Х»
    RESULT_AWAY_WIN((short) 103, new GameResultChecker()),          // «П2»
    DOUBLE_CHANCE_HOME_OR_DRAW((short) 104, new GameResultChecker()),   // «1Х»
    DOUBLE_CHANCE_HOME_OR_AWAY((short) 105, new GameResultChecker()),   // «12»
    DOUBLE_CHANCE_DRAW_OR_AWAY((short) 106, new GameResultChecker()),   // «Х2»

    //========== 201–250. П1 + Тотал меньше (Home win + Goals UNDER) ==========
    HOME_WIN_AND_UNDER_1_0((short) 201, new GameResultWithTotalChecker()),   // «П1 + ТМ 1»
    HOME_WIN_AND_UNDER_1_5((short) 202, new GameResultWithTotalChecker()),   // «П1 + ТМ 1,5»
    HOME_WIN_AND_UNDER_2_0((short) 203, new GameResultWithTotalChecker()),   // «П1 + ТМ 2»
    HOME_WIN_AND_UNDER_2_5((short) 204, new GameResultWithTotalChecker()),   // «П1 + ТМ 2,5»
    HOME_WIN_AND_UNDER_3_0((short) 205, new GameResultWithTotalChecker()),   // «П1 + ТМ 3»
    HOME_WIN_AND_UNDER_3_5((short) 206, new GameResultWithTotalChecker()),   // «П1 + ТМ 3,5»
    HOME_WIN_AND_UNDER_4_0((short) 207, new GameResultWithTotalChecker()),   // «П1 + ТМ 4»
    HOME_WIN_AND_UNDER_4_5((short) 208, new GameResultWithTotalChecker()),   // «П1 + ТМ 4,5»
    HOME_WIN_AND_UNDER_5_0((short) 209, new GameResultWithTotalChecker()),   // «П1 + ТМ 5»
    HOME_WIN_AND_UNDER_5_5((short) 210, new GameResultWithTotalChecker()),   // «П1 + ТМ 5,5»
    HOME_WIN_AND_UNDER_6_0((short) 211, new GameResultWithTotalChecker()),   // «П1 + ТМ 6»

    //========== 251–300. П1 + Тотал больше (Home win + Goals OVER) ==========
    HOME_WIN_AND_OVER_1_0((short) 251, new GameResultWithTotalChecker()),    // «П1 + ТБ 1»
    HOME_WIN_AND_OVER_1_5((short) 252, new GameResultWithTotalChecker()),    // «П1 + ТБ 1,5»
    HOME_WIN_AND_OVER_2_0((short) 253, new GameResultWithTotalChecker()),    // «П1 + ТБ 2»
    HOME_WIN_AND_OVER_2_5((short) 254, new GameResultWithTotalChecker()),    // «П1 + ТБ 2,5»
    HOME_WIN_AND_OVER_3_0((short) 255, new GameResultWithTotalChecker()),    // «П1 + ТБ 3»
    HOME_WIN_AND_OVER_3_5((short) 256, new GameResultWithTotalChecker()),    // «П1 + ТБ 3,5»
    HOME_WIN_AND_OVER_4_0((short) 257, new GameResultWithTotalChecker()),    // «П1 + ТБ 4»
    HOME_WIN_AND_OVER_4_5((short) 258, new GameResultWithTotalChecker()),    // «П1 + ТБ 4,5»
    HOME_WIN_AND_OVER_5_0((short) 259, new GameResultWithTotalChecker()),    // «П1 + ТБ 5»
    HOME_WIN_AND_OVER_5_5((short) 260, new GameResultWithTotalChecker()),    // «П1 + ТБ 5,5»
    HOME_WIN_AND_OVER_6_0((short) 261, new GameResultWithTotalChecker()),    // «П1 + ТБ 6»

    //========== 301–350. 1Х + Тотал меньше (Home win or Draw (1X) + Goals UNDER) ==========
    HOME_OR_DRAW_AND_UNDER_1_0((short) 301, new GameResultWithTotalChecker()), // «1Х + ТМ 1»
    HOME_OR_DRAW_AND_UNDER_1_5((short) 302, new GameResultWithTotalChecker()), // «1Х + ТМ 1,5»
    HOME_OR_DRAW_AND_UNDER_2_0((short) 303, new GameResultWithTotalChecker()), // «1Х + ТМ 2»
    HOME_OR_DRAW_AND_UNDER_2_5((short) 304, new GameResultWithTotalChecker()), // «1Х + ТМ 2,5»
    HOME_OR_DRAW_AND_UNDER_3_0((short) 305, new GameResultWithTotalChecker()), // «1Х + ТМ 3»
    HOME_OR_DRAW_AND_UNDER_3_5((short) 306, new GameResultWithTotalChecker()), // «1Х + ТМ 3,5»
    HOME_OR_DRAW_AND_UNDER_4_0((short) 307, new GameResultWithTotalChecker()), // «1Х + ТМ 4»
    HOME_OR_DRAW_AND_UNDER_4_5((short) 308, new GameResultWithTotalChecker()), // «1Х + ТМ 4,5»
    HOME_OR_DRAW_AND_UNDER_5_0((short) 309, new GameResultWithTotalChecker()), // «1Х + ТМ 5»
    HOME_OR_DRAW_AND_UNDER_5_5((short) 310, new GameResultWithTotalChecker()), // «1Х + ТМ 5,5»
    HOME_OR_DRAW_AND_UNDER_6_0((short) 311, new GameResultWithTotalChecker()), // «1Х + ТМ 6»

    //========== 351–400. 1Х + Тотал больше (Home win or Draw (1X) + Goals OVER) ==========
    HOME_OR_DRAW_AND_OVER_1_0((short) 351, new GameResultWithTotalChecker()),  // «1Х + ТБ 1»
    HOME_OR_DRAW_AND_OVER_1_5((short) 352, new GameResultWithTotalChecker()),  // «1Х + ТБ 1,5»
    HOME_OR_DRAW_AND_OVER_2_0((short) 353, new GameResultWithTotalChecker()),  // «1Х + ТБ 2»
    HOME_OR_DRAW_AND_OVER_2_5((short) 354, new GameResultWithTotalChecker()),  // «1Х + ТБ 2,5»
    HOME_OR_DRAW_AND_OVER_3_0((short) 355, new GameResultWithTotalChecker()),  // «1Х + ТБ 3»
    HOME_OR_DRAW_AND_OVER_3_5((short) 356, new GameResultWithTotalChecker()),  // «1Х + ТБ 3,5»
    HOME_OR_DRAW_AND_OVER_4_0((short) 357, new GameResultWithTotalChecker()),  // «1Х + ТБ 4»
    HOME_OR_DRAW_AND_OVER_4_5((short) 358, new GameResultWithTotalChecker()),  // «1Х + ТБ 4,5»
    HOME_OR_DRAW_AND_OVER_5_0((short) 359, new GameResultWithTotalChecker()),  // «1Х + ТБ 5»
    HOME_OR_DRAW_AND_OVER_5_5((short) 360, new GameResultWithTotalChecker()),  // «1Х + ТБ 5,5»
    HOME_OR_DRAW_AND_OVER_6_0((short) 361, new GameResultWithTotalChecker()),  // «1Х + ТБ 6»

    //========== 401–450. Х + Тотал меньше (Draw (X) + Goals UNDER) ==========
    DRAW_AND_UNDER_1_0((short) 401, new GameResultWithTotalChecker()),  // «Х + ТМ 1»
    DRAW_AND_UNDER_1_5((short) 402, new GameResultWithTotalChecker()),  // «Х + ТМ 1,5»
    DRAW_AND_UNDER_2_0((short) 403, new GameResultWithTotalChecker()),  // «Х + ТМ 2»
    DRAW_AND_UNDER_2_5((short) 404, new GameResultWithTotalChecker()),  // «Х + ТМ 2,5»
    DRAW_AND_UNDER_3_0((short) 405, new GameResultWithTotalChecker()),  // «Х + ТМ 3»
    DRAW_AND_UNDER_3_5((short) 406, new GameResultWithTotalChecker()),  // «Х + ТМ 3,5»
    DRAW_AND_UNDER_4_0((short) 407, new GameResultWithTotalChecker()),  // «Х + ТМ 4»
    DRAW_AND_UNDER_4_5((short) 408, new GameResultWithTotalChecker()),  // «Х + ТМ 4,5»
    DRAW_AND_UNDER_5_0((short) 409, new GameResultWithTotalChecker()),  // «Х + ТМ 5»
    DRAW_AND_UNDER_5_5((short) 410, new GameResultWithTotalChecker()),  // «Х + ТМ 5,5»
    DRAW_AND_UNDER_6_0((short) 411, new GameResultWithTotalChecker()),  // «Х + ТМ 6»

    //========== 451–500. Х + Тотал больше (Draw (X) + Goals OVER) ==========
    DRAW_AND_OVER_1_0((short) 451, new GameResultWithTotalChecker()),  // «Х + ТБ 1»
    DRAW_AND_OVER_1_5((short) 452, new GameResultWithTotalChecker()),  // «Х + ТБ 1,5»
    DRAW_AND_OVER_2_0((short) 453, new GameResultWithTotalChecker()),  // «Х + ТБ 2»
    DRAW_AND_OVER_2_5((short) 454, new GameResultWithTotalChecker()),  // «Х + ТБ 2,5»
    DRAW_AND_OVER_3_0((short) 455, new GameResultWithTotalChecker()),  // «Х + ТБ 3»
    DRAW_AND_OVER_3_5((short) 456, new GameResultWithTotalChecker()),  // «Х + ТБ 3,5»
    DRAW_AND_OVER_4_0((short) 457, new GameResultWithTotalChecker()),  // «Х + ТБ 4»
    DRAW_AND_OVER_4_5((short) 458, new GameResultWithTotalChecker()),  // «Х + ТБ 4,5»
    DRAW_AND_OVER_5_0((short) 459, new GameResultWithTotalChecker()),  // «Х + ТБ 5»
    DRAW_AND_OVER_5_5((short) 460, new GameResultWithTotalChecker()),  // «Х + ТБ 5,5»
    DRAW_AND_OVER_6_0((short) 461, new GameResultWithTotalChecker()),  // «Х + ТБ 6»

    //========== 501–550. П2 + Тотал меньше (Away win + Goals UNDER) ==========
    AWAY_WIN_AND_UNDER_1_0((short) 501, new GameResultWithTotalChecker()),   // «П2 + ТМ 1»
    AWAY_WIN_AND_UNDER_1_5((short) 502, new GameResultWithTotalChecker()),   // «П2 + ТМ 1,5»
    AWAY_WIN_AND_UNDER_2_0((short) 503, new GameResultWithTotalChecker()),   // «П2 + ТМ 2»
    AWAY_WIN_AND_UNDER_2_5((short) 504, new GameResultWithTotalChecker()),   // «П2 + ТМ 2,5»
    AWAY_WIN_AND_UNDER_3_0((short) 505, new GameResultWithTotalChecker()),   // «П2 + ТМ 3»
    AWAY_WIN_AND_UNDER_3_5((short) 506, new GameResultWithTotalChecker()),   // «П2 + ТМ 3,5»
    AWAY_WIN_AND_UNDER_4_0((short) 507, new GameResultWithTotalChecker()),   // «П2 + ТМ 4»
    AWAY_WIN_AND_UNDER_4_5((short) 508, new GameResultWithTotalChecker()),   // «П2 + ТМ 4,5»
    AWAY_WIN_AND_UNDER_5_0((short) 509, new GameResultWithTotalChecker()),   // «П2 + ТМ 5»
    AWAY_WIN_AND_UNDER_5_5((short) 510, new GameResultWithTotalChecker()),   // «П2 + ТМ 5,5»
    AWAY_WIN_AND_UNDER_6_0((short) 511, new GameResultWithTotalChecker()),   // «П2 + ТМ 6»

    //========== 551-600. П2 + Тотал больше (Away win + Goals OVER) ==========
    AWAY_WIN_AND_OVER_1_0((short) 551, new GameResultWithTotalChecker()),    // «П2 + ТБ 1»
    AWAY_WIN_AND_OVER_1_5((short) 552, new GameResultWithTotalChecker()),    // «П2 + ТБ 1,5»
    AWAY_WIN_AND_OVER_2_0((short) 553, new GameResultWithTotalChecker()),    // «П2 + ТБ 2»
    AWAY_WIN_AND_OVER_2_5((short) 554, new GameResultWithTotalChecker()),    // «П2 + ТБ 2,5»
    AWAY_WIN_AND_OVER_3_0((short) 555, new GameResultWithTotalChecker()),    // «П2 + ТБ 3»
    AWAY_WIN_AND_OVER_3_5((short) 556, new GameResultWithTotalChecker()),    // «П2 + ТБ 3,5»
    AWAY_WIN_AND_OVER_4_0((short) 557, new GameResultWithTotalChecker()),    // «П2 + ТБ 4»
    AWAY_WIN_AND_OVER_4_5((short) 558, new GameResultWithTotalChecker()),    // «П2 + ТБ 4,5»
    AWAY_WIN_AND_OVER_5_0((short) 559, new GameResultWithTotalChecker()),    // «П2 + ТБ 5»
    AWAY_WIN_AND_OVER_5_5((short) 560, new GameResultWithTotalChecker()),    // «П2 + ТБ 5,5»
    AWAY_WIN_AND_OVER_6_0((short) 561, new GameResultWithTotalChecker()),    // «П2 + ТБ 6»

    //========== 601-650. Х2 + Тотал меньше (Draw or Away win + Goals UNDER) ==========
    DRAW_OR_AWAY_AND_UNDER_1_0((short) 601, new GameResultWithTotalChecker()), // «Х2 + ТМ 1»
    DRAW_OR_AWAY_AND_UNDER_1_5((short) 602, new GameResultWithTotalChecker()), // «Х2 + ТМ 1,5»
    DRAW_OR_AWAY_AND_UNDER_2_0((short) 603, new GameResultWithTotalChecker()), // «Х2 + ТМ 2»
    DRAW_OR_AWAY_AND_UNDER_2_5((short) 604, new GameResultWithTotalChecker()), // «Х2 + ТМ 2,5»
    DRAW_OR_AWAY_AND_UNDER_3_0((short) 605, new GameResultWithTotalChecker()), // «Х2 + ТМ 3»
    DRAW_OR_AWAY_AND_UNDER_3_5((short) 606, new GameResultWithTotalChecker()), // «Х2 + ТМ 3,5»
    DRAW_OR_AWAY_AND_UNDER_4_0((short) 607, new GameResultWithTotalChecker()), // «Х2 + ТМ 4»
    DRAW_OR_AWAY_AND_UNDER_4_5((short) 608, new GameResultWithTotalChecker()), // «Х2 + ТМ 4,5»
    DRAW_OR_AWAY_AND_UNDER_5_0((short) 609, new GameResultWithTotalChecker()), // «Х2 + ТМ 5»
    DRAW_OR_AWAY_AND_UNDER_5_5((short) 610, new GameResultWithTotalChecker()), // «Х2 + ТМ 5,5»
    DRAW_OR_AWAY_AND_UNDER_6_0((short) 611, new GameResultWithTotalChecker()), // «Х2 + ТМ 6»

    //========== 651-700. Х2 + Тотал больше (Draw or Away win + Goals OVER) ==========
    DRAW_OR_AWAY_AND_OVER_1_0((short) 651, new GameResultWithTotalChecker()),  // «Х2 + ТБ 1»
    DRAW_OR_AWAY_AND_OVER_1_5((short) 652, new GameResultWithTotalChecker()),  // «Х2 + ТБ 1,5»
    DRAW_OR_AWAY_AND_OVER_2_0((short) 653, new GameResultWithTotalChecker()),  // «Х2 + ТБ 2»
    DRAW_OR_AWAY_AND_OVER_2_5((short) 654, new GameResultWithTotalChecker()),  // «Х2 + ТБ 2,5»
    DRAW_OR_AWAY_AND_OVER_3_0((short) 655, new GameResultWithTotalChecker()),  // «Х2 + ТБ 3»
    DRAW_OR_AWAY_AND_OVER_3_5((short) 656, new GameResultWithTotalChecker()),  // «Х2 + ТБ 3,5»
    DRAW_OR_AWAY_AND_OVER_4_0((short) 657, new GameResultWithTotalChecker()),  // «Х2 + ТБ 4»
    DRAW_OR_AWAY_AND_OVER_4_5((short) 658, new GameResultWithTotalChecker()),  // «Х2 + ТБ 4,5»
    DRAW_OR_AWAY_AND_OVER_5_0((short) 659, new GameResultWithTotalChecker()),  // «Х2 + ТБ 5»
    DRAW_OR_AWAY_AND_OVER_5_5((short) 660, new GameResultWithTotalChecker()),  // «Х2 + ТБ 5,5»
    DRAW_OR_AWAY_AND_OVER_6_0((short) 661, new GameResultWithTotalChecker()),  // «Х2 + ТБ 6»

    //========== 701-750. 12 + Тотал меньше (Home or Away win + Goals UNDER) ==========
    HOME_OR_AWAY_AND_UNDER_1_0((short) 701, new GameResultWithTotalChecker()), // «12 + ТМ 1»
    HOME_OR_AWAY_AND_UNDER_1_5((short) 702, new GameResultWithTotalChecker()), // «12 + ТМ 1,5»
    HOME_OR_AWAY_AND_UNDER_2_0((short) 703, new GameResultWithTotalChecker()), // «12 + ТМ 2»
    HOME_OR_AWAY_AND_UNDER_2_5((short) 704, new GameResultWithTotalChecker()), // «12 + ТМ 2,5»
    HOME_OR_AWAY_AND_UNDER_3_0((short) 705, new GameResultWithTotalChecker()), // «12 + ТМ 3»
    HOME_OR_AWAY_AND_UNDER_3_5((short) 706, new GameResultWithTotalChecker()), // «12 + ТМ 3,5»
    HOME_OR_AWAY_AND_UNDER_4_0((short) 707, new GameResultWithTotalChecker()), // «12 + ТМ 4»
    HOME_OR_AWAY_AND_UNDER_4_5((short) 708, new GameResultWithTotalChecker()), // «12 + ТМ 4,5»
    HOME_OR_AWAY_AND_UNDER_5_0((short) 709, new GameResultWithTotalChecker()), // «12 + ТМ 5»
    HOME_OR_AWAY_AND_UNDER_5_5((short) 710, new GameResultWithTotalChecker()), // «12 + ТМ 5,5»
    HOME_OR_AWAY_AND_UNDER_6_0((short) 711, new GameResultWithTotalChecker()), // «12 + ТМ 6»

    //========== 751-800. 12 + Тотал больше (Home or Away win + Goals OVER) ==========
    HOME_OR_AWAY_AND_OVER_1_0((short) 751, new GameResultWithTotalChecker()),  // «12 + ТБ 1»
    HOME_OR_AWAY_AND_OVER_1_5((short) 752, new GameResultWithTotalChecker()),  // «12 + ТБ 1,5»
    HOME_OR_AWAY_AND_OVER_2_0((short) 753, new GameResultWithTotalChecker()),  // «12 + ТБ 2»
    HOME_OR_AWAY_AND_OVER_2_5((short) 754, new GameResultWithTotalChecker()),  // «12 + ТБ 2,5»
    HOME_OR_AWAY_AND_OVER_3_0((short) 755, new GameResultWithTotalChecker()),  // «12 + ТБ 3»
    HOME_OR_AWAY_AND_OVER_3_5((short) 756, new GameResultWithTotalChecker()),  // «12 + ТБ 3,5»
    HOME_OR_AWAY_AND_OVER_4_0((short) 757, new GameResultWithTotalChecker()),  // «12 + ТБ 4»
    HOME_OR_AWAY_AND_OVER_4_5((short) 758, new GameResultWithTotalChecker()),  // «12 + ТБ 4,5»
    HOME_OR_AWAY_AND_OVER_5_0((short) 759, new GameResultWithTotalChecker()),  // «12 + ТБ 5»
    HOME_OR_AWAY_AND_OVER_5_5((short) 760, new GameResultWithTotalChecker()),  // «12 + ТБ 5,5»
    HOME_OR_AWAY_AND_OVER_6_0((short) 761, new GameResultWithTotalChecker()),  // «12 + ТБ 6»

    //========== 801-850. Тотал голов Меньше (Total goals UNDER) ==========
    TOTAL_UNDER_1_0((short) 801, new TotalChecker()),        // «ТМ 1»
    TOTAL_UNDER_1_5((short) 802, new TotalChecker()),        // «ТМ 1,5»
    TOTAL_UNDER_2_0((short) 803, new TotalChecker()),        // «ТМ 2»
    TOTAL_UNDER_2_5((short) 804, new TotalChecker()),        // «ТМ 2,5»
    TOTAL_UNDER_3_0((short) 805, new TotalChecker()),        // «ТМ 3»
    TOTAL_UNDER_3_5((short) 806, new TotalChecker()),        // «ТМ 3,5»
    TOTAL_UNDER_4_0((short) 807, new TotalChecker()),        // «ТМ 4»
    TOTAL_UNDER_4_5((short) 808, new TotalChecker()),        // «ТМ 4,5»
    TOTAL_UNDER_5_0((short) 809, new TotalChecker()),        // «ТМ 5»
    TOTAL_UNDER_5_5((short) 810, new TotalChecker()),        // «ТМ 5,5»
    TOTAL_UNDER_6_0((short) 811, new TotalChecker()),        // «ТМ 6»
    TOTAL_UNDER_6_5((short) 812, new TotalChecker()),        // «ТМ 6,5»

    //========== 851-900. Тотал голов Больше (Total goals OVER) ==========
    TOTAL_OVER_1_0((short) 851, new TotalChecker()),         // «ТБ 1»
    TOTAL_OVER_1_5((short) 852, new TotalChecker()),         // «ТБ 1,5»
    TOTAL_OVER_2_0((short) 853, new TotalChecker()),         // «ТБ 2»
    TOTAL_OVER_2_5((short) 854, new TotalChecker()),         // «ТБ 2,5»
    TOTAL_OVER_3_0((short) 855, new TotalChecker()),         // «ТБ 3»
    TOTAL_OVER_3_5((short) 856, new TotalChecker()),         // «ТБ 3,5»
    TOTAL_OVER_4_0((short) 857, new TotalChecker()),         // «ТБ 4»
    TOTAL_OVER_4_5((short) 858, new TotalChecker()),         // «ТБ 4,5»
    TOTAL_OVER_5_0((short) 859, new TotalChecker()),         // «ТБ 5»
    TOTAL_OVER_5_5((short) 860, new TotalChecker()),         // «ТБ 5,5»
    TOTAL_OVER_6_0((short) 861, new TotalChecker()),         // «ТБ 6»
    TOTAL_OVER_6_5((short) 862, new TotalChecker()),         // «ТБ 6,5»

    //========== 901-950. Хозяева ИТМ (Home team UNDER) ==========
    HOME_TEAM_UNDER_1_0((short) 901, new TotalChecker()),    // «Хозяева ИТМ 1»
    HOME_TEAM_UNDER_1_5((short) 902, new TotalChecker()),    // «Хозяева ИТМ 1,5»
    HOME_TEAM_UNDER_2_0((short) 903, new TotalChecker()),    // «Хозяева ИТМ 2»
    HOME_TEAM_UNDER_2_5((short) 904, new TotalChecker()),    // «Хозяева ИТМ 2,5»
    HOME_TEAM_UNDER_3_0((short) 905, new TotalChecker()),    // «Хозяева ИТМ 3»
    HOME_TEAM_UNDER_3_5((short) 906, new TotalChecker()),    // «Хозяева ИТМ 3,5»
    HOME_TEAM_UNDER_4_0((short) 907, new TotalChecker()),    // «Хозяева ИТМ 4»
    HOME_TEAM_UNDER_4_5((short) 908, new TotalChecker()),    // «Хозяева ИТМ 4,5»
    HOME_TEAM_UNDER_5_0((short) 909, new TotalChecker()),    // «Хозяева ИТМ 5»
    HOME_TEAM_UNDER_5_5((short) 910, new TotalChecker()),    // «Хозяева ИТМ 5,5»
    HOME_TEAM_UNDER_6_0((short) 911, new TotalChecker()),    // «Хозяева ИТМ 6»
    HOME_TEAM_UNDER_6_5((short) 912, new TotalChecker()),    // «Хозяева ИТМ 6,5»

    //========== 951-1000. Хозяева ИТБ (Home team OVER) ==========
    HOME_TEAM_OVER_1_0((short) 951, new TotalChecker()),     // «Хозяева ИТБ 1»
    HOME_TEAM_OVER_1_5((short) 952, new TotalChecker()),     // «Хозяева ИТБ 1,5»
    HOME_TEAM_OVER_2_0((short) 953, new TotalChecker()),     // «Хозяева ИТБ 2»
    HOME_TEAM_OVER_2_5((short) 954, new TotalChecker()),     // «Хозяева ИТБ 2,5»
    HOME_TEAM_OVER_3_0((short) 955, new TotalChecker()),     // «Хозяева ИТБ 3»
    HOME_TEAM_OVER_3_5((short) 956, new TotalChecker()),     // «Хозяева ИТБ 3,5»
    HOME_TEAM_OVER_4_0((short) 957, new TotalChecker()),     // «Хозяева ИТБ 4»
    HOME_TEAM_OVER_4_5((short) 958, new TotalChecker()),     // «Хозяева ИТБ 4,5»
    HOME_TEAM_OVER_5_0((short) 959, new TotalChecker()),     // «Хозяева ИТБ 5»
    HOME_TEAM_OVER_5_5((short) 960, new TotalChecker()),     // «Хозяева ИТБ 5,5»
    HOME_TEAM_OVER_6_0((short) 961, new TotalChecker()),     // «Хозяева ИТБ 6»
    HOME_TEAM_OVER_6_5((short) 962, new TotalChecker()),     // «Хозяева ИТБ 6,5»

    //========== 1001-1050. Гости ИТМ (Away team UNDER) ==========
    AWAY_TEAM_UNDER_1_0((short) 1001, new TotalChecker()),    // «Гости ИТМ 1»
    AWAY_TEAM_UNDER_1_5((short) 1002, new TotalChecker()),    // «Гости ИТМ 1,5»
    AWAY_TEAM_UNDER_2_0((short) 1003, new TotalChecker()),    // «Гости ИТМ 2»
    AWAY_TEAM_UNDER_2_5((short) 1004, new TotalChecker()),    // «Гости ИТМ 2,5»
    AWAY_TEAM_UNDER_3_0((short) 1005, new TotalChecker()),    // «Гости ИТМ 3»
    AWAY_TEAM_UNDER_3_5((short) 1006, new TotalChecker()),    // «Гости ИТМ 3,5»
    AWAY_TEAM_UNDER_4_0((short) 1007, new TotalChecker()),    // «Гости ИТМ 4»
    AWAY_TEAM_UNDER_4_5((short) 1008, new TotalChecker()),    // «Гости ИТМ 4,5»
    AWAY_TEAM_UNDER_5_0((short) 1009, new TotalChecker()),    // «Гости ИТМ 5»
    AWAY_TEAM_UNDER_5_5((short) 1010, new TotalChecker()),    // «Гости ИТМ 5,5»
    AWAY_TEAM_UNDER_6_0((short) 1011, new TotalChecker()),    // «Гости ИТМ 6»
    AWAY_TEAM_UNDER_6_5((short) 1012, new TotalChecker()),    // «Гости ИТМ 6,5»

    //========== 1051-1100. Гости ИТБ (Away team OVER) ==========
    AWAY_TEAM_OVER_1_0((short) 1051, new TotalChecker()),     // «Гости ИТБ 1»
    AWAY_TEAM_OVER_1_5((short) 1052, new TotalChecker()),     // «Гости ИТБ 1,5»
    AWAY_TEAM_OVER_2_0((short) 1053, new TotalChecker()),     // «Гости ИТБ 2»
    AWAY_TEAM_OVER_2_5((short) 1054, new TotalChecker()),     // «Гости ИТБ 2,5»
    AWAY_TEAM_OVER_3_0((short) 1055, new TotalChecker()),     // «Гости ИТБ 3»
    AWAY_TEAM_OVER_3_5((short) 1056, new TotalChecker()),     // «Гости ИТБ 3,5»
    AWAY_TEAM_OVER_4_0((short) 1057, new TotalChecker()),     // «Гости ИТБ 4»
    AWAY_TEAM_OVER_4_5((short) 1058, new TotalChecker()),     // «Гости ИТБ 4,5»
    AWAY_TEAM_OVER_5_0((short) 1059, new TotalChecker()),     // «Гости ИТБ 5»
    AWAY_TEAM_OVER_5_5((short) 1060, new TotalChecker()),     // «Гости ИТБ 5,5»
    AWAY_TEAM_OVER_6_0((short) 1061, new TotalChecker()),     // «Гости ИТБ 6»
    AWAY_TEAM_OVER_6_5((short) 1062, new TotalChecker()),     // «Гости ИТБ 6,5»

    //========== 1101-1150. Фора хозяев (Home team handicap) ==========
    HANDICAP_HOME_0((short) 1101, new HandicapChecker()),        // «Ф1(0)»
    HANDICAP_HOME_MINUS_1_0((short) 1102, new HandicapChecker()),  // «Ф1(-1)»
    HANDICAP_HOME_PLUS_1_0((short) 1103, new HandicapChecker()),   // «Ф1(+1)»
    HANDICAP_HOME_MINUS_1_5((short) 1104, new HandicapChecker()),// «Ф1(-1,5)»
    HANDICAP_HOME_PLUS_1_5((short) 1105, new HandicapChecker()), // «Ф1(+1,5)»
    HANDICAP_HOME_MINUS_2_0((short) 1106, new HandicapChecker()),  // «Ф1(-2)»
    HANDICAP_HOME_PLUS_2_0((short) 1107, new HandicapChecker()),   // «Ф1(+2)»
    HANDICAP_HOME_MINUS_2_5((short) 1108, new HandicapChecker()),// «Ф1(-2,5)»
    HANDICAP_HOME_PLUS_2_5((short) 1109, new HandicapChecker()), // «Ф1(+2,5)»
    HANDICAP_HOME_MINUS_3_0((short) 1110, new HandicapChecker()),  // «Ф1(-3)»
    HANDICAP_HOME_PLUS_3_0((short) 1111, new HandicapChecker()),   // «Ф1(+3)»
    HANDICAP_HOME_MINUS_3_5((short) 1112, new HandicapChecker()),// «Ф1(-3,5)»
    HANDICAP_HOME_PLUS_3_5((short) 1113, new HandicapChecker()), // «Ф1(+3,5)»
    HANDICAP_HOME_MINUS_4_0((short) 1114, new HandicapChecker()),  // «Ф1(-4)»
    HANDICAP_HOME_PLUS_4_0((short) 1115, new HandicapChecker()),   // «Ф1(+4)»
    HANDICAP_HOME_MINUS_4_5((short) 1116, new HandicapChecker()),// «Ф1(-4,5)»
    HANDICAP_HOME_PLUS_4_5((short) 1117, new HandicapChecker()), // «Ф1(+4,5)»
    HANDICAP_HOME_MINUS_5_0((short) 1118, new HandicapChecker()),  // «Ф1(-5)»
    HANDICAP_HOME_PLUS_5_0((short) 1119, new HandicapChecker()),   // «Ф1(+5)»
    HANDICAP_HOME_MINUS_5_5((short) 1120, new HandicapChecker()),// «Ф1(-5,5)»
    HANDICAP_HOME_PLUS_5_5((short) 1121, new HandicapChecker()), // «Ф1(+5,5)»
    HANDICAP_HOME_MINUS_6_0((short) 1122, new HandicapChecker()),  // «Ф1(-6)»
    HANDICAP_HOME_PLUS_6_0((short) 1123, new HandicapChecker()),   // «Ф1(+6)»

    //========== 1151-1200. Фора гостей (Away team handicap) ==========
    HANDICAP_AWAY_0((short) 1151, new HandicapChecker()),        // «Ф2(0)»
    HANDICAP_AWAY_MINUS_1_0((short) 1152, new HandicapChecker()),  // «Ф2(-1)»
    HANDICAP_AWAY_PLUS_1_0((short) 1153, new HandicapChecker()),   // «Ф2(+1)»
    HANDICAP_AWAY_MINUS_1_5((short) 1154, new HandicapChecker()),// «Ф2(-1.5)»
    HANDICAP_AWAY_PLUS_1_5((short) 1155, new HandicapChecker()), // «Ф2(+1.5)»
    HANDICAP_AWAY_MINUS_2_0((short) 1156, new HandicapChecker()),  // «Ф2(-2)»
    HANDICAP_AWAY_PLUS_2_0((short) 1157, new HandicapChecker()),   // «Ф2(+2)»
    HANDICAP_AWAY_MINUS_2_5((short) 1158, new HandicapChecker()),// «Ф2(-2.5)»
    HANDICAP_AWAY_PLUS_2_5((short) 1159, new HandicapChecker()), // «Ф2(+2.5)»
    HANDICAP_AWAY_MINUS_3_0((short) 1160, new HandicapChecker()),  // «Ф2(-3)»
    HANDICAP_AWAY_PLUS_3_0((short) 1161, new HandicapChecker()),   // «Ф2(+3)»
    HANDICAP_AWAY_MINUS_3_5((short) 1162, new HandicapChecker()),// «Ф2(-3.5)»
    HANDICAP_AWAY_PLUS_3_5((short) 1163, new HandicapChecker()), // «Ф2(+3.5)»
    HANDICAP_AWAY_MINUS_4_0((short) 1164, new HandicapChecker()),  // «Ф2(-4)»
    HANDICAP_AWAY_PLUS_4_0((short) 1165, new HandicapChecker()),   // «Ф2(+4)»
    HANDICAP_AWAY_MINUS_4_5((short) 1166, new HandicapChecker()),// «Ф2(-4.5)»
    HANDICAP_AWAY_PLUS_4_5((short) 1167, new HandicapChecker()), // «Ф2(+4.5)»
    HANDICAP_AWAY_MINUS_5_0((short) 1168, new HandicapChecker()),  // «Ф2(-5)»
    HANDICAP_AWAY_PLUS_5_0((short) 1169, new HandicapChecker()),   // «Ф2(+5)»
    HANDICAP_AWAY_MINUS_5_5((short) 1170, new HandicapChecker()),// «Ф2(-5.5)»
    HANDICAP_AWAY_PLUS_5_5((short) 1171, new HandicapChecker()), // «Ф2(+5.5)»
    HANDICAP_AWAY_MINUS_6_0((short) 1172, new HandicapChecker()),  // «Ф2(-6)»
    HANDICAP_AWAY_PLUS_6_0((short) 1173, new HandicapChecker()),   // «Ф2(+6)»

    //========== 1201-1300. Счёт игры (Game Score) ==========
    GAME_SCORE_0_0((short) 1201, new GameScoreChecker()),  // «Счёт 0:0»
    GAME_SCORE_1_0((short) 1202, new GameScoreChecker()),  // «Счёт 1:0»
    GAME_SCORE_2_0((short) 1203, new GameScoreChecker()),  // «Счёт 2:0»
    GAME_SCORE_3_0((short) 1204, new GameScoreChecker()),  // «Счёт 3:0»
    GAME_SCORE_0_1((short) 1205, new GameScoreChecker()),  // «Счёт 0:1»
    GAME_SCORE_1_1((short) 1206, new GameScoreChecker()),  // «Счёт 1:1»
    GAME_SCORE_2_1((short) 1207, new GameScoreChecker()),  // «Счёт 2:1»
    GAME_SCORE_3_1((short) 1208, new GameScoreChecker()),  // «Счёт 3:1»
    GAME_SCORE_0_2((short) 1209, new GameScoreChecker()),  // «Счёт 0:2»
    GAME_SCORE_1_2((short) 1210, new GameScoreChecker()),  // «Счёт 1:2»
    GAME_SCORE_2_2((short) 1211, new GameScoreChecker()),  // «Счёт 2:2»
    GAME_SCORE_3_2((short) 1212, new GameScoreChecker()),  // «Счёт 3:2»
    GAME_SCORE_0_3((short) 1213, new GameScoreChecker()),  // «Счёт 0:3»
    GAME_SCORE_1_3((short) 1214, new GameScoreChecker()),  // «Счёт 1:3»
    GAME_SCORE_2_3((short) 1215, new GameScoreChecker()),  // «Счёт 2:3»
    GAME_SCORE_3_3((short) 1216, new GameScoreChecker()),  // «Счёт 3:3»

    // Полный список от 0:4 до 7:7
    GAME_SCORE_0_4((short) 1251, new GameScoreChecker()),  // «Счёт 0:4»
    GAME_SCORE_1_4((short) 1252, new GameScoreChecker()),  // «Счёт 1:4»
    GAME_SCORE_2_4((short) 1253, new GameScoreChecker()),  // «Счёт 2:4»
    GAME_SCORE_3_4((short) 1254, new GameScoreChecker()),  // «Счёт 3:4»
    GAME_SCORE_4_0((short) 1255, new GameScoreChecker()),  // «Счёт 4:0»
    GAME_SCORE_4_1((short) 1256, new GameScoreChecker()),  // «Счёт 4:1»
    GAME_SCORE_4_2((short) 1257, new GameScoreChecker()),  // «Счёт 4:2»
    GAME_SCORE_4_3((short) 1258, new GameScoreChecker()),  // «Счёт 4:3»
    GAME_SCORE_4_4((short) 1259, new GameScoreChecker()),  // «Счёт 4:4»
    GAME_SCORE_0_5((short) 1260, new GameScoreChecker()),  // «Счёт 0:5»
    GAME_SCORE_1_5((short) 1261, new GameScoreChecker()),  // «Счёт 1:5»
    GAME_SCORE_2_5((short) 1262, new GameScoreChecker()),  // «Счёт 2:5»
    GAME_SCORE_3_5((short) 1263, new GameScoreChecker()),  // «Счёт 3:5»
    GAME_SCORE_4_5((short) 1264, new GameScoreChecker()),  // «Счёт 4:5»
    GAME_SCORE_5_0((short) 1265, new GameScoreChecker()),  // «Счёт 5:0»
    GAME_SCORE_5_1((short) 1266, new GameScoreChecker()),  // «Счёт 5:1»
    GAME_SCORE_5_2((short) 1267, new GameScoreChecker()),  // «Счёт 5:2»
    GAME_SCORE_5_3((short) 1268, new GameScoreChecker()),  // «Счёт 5:3»
    GAME_SCORE_5_4((short) 1269, new GameScoreChecker()),  // «Счёт 5:4»
    GAME_SCORE_5_5((short) 1270, new GameScoreChecker()),  // «Счёт 5:5»
    GAME_SCORE_0_6((short) 1271, new GameScoreChecker()),  // «Счёт 0:6»
    GAME_SCORE_1_6((short) 1272, new GameScoreChecker()),  // «Счёт 1:6»
    GAME_SCORE_2_6((short) 1273, new GameScoreChecker()),  // «Счёт 2:6»
    GAME_SCORE_3_6((short) 1274, new GameScoreChecker()),  // «Счёт 3:6»
    GAME_SCORE_4_6((short) 1275, new GameScoreChecker()),  // «Счёт 4:6»
    GAME_SCORE_5_6((short) 1276, new GameScoreChecker()),  // «Счёт 5:6»
    GAME_SCORE_6_0((short) 1277, new GameScoreChecker()),  // «Счёт 6:0»
    GAME_SCORE_6_1((short) 1278, new GameScoreChecker()),  // «Счёт 6:1»
    GAME_SCORE_6_2((short) 1279, new GameScoreChecker()),  // «Счёт 6:2»
    GAME_SCORE_6_3((short) 1280, new GameScoreChecker()),  // «Счёт 6:3»
    GAME_SCORE_6_4((short) 1281, new GameScoreChecker()),  // «Счёт 6:4»
    GAME_SCORE_6_5((short) 1282, new GameScoreChecker()),  // «Счёт 6:5»
    GAME_SCORE_6_6((short) 1283, new GameScoreChecker()),  // «Счёт 6:6»
    GAME_SCORE_0_7((short) 1284, new GameScoreChecker()),  // «Счёт 0:7»
    GAME_SCORE_1_7((short) 1285, new GameScoreChecker()),  // «Счёт 1:7»
    GAME_SCORE_2_7((short) 1286, new GameScoreChecker()),  // «Счёт 2:7»
    GAME_SCORE_3_7((short) 1287, new GameScoreChecker()),  // «Счёт 3:7»
    GAME_SCORE_4_7((short) 1288, new GameScoreChecker()),  // «Счёт 4:7»
    GAME_SCORE_5_7((short) 1289, new GameScoreChecker()),  // «Счёт 5:7»
    GAME_SCORE_6_7((short) 1290, new GameScoreChecker()),  // «Счёт 6:7»
    GAME_SCORE_7_0((short) 1291, new GameScoreChecker()),  // «Счёт 7:0»
    GAME_SCORE_7_1((short) 1292, new GameScoreChecker()),  // «Счёт 7:1»
    GAME_SCORE_7_2((short) 1293, new GameScoreChecker()),  // «Счёт 7:2»
    GAME_SCORE_7_3((short) 1294, new GameScoreChecker()),  // «Счёт 7:3»
    GAME_SCORE_7_4((short) 1295, new GameScoreChecker()),  // «Счёт 7:4»
    GAME_SCORE_7_5((short) 1296, new GameScoreChecker()),  // «Счёт 7:5»
    GAME_SCORE_7_6((short) 1297, new GameScoreChecker()),  // «Счёт 7:6»
    GAME_SCORE_7_7((short) 1298, new GameScoreChecker()),  // «Счёт 7:7»

    //========== 1301-1350. Голы (Goals) ==========
    BOTH_TEAMS_SCORE((short) 1301, new GoalsChecker()),    // «Обе забьют»
    HOME_TEAM_SCORES((short) 1302, new GoalsChecker()),    // «Хозяева забьют»
    AWAY_TEAM_SCORES((short) 1303, new GoalsChecker()),    // «Гости забьют»

    //========== 1351-1400. Голы по таймам (Goals by halftimes) ==========
    HOME_SCORES_1ST_HALF((short) 1351, new GoalsChecker()),         // «Хозяева забьют в 1 тайме»
    HOME_SCORES_2ND_HALF((short) 1352, new GoalsChecker()),         // «Хозяева забьют во 2 тайме»
    AWAY_SCORES_1ST_HALF((short) 1353, new GoalsChecker()),         // «Гости забьют в 1 тайме»
    AWAY_SCORES_2ND_HALF((short) 1354, new GoalsChecker()),         // «Гости забьют во 2 тайме»
    HOME_SCORES_BOTH_HALVES((short) 1355, new GoalsChecker()),      // «Хозяева забьют в обоих таймах»
    AWAY_SCORES_BOTH_HALVES((short) 1356, new GoalsChecker()),      // «Гости забьют в обоих таймах»
    BOTH_TEAMS_SCORE_1ST_HALF((short) 1357, new GoalsChecker()),    // «Обе забьют в 1 тайме»
    BOTH_TEAMS_SCORE_2ND_HALF((short) 1358, new GoalsChecker()),    // «Обе забьют во 2 тайме»
    BOTH_TEAMS_SCORE_BOTH_HALVES((short) 1359, new GoalsChecker()), // «Обе забьют в обоих таймах»
    GOALS_IN_BOTH_HALVES((short) 1360, new GoalsChecker()),         // «Голы в обоих таймах»

    //========== 1401-1450. Результат матча + Обе забьют (Game result + Both team score) ==========
    HOME_WIN_AND_BOTH_TEAMS_SCORE((short) 1401, new GoalsChecker()),     // «П1 + Обе забьют»
    AWAY_WIN_AND_BOTH_TEAMS_SCORE((short) 1402, new GoalsChecker()),     // «П2 + Обе забьют»
    HOME_OR_DRAW_AND_BOTH_TEAMS_SCORE((short) 1403, new GoalsChecker()), // «1Х + Обе забьют»
    AWAY_OR_DRAW_AND_BOTH_TEAMS_SCORE((short) 1404, new GoalsChecker()), // «Х2 + Обе забьют»
    DRAW_AND_BOTH_TEAMS_SCORE((short) 1405, new GoalsChecker()),         // «Х + Обе забьют»
    HOME_OR_AWAY_AND_BOTH_TEAMS_SCORE((short) 1406, new GoalsChecker()), // «12 + Обе забьют»

    //========== 1451-1500. Любая забьет больше чем (Scores More Than) ==========
    ANY_TEAM_SCORES_2_OR_MORE((short) 1451, new GoalsChecker()), // «Любая команда забьет 2 и больше голов»
    ANY_TEAM_SCORES_3_OR_MORE((short) 1452, new GoalsChecker()), // «Любая команда забьет 3 и больше голов»
    ANY_TEAM_SCORES_4_OR_MORE((short) 1453, new GoalsChecker()), // «Любая команда забьет 4 и больше голов»
    ANY_TEAM_SCORES_5_OR_MORE((short) 1454, new GoalsChecker()), // «Любая команда забьет 5 и больше голов»

    //========== 1501-1550. Обе забьют + тотал меньше (Both Team Score + Goals Amount (Under)) ==========
    BOTH_TEAMS_SCORE_AND_UNDER_1_5((short) 1501, new GoalsChecker()),  // «ОЗ +  ТМ 1,5»
    BOTH_TEAMS_SCORE_AND_UNDER_2_0((short) 1502, new GoalsChecker()),    // «ОЗ +  ТМ 2»
    BOTH_TEAMS_SCORE_AND_UNDER_2_5((short) 1503, new GoalsChecker()),  // «ОЗ +  ТМ 2,5»
    BOTH_TEAMS_SCORE_AND_UNDER_3_0((short) 1504, new GoalsChecker()),    // «ОЗ +  ТМ 3»
    BOTH_TEAMS_SCORE_AND_UNDER_3_5((short) 1505, new GoalsChecker()),  // «ОЗ +  ТМ 3,5»
    BOTH_TEAMS_SCORE_AND_UNDER_4_0((short) 1506, new GoalsChecker()),    // «ОЗ +  ТМ 4»
    BOTH_TEAMS_SCORE_AND_UNDER_4_5((short) 1507, new GoalsChecker()),  // «ОЗ +  ТМ 4,5»
    BOTH_TEAMS_SCORE_AND_UNDER_5_0((short) 1508, new GoalsChecker()),    // «ОЗ +  ТМ 5»
    BOTH_TEAMS_SCORE_AND_UNDER_5_5((short) 1509, new GoalsChecker()),  // «ОЗ +  ТМ 5,5»
    BOTH_TEAMS_SCORE_AND_UNDER_6_0((short) 1510, new GoalsChecker()),  // «ОЗ +  ТМ 6»
    BOTH_TEAMS_SCORE_AND_UNDER_6_5((short) 1511, new GoalsChecker()),  // «ОЗ +  ТМ 6,5»

    //========== 1551-1600. Обе забьют + тотал больше (Both Team Score + Goals Amount (Over)) ==========
    BOTH_TEAMS_SCORE_AND_OVER_1_5((short) 1551, new GoalsChecker()),   // «ОЗ +  ТБ 1,5»
    BOTH_TEAMS_SCORE_AND_OVER_2_0((short) 1552, new GoalsChecker()),     // «ОЗ +  ТБ 2»
    BOTH_TEAMS_SCORE_AND_OVER_2_5((short) 1553, new GoalsChecker()),   // «ОЗ +  ТБ 2,5»
    BOTH_TEAMS_SCORE_AND_OVER_3_0((short) 1554, new GoalsChecker()),     // «ОЗ +  ТБ 3»
    BOTH_TEAMS_SCORE_AND_OVER_3_5((short) 1555, new GoalsChecker()),   // «ОЗ +  ТБ 3,5»
    BOTH_TEAMS_SCORE_AND_OVER_4_0((short) 1556, new GoalsChecker()),     // «ОЗ +  ТБ 4»
    BOTH_TEAMS_SCORE_AND_OVER_4_5((short) 1557, new GoalsChecker()),   // «ОЗ +  ТБ 4,5»
    BOTH_TEAMS_SCORE_AND_OVER_5_0((short) 1558, new GoalsChecker()),     // «ОЗ +  ТБ 5»
    BOTH_TEAMS_SCORE_AND_OVER_5_5((short) 1559, new GoalsChecker()),   // «ОЗ +  ТБ 5,5»
    BOTH_TEAMS_SCORE_AND_OVER_6_0((short) 1560, new GoalsChecker()),   // «ОЗ +  ТБ 6»
    BOTH_TEAMS_SCORE_AND_OVER_6_5((short) 1561, new GoalsChecker()),   // «ОЗ +  ТБ 6,5»

    // =================================================================================================
    // Ставки на таймы являются отдельной крупной группой,
    // поэтому под них выделен особый диапазон кодов, начиная с 3000
    // =================================================================================================

    //========== 30. Результаты по таймам (Half-time Results) ==========
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

    private static final Map<Short, BetTitleCode> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(BetTitleCode::getCode, Function.identity()));

    /**
     * Найти enum по его числовому коду.
     *
     * @param code short-код ставки
     * @return соответствующий BetTitleCode или null, если не найден
     */
    public static BetTitleCode fromCode(short code) {
        return CODE_MAP.get(code);
    }

    public Bet.BetStatus evaluate(GameResult gameResult) {
        if (checker == null) {
            throw new IllegalStateException("No BetChecker defined for code " + code);
        }
        return checker.check(gameResult, this);
    }

    public static Bet.BetStatus evaluateByCode(short code, GameResult gameResult) {
        BetTitleCode bet = fromCode(code);
        if (bet == null) {
            throw new IllegalArgumentException("Unknown bet code: " + code);
        }
        return bet.evaluate(gameResult);
    }
}
