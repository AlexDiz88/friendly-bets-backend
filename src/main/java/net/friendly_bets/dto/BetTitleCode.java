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
    HOME_WIN((short) 101, new GameResultChecker(), "П1"),
    DRAW((short) 102, new GameResultChecker(), "Х"),
    AWAY_WIN((short) 103, new GameResultChecker(), "П2"),
    HOME_WIN_OR_DRAW((short) 104, new GameResultChecker(), "1Х"),
    HOME_OR_AWAY_WIN((short) 105, new GameResultChecker(), "12"),
    AWAY_WIN_OR_DRAW((short) 106, new GameResultChecker(), "Х2"),

    //========== 201–250. П1 + Тотал меньше (Home win + Goals UNDER) ==========
    HOME_WIN_AND_UNDER_1_0((short) 201, new GameResultWithTotalChecker(), "П1 + ТМ 1"),
    HOME_WIN_AND_UNDER_1_5((short) 202, new GameResultWithTotalChecker(), "П1 + ТМ 1,5"),
    HOME_WIN_AND_UNDER_2_0((short) 203, new GameResultWithTotalChecker(), "П1 + ТМ 2"),
    HOME_WIN_AND_UNDER_2_5((short) 204, new GameResultWithTotalChecker(), "П1 + ТМ 2,5"),
    HOME_WIN_AND_UNDER_3_0((short) 205, new GameResultWithTotalChecker(), "П1 + ТМ 3"),
    HOME_WIN_AND_UNDER_3_5((short) 206, new GameResultWithTotalChecker(), "П1 + ТМ 3,5"),
    HOME_WIN_AND_UNDER_4_0((short) 207, new GameResultWithTotalChecker(), "П1 + ТМ 4"),
    HOME_WIN_AND_UNDER_4_5((short) 208, new GameResultWithTotalChecker(), "П1 + ТМ 4,5"),
    HOME_WIN_AND_UNDER_5_0((short) 209, new GameResultWithTotalChecker(), "П1 + ТМ 5"),
    HOME_WIN_AND_UNDER_5_5((short) 210, new GameResultWithTotalChecker(), "П1 + ТМ 5,5"),
    HOME_WIN_AND_UNDER_6_0((short) 211, new GameResultWithTotalChecker(), "П1 + ТМ 6"),

    //========== 251–300. П1 + Тотал больше (Home win + Goals OVER) ==========
    HOME_WIN_AND_OVER_1_0((short) 251, new GameResultWithTotalChecker(), "П1 + ТБ 1"),
    HOME_WIN_AND_OVER_1_5((short) 252, new GameResultWithTotalChecker(), "П1 + ТБ 1,5"),
    HOME_WIN_AND_OVER_2_0((short) 253, new GameResultWithTotalChecker(), "П1 + ТБ 2"),
    HOME_WIN_AND_OVER_2_5((short) 254, new GameResultWithTotalChecker(), "П1 + ТБ 2,5"),
    HOME_WIN_AND_OVER_3_0((short) 255, new GameResultWithTotalChecker(), "П1 + ТБ 3"),
    HOME_WIN_AND_OVER_3_5((short) 256, new GameResultWithTotalChecker(), "П1 + ТБ 3,5"),
    HOME_WIN_AND_OVER_4_0((short) 257, new GameResultWithTotalChecker(), "П1 + ТБ 4"),
    HOME_WIN_AND_OVER_4_5((short) 258, new GameResultWithTotalChecker(), "П1 + ТБ 4,5"),
    HOME_WIN_AND_OVER_5_0((short) 259, new GameResultWithTotalChecker(), "П1 + ТБ 5"),
    HOME_WIN_AND_OVER_5_5((short) 260, new GameResultWithTotalChecker(), "П1 + ТБ 5,5"),
    HOME_WIN_AND_OVER_6_0((short) 261, new GameResultWithTotalChecker(), "П1 + ТБ 6"),

    //========== 301–350. 1Х + Тотал меньше (Home win or Draw (1X) + Goals UNDER) ==========
    HOME_OR_DRAW_AND_UNDER_1_0((short) 301, new GameResultWithTotalChecker(), "1Х + ТМ 1"),
    HOME_OR_DRAW_AND_UNDER_1_5((short) 302, new GameResultWithTotalChecker(), "1Х + ТМ 1,5"),
    HOME_OR_DRAW_AND_UNDER_2_0((short) 303, new GameResultWithTotalChecker(), "1Х + ТМ 2"),
    HOME_OR_DRAW_AND_UNDER_2_5((short) 304, new GameResultWithTotalChecker(), "1Х + ТМ 2,5"),
    HOME_OR_DRAW_AND_UNDER_3_0((short) 305, new GameResultWithTotalChecker(), "1Х + ТМ 3"),
    HOME_OR_DRAW_AND_UNDER_3_5((short) 306, new GameResultWithTotalChecker(), "1Х + ТМ 3,5"),
    HOME_OR_DRAW_AND_UNDER_4_0((short) 307, new GameResultWithTotalChecker(), "1Х + ТМ 4"),
    HOME_OR_DRAW_AND_UNDER_4_5((short) 308, new GameResultWithTotalChecker(), "1Х + ТМ 4,5"),
    HOME_OR_DRAW_AND_UNDER_5_0((short) 309, new GameResultWithTotalChecker(), "1Х + ТМ 5"),
    HOME_OR_DRAW_AND_UNDER_5_5((short) 310, new GameResultWithTotalChecker(), "1Х + ТМ 5,5"),
    HOME_OR_DRAW_AND_UNDER_6_0((short) 311, new GameResultWithTotalChecker(), "1Х + ТМ 6"),

    //========== 351–400. 1Х + Тотал больше (Home win or Draw (1X) + Goals OVER) ==========
    HOME_OR_DRAW_AND_OVER_1_0((short) 351, new GameResultWithTotalChecker(), "1Х + ТБ 1"),
    HOME_OR_DRAW_AND_OVER_1_5((short) 352, new GameResultWithTotalChecker(), "1Х + ТБ 1,5"),
    HOME_OR_DRAW_AND_OVER_2_0((short) 353, new GameResultWithTotalChecker(), "1Х + ТБ 2"),
    HOME_OR_DRAW_AND_OVER_2_5((short) 354, new GameResultWithTotalChecker(), "1Х + ТБ 2,5"),
    HOME_OR_DRAW_AND_OVER_3_0((short) 355, new GameResultWithTotalChecker(), "1Х + ТБ 3"),
    HOME_OR_DRAW_AND_OVER_3_5((short) 356, new GameResultWithTotalChecker(), "1Х + ТБ 3,5"),
    HOME_OR_DRAW_AND_OVER_4_0((short) 357, new GameResultWithTotalChecker(), "1Х + ТБ 4"),
    HOME_OR_DRAW_AND_OVER_4_5((short) 358, new GameResultWithTotalChecker(), "1Х + ТБ 4,5"),
    HOME_OR_DRAW_AND_OVER_5_0((short) 359, new GameResultWithTotalChecker(), "1Х + ТБ 5"),
    HOME_OR_DRAW_AND_OVER_5_5((short) 360, new GameResultWithTotalChecker(), "1Х + ТБ 5,5"),
    HOME_OR_DRAW_AND_OVER_6_0((short) 361, new GameResultWithTotalChecker(), "1Х + ТБ 6"),

    //========== 401–450. Х + Тотал меньше (Draw (X) + Goals UNDER) ==========
    DRAW_AND_UNDER_1_0((short) 401, new GameResultWithTotalChecker(), "Х + ТМ 1"),
    DRAW_AND_UNDER_1_5((short) 402, new GameResultWithTotalChecker(), "Х + ТМ 1,5"),
    DRAW_AND_UNDER_2_0((short) 403, new GameResultWithTotalChecker(), "Х + ТМ 2"),
    DRAW_AND_UNDER_2_5((short) 404, new GameResultWithTotalChecker(), "Х + ТМ 2,5"),
    DRAW_AND_UNDER_3_0((short) 405, new GameResultWithTotalChecker(), "Х + ТМ 3"),
    DRAW_AND_UNDER_3_5((short) 406, new GameResultWithTotalChecker(), "Х + ТМ 3,5"),
    DRAW_AND_UNDER_4_0((short) 407, new GameResultWithTotalChecker(), "Х + ТМ 4"),
    DRAW_AND_UNDER_4_5((short) 408, new GameResultWithTotalChecker(), "Х + ТМ 4,5"),
    DRAW_AND_UNDER_5_0((short) 409, new GameResultWithTotalChecker(), "Х + ТМ 5"),
    DRAW_AND_UNDER_5_5((short) 410, new GameResultWithTotalChecker(), "Х + ТМ 5,5"),
    DRAW_AND_UNDER_6_0((short) 411, new GameResultWithTotalChecker(), "Х + ТМ 6"),

    //========== 451–500. Х + Тотал больше (Draw (X) + Goals OVER) ==========
    DRAW_AND_OVER_1_0((short) 451, new GameResultWithTotalChecker(), "Х + ТБ 1"),
    DRAW_AND_OVER_1_5((short) 452, new GameResultWithTotalChecker(), "Х + ТБ 1,5"),
    DRAW_AND_OVER_2_0((short) 453, new GameResultWithTotalChecker(), "Х + ТБ 2"),
    DRAW_AND_OVER_2_5((short) 454, new GameResultWithTotalChecker(), "Х + ТБ 2,5"),
    DRAW_AND_OVER_3_0((short) 455, new GameResultWithTotalChecker(), "Х + ТБ 3"),
    DRAW_AND_OVER_3_5((short) 456, new GameResultWithTotalChecker(), "Х + ТБ 3,5"),
    DRAW_AND_OVER_4_0((short) 457, new GameResultWithTotalChecker(), "Х + ТБ 4"),
    DRAW_AND_OVER_4_5((short) 458, new GameResultWithTotalChecker(), "Х + ТБ 4,5"),
    DRAW_AND_OVER_5_0((short) 459, new GameResultWithTotalChecker(), "Х + ТБ 5"),
    DRAW_AND_OVER_5_5((short) 460, new GameResultWithTotalChecker(), "Х + ТБ 5,5"),
    DRAW_AND_OVER_6_0((short) 461, new GameResultWithTotalChecker(), "Х + ТБ 6"),

    //========== 501–550. П2 + Тотал меньше (Away win + Goals UNDER) ==========
    AWAY_WIN_AND_UNDER_1_0((short) 501, new GameResultWithTotalChecker(), "П2 + ТМ 1"),
    AWAY_WIN_AND_UNDER_1_5((short) 502, new GameResultWithTotalChecker(), "П2 + ТМ 1,5"),
    AWAY_WIN_AND_UNDER_2_0((short) 503, new GameResultWithTotalChecker(), "П2 + ТМ 2"),
    AWAY_WIN_AND_UNDER_2_5((short) 504, new GameResultWithTotalChecker(), "П2 + ТМ 2,5"),
    AWAY_WIN_AND_UNDER_3_0((short) 505, new GameResultWithTotalChecker(), "П2 + ТМ 3"),
    AWAY_WIN_AND_UNDER_3_5((short) 506, new GameResultWithTotalChecker(), "П2 + ТМ 3,5"),
    AWAY_WIN_AND_UNDER_4_0((short) 507, new GameResultWithTotalChecker(), "П2 + ТМ 4"),
    AWAY_WIN_AND_UNDER_4_5((short) 508, new GameResultWithTotalChecker(), "П2 + ТМ 4,5"),
    AWAY_WIN_AND_UNDER_5_0((short) 509, new GameResultWithTotalChecker(), "П2 + ТМ 5"),
    AWAY_WIN_AND_UNDER_5_5((short) 510, new GameResultWithTotalChecker(), "П2 + ТМ 5,5"),
    AWAY_WIN_AND_UNDER_6_0((short) 511, new GameResultWithTotalChecker(), "П2 + ТМ 6"),

    //========== 551-600. П2 + Тотал больше (Away win + Goals OVER) ==========
    AWAY_WIN_AND_OVER_1_0((short) 551, new GameResultWithTotalChecker(), "П2 + ТБ 1"),
    AWAY_WIN_AND_OVER_1_5((short) 552, new GameResultWithTotalChecker(), "П2 + ТБ 1,5"),
    AWAY_WIN_AND_OVER_2_0((short) 553, new GameResultWithTotalChecker(), "П2 + ТБ 2"),
    AWAY_WIN_AND_OVER_2_5((short) 554, new GameResultWithTotalChecker(), "П2 + ТБ 2,5"),
    AWAY_WIN_AND_OVER_3_0((short) 555, new GameResultWithTotalChecker(), "П2 + ТБ 3"),
    AWAY_WIN_AND_OVER_3_5((short) 556, new GameResultWithTotalChecker(), "П2 + ТБ 3,5"),
    AWAY_WIN_AND_OVER_4_0((short) 557, new GameResultWithTotalChecker(), "П2 + ТБ 4"),
    AWAY_WIN_AND_OVER_4_5((short) 558, new GameResultWithTotalChecker(), "П2 + ТБ 4,5"),
    AWAY_WIN_AND_OVER_5_0((short) 559, new GameResultWithTotalChecker(), "П2 + ТБ 5"),
    AWAY_WIN_AND_OVER_5_5((short) 560, new GameResultWithTotalChecker(), "П2 + ТБ 5,5"),
    AWAY_WIN_AND_OVER_6_0((short) 561, new GameResultWithTotalChecker(), "П2 + ТБ 6"),

    //========== 601-650. Х2 + Тотал меньше (Draw or Away win + Goals UNDER) ==========
    AWAY_OR_DRAW_AND_UNDER_1_0((short) 601, new GameResultWithTotalChecker(), "Х2 + ТМ 1"),
    AWAY_OR_DRAW_AND_UNDER_1_5((short) 602, new GameResultWithTotalChecker(), "Х2 + ТМ 1,5"),
    AWAY_OR_DRAW_AND_UNDER_2_0((short) 603, new GameResultWithTotalChecker(), "Х2 + ТМ 2"),
    AWAY_OR_DRAW_AND_UNDER_2_5((short) 604, new GameResultWithTotalChecker(), "Х2 + ТМ 2,5"),
    AWAY_OR_DRAW_AND_UNDER_3_0((short) 605, new GameResultWithTotalChecker(), "Х2 + ТМ 3"),
    AWAY_OR_DRAW_AND_UNDER_3_5((short) 606, new GameResultWithTotalChecker(), "Х2 + ТМ 3,5"),
    AWAY_OR_DRAW_AND_UNDER_4_0((short) 607, new GameResultWithTotalChecker(), "Х2 + ТМ 4"),
    AWAY_OR_DRAW_AND_UNDER_4_5((short) 608, new GameResultWithTotalChecker(), "Х2 + ТМ 4,5"),
    AWAY_OR_DRAW_AND_UNDER_5_0((short) 609, new GameResultWithTotalChecker(), "Х2 + ТМ 5"),
    AWAY_OR_DRAW_AND_UNDER_5_5((short) 610, new GameResultWithTotalChecker(), "Х2 + ТМ 5,5"),
    AWAY_OR_DRAW_AND_UNDER_6_0((short) 611, new GameResultWithTotalChecker(), "Х2 + ТМ 6"),

    //========== 651-700. Х2 + Тотал больше (Draw or Away win + Goals OVER) ==========
    AWAY_OR_DRAW_AND_OVER_1_0((short) 651, new GameResultWithTotalChecker(), "Х2 + ТБ 1"),
    AWAY_OR_DRAW_AND_OVER_1_5((short) 652, new GameResultWithTotalChecker(), "Х2 + ТБ 1,5"),
    AWAY_OR_DRAW_AND_OVER_2_0((short) 653, new GameResultWithTotalChecker(), "Х2 + ТБ 2"),
    AWAY_OR_DRAW_AND_OVER_2_5((short) 654, new GameResultWithTotalChecker(), "Х2 + ТБ 2,5"),
    AWAY_OR_DRAW_AND_OVER_3_0((short) 655, new GameResultWithTotalChecker(), "Х2 + ТБ 3"),
    AWAY_OR_DRAW_AND_OVER_3_5((short) 656, new GameResultWithTotalChecker(), "Х2 + ТБ 3,5"),
    AWAY_OR_DRAW_AND_OVER_4_0((short) 657, new GameResultWithTotalChecker(), "Х2 + ТБ 4"),
    AWAY_OR_DRAW_AND_OVER_4_5((short) 658, new GameResultWithTotalChecker(), "Х2 + ТБ 4,5"),
    AWAY_OR_DRAW_AND_OVER_5_0((short) 659, new GameResultWithTotalChecker(), "Х2 + ТБ 5"),
    AWAY_OR_DRAW_AND_OVER_5_5((short) 660, new GameResultWithTotalChecker(), "Х2 + ТБ 5,5"),
    AWAY_OR_DRAW_AND_OVER_6_0((short) 661, new GameResultWithTotalChecker(), "Х2 + ТБ 6"),

    //========== 701-750. 12 + Тотал меньше (Home or Away win + Goals UNDER) ==========
    HOME_OR_AWAY_AND_UNDER_1_0((short) 701, new GameResultWithTotalChecker(), "12 + ТМ 1"),
    HOME_OR_AWAY_AND_UNDER_1_5((short) 702, new GameResultWithTotalChecker(), "12 + ТМ 1,5"),
    HOME_OR_AWAY_AND_UNDER_2_0((short) 703, new GameResultWithTotalChecker(), "12 + ТМ 2"),
    HOME_OR_AWAY_AND_UNDER_2_5((short) 704, new GameResultWithTotalChecker(), "12 + ТМ 2,5"),
    HOME_OR_AWAY_AND_UNDER_3_0((short) 705, new GameResultWithTotalChecker(), "12 + ТМ 3"),
    HOME_OR_AWAY_AND_UNDER_3_5((short) 706, new GameResultWithTotalChecker(), "12 + ТМ 3,5"),
    HOME_OR_AWAY_AND_UNDER_4_0((short) 707, new GameResultWithTotalChecker(), "12 + ТМ 4"),
    HOME_OR_AWAY_AND_UNDER_4_5((short) 708, new GameResultWithTotalChecker(), "12 + ТМ 4,5"),
    HOME_OR_AWAY_AND_UNDER_5_0((short) 709, new GameResultWithTotalChecker(), "12 + ТМ 5"),
    HOME_OR_AWAY_AND_UNDER_5_5((short) 710, new GameResultWithTotalChecker(), "12 + ТМ 5,5"),
    HOME_OR_AWAY_AND_UNDER_6_0((short) 711, new GameResultWithTotalChecker(), "12 + ТМ 6"),

    //========== 751-800. 12 + Тотал больше (Home or Away win + Goals OVER) ==========
    HOME_OR_AWAY_AND_OVER_1_0((short) 751, new GameResultWithTotalChecker(), "12 + ТБ 1"),
    HOME_OR_AWAY_AND_OVER_1_5((short) 752, new GameResultWithTotalChecker(), "12 + ТБ 1,5"),
    HOME_OR_AWAY_AND_OVER_2_0((short) 753, new GameResultWithTotalChecker(), "12 + ТБ 2"),
    HOME_OR_AWAY_AND_OVER_2_5((short) 754, new GameResultWithTotalChecker(), "12 + ТБ 2,5"),
    HOME_OR_AWAY_AND_OVER_3_0((short) 755, new GameResultWithTotalChecker(), "12 + ТБ 3"),
    HOME_OR_AWAY_AND_OVER_3_5((short) 756, new GameResultWithTotalChecker(), "12 + ТБ 3,5"),
    HOME_OR_AWAY_AND_OVER_4_0((short) 757, new GameResultWithTotalChecker(), "12 + ТБ 4"),
    HOME_OR_AWAY_AND_OVER_4_5((short) 758, new GameResultWithTotalChecker(), "12 + ТБ 4,5"),
    HOME_OR_AWAY_AND_OVER_5_0((short) 759, new GameResultWithTotalChecker(), "12 + ТБ 5"),
    HOME_OR_AWAY_AND_OVER_5_5((short) 760, new GameResultWithTotalChecker(), "12 + ТБ 5,5"),
    HOME_OR_AWAY_AND_OVER_6_0((short) 761, new GameResultWithTotalChecker(), "12 + ТБ 6"),

    //========== 801-850. Тотал голов Меньше (Total goals UNDER) ==========
    TOTAL_UNDER_1_0((short) 801, new TotalChecker(), "ТМ 1"),
    TOTAL_UNDER_1_5((short) 802, new TotalChecker(), "ТМ 1,5"),
    TOTAL_UNDER_2_0((short) 803, new TotalChecker(), "ТМ 2"),
    TOTAL_UNDER_2_5((short) 804, new TotalChecker(), "ТМ 2,5"),
    TOTAL_UNDER_3_0((short) 805, new TotalChecker(), "ТМ 3"),
    TOTAL_UNDER_3_5((short) 806, new TotalChecker(), "ТМ 3,5"),
    TOTAL_UNDER_4_0((short) 807, new TotalChecker(), "ТМ 4"),
    TOTAL_UNDER_4_5((short) 808, new TotalChecker(), "ТМ 4,5"),
    TOTAL_UNDER_5_0((short) 809, new TotalChecker(), "ТМ 5"),
    TOTAL_UNDER_5_5((short) 810, new TotalChecker(), "ТМ 5,5"),
    TOTAL_UNDER_6_0((short) 811, new TotalChecker(), "ТМ 6"),
    TOTAL_UNDER_6_5((short) 812, new TotalChecker(), "ТМ 6,5"),

    //========== 851-900. Тотал голов Больше (Total goals OVER) ==========
    TOTAL_OVER_1_0((short) 851, new TotalChecker(), "ТБ 1"),
    TOTAL_OVER_1_5((short) 852, new TotalChecker(), "ТБ 1,5"),
    TOTAL_OVER_2_0((short) 853, new TotalChecker(), "ТБ 2"),
    TOTAL_OVER_2_5((short) 854, new TotalChecker(), "ТБ 2,5"),
    TOTAL_OVER_3_0((short) 855, new TotalChecker(), "ТБ 3"),
    TOTAL_OVER_3_5((short) 856, new TotalChecker(), "ТБ 3,5"),
    TOTAL_OVER_4_0((short) 857, new TotalChecker(), "ТБ 4"),
    TOTAL_OVER_4_5((short) 858, new TotalChecker(), "ТБ 4,5"),
    TOTAL_OVER_5_0((short) 859, new TotalChecker(), "ТБ 5"),
    TOTAL_OVER_5_5((short) 860, new TotalChecker(), "ТБ 5,5"),
    TOTAL_OVER_6_0((short) 861, new TotalChecker(), "ТБ 6"),
    TOTAL_OVER_6_5((short) 862, new TotalChecker(), "ТБ 6,5"),

    //========== 901-950. Хозяева ИТМ (Home team UNDER) ==========
    HOME_TEAM_UNDER_1_0((short) 901, new TotalChecker(), "Хозяева ИТМ 1"),
    HOME_TEAM_UNDER_1_5((short) 902, new TotalChecker(), "Хозяева ИТМ 1,5"),
    HOME_TEAM_UNDER_2_0((short) 903, new TotalChecker(), "Хозяева ИТМ 2"),
    HOME_TEAM_UNDER_2_5((short) 904, new TotalChecker(), "Хозяева ИТМ 2,5"),
    HOME_TEAM_UNDER_3_0((short) 905, new TotalChecker(), "Хозяева ИТМ 3"),
    HOME_TEAM_UNDER_3_5((short) 906, new TotalChecker(), "Хозяева ИТМ 3,5"),
    HOME_TEAM_UNDER_4_0((short) 907, new TotalChecker(), "Хозяева ИТМ 4"),
    HOME_TEAM_UNDER_4_5((short) 908, new TotalChecker(), "Хозяева ИТМ 4,5"),
    HOME_TEAM_UNDER_5_0((short) 909, new TotalChecker(), "Хозяева ИТМ 5"),
    HOME_TEAM_UNDER_5_5((short) 910, new TotalChecker(), "Хозяева ИТМ 5,5"),
    HOME_TEAM_UNDER_6_0((short) 911, new TotalChecker(), "Хозяева ИТМ 6"),
    HOME_TEAM_UNDER_6_5((short) 912, new TotalChecker(), "Хозяева ИТМ 6,5"),

    //========== 951-1000. Хозяева ИТБ (Home team OVER) ==========
    HOME_TEAM_OVER_1_0((short) 951, new TotalChecker(), "Хозяева ИТБ 1"),
    HOME_TEAM_OVER_1_5((short) 952, new TotalChecker(), "Хозяева ИТБ 1,5"),
    HOME_TEAM_OVER_2_0((short) 953, new TotalChecker(), "Хозяева ИТБ 2"),
    HOME_TEAM_OVER_2_5((short) 954, new TotalChecker(), "Хозяева ИТБ 2,5"),
    HOME_TEAM_OVER_3_0((short) 955, new TotalChecker(), "Хозяева ИТБ 3"),
    HOME_TEAM_OVER_3_5((short) 956, new TotalChecker(), "Хозяева ИТБ 3,5"),
    HOME_TEAM_OVER_4_0((short) 957, new TotalChecker(), "Хозяева ИТБ 4"),
    HOME_TEAM_OVER_4_5((short) 958, new TotalChecker(), "Хозяева ИТБ 4,5"),
    HOME_TEAM_OVER_5_0((short) 959, new TotalChecker(), "Хозяева ИТБ 5"),
    HOME_TEAM_OVER_5_5((short) 960, new TotalChecker(), "Хозяева ИТБ 5,5"),
    HOME_TEAM_OVER_6_0((short) 961, new TotalChecker(), "Хозяева ИТБ 6"),
    HOME_TEAM_OVER_6_5((short) 962, new TotalChecker(), "Хозяева ИТБ 6,5"),

    //========== 1001-1050. Гости ИТМ (Away team UNDER) ==========
    AWAY_TEAM_UNDER_1_0((short) 1001, new TotalChecker(), "Гости ИТМ 1"),
    AWAY_TEAM_UNDER_1_5((short) 1002, new TotalChecker(), "Гости ИТМ 1,5"),
    AWAY_TEAM_UNDER_2_0((short) 1003, new TotalChecker(), "Гости ИТМ 2"),
    AWAY_TEAM_UNDER_2_5((short) 1004, new TotalChecker(), "Гости ИТМ 2,5"),
    AWAY_TEAM_UNDER_3_0((short) 1005, new TotalChecker(), "Гости ИТМ 3"),
    AWAY_TEAM_UNDER_3_5((short) 1006, new TotalChecker(), "Гости ИТМ 3,5"),
    AWAY_TEAM_UNDER_4_0((short) 1007, new TotalChecker(), "Гости ИТМ 4"),
    AWAY_TEAM_UNDER_4_5((short) 1008, new TotalChecker(), "Гости ИТМ 4,5"),
    AWAY_TEAM_UNDER_5_0((short) 1009, new TotalChecker(), "Гости ИТМ 5"),
    AWAY_TEAM_UNDER_5_5((short) 1010, new TotalChecker(), "Гости ИТМ 5,5"),
    AWAY_TEAM_UNDER_6_0((short) 1011, new TotalChecker(), "Гости ИТМ 6"),
    AWAY_TEAM_UNDER_6_5((short) 1012, new TotalChecker(), "Гости ИТМ 6,5"),

    //========== 1051-1100. Гости ИТБ (Away team OVER) ==========
    AWAY_TEAM_OVER_1_0((short) 1051, new TotalChecker(), "Гости ИТБ 1"),
    AWAY_TEAM_OVER_1_5((short) 1052, new TotalChecker(), "Гости ИТБ 1,5"),
    AWAY_TEAM_OVER_2_0((short) 1053, new TotalChecker(), "Гости ИТБ 2"),
    AWAY_TEAM_OVER_2_5((short) 1054, new TotalChecker(), "Гости ИТБ 2,5"),
    AWAY_TEAM_OVER_3_0((short) 1055, new TotalChecker(), "Гости ИТБ 3"),
    AWAY_TEAM_OVER_3_5((short) 1056, new TotalChecker(), "Гости ИТБ 3,5"),
    AWAY_TEAM_OVER_4_0((short) 1057, new TotalChecker(), "Гости ИТБ 4"),
    AWAY_TEAM_OVER_4_5((short) 1058, new TotalChecker(), "Гости ИТБ 4,5"),
    AWAY_TEAM_OVER_5_0((short) 1059, new TotalChecker(), "Гости ИТБ 5"),
    AWAY_TEAM_OVER_5_5((short) 1060, new TotalChecker(), "Гости ИТБ 5,5"),
    AWAY_TEAM_OVER_6_0((short) 1061, new TotalChecker(), "Гости ИТБ 6"),
    AWAY_TEAM_OVER_6_5((short) 1062, new TotalChecker(), "Гости ИТБ 6,5"),

    //========== 1101-1150. Фора хозяев (Home team handicap) ==========
    HANDICAP_HOME_0((short) 1101, new HandicapChecker(), "Ф1(0)"),
    HANDICAP_HOME_MINUS_1_0((short) 1102, new HandicapChecker(), "Ф1(-1)"),
    HANDICAP_HOME_PLUS_1_0((short) 1103, new HandicapChecker(), "Ф1(+1)"),
    HANDICAP_HOME_MINUS_1_5((short) 1104, new HandicapChecker(), "Ф1(-1,5)"),
    HANDICAP_HOME_PLUS_1_5((short) 1105, new HandicapChecker(), "Ф1(+1,5)"),
    HANDICAP_HOME_MINUS_2_0((short) 1106, new HandicapChecker(), "Ф1(-2)"),
    HANDICAP_HOME_PLUS_2_0((short) 1107, new HandicapChecker(), "Ф1(+2)"),
    HANDICAP_HOME_MINUS_2_5((short) 1108, new HandicapChecker(), "Ф1(-2,5)"),
    HANDICAP_HOME_PLUS_2_5((short) 1109, new HandicapChecker(), "Ф1(+2,5)"),
    HANDICAP_HOME_MINUS_3_0((short) 1110, new HandicapChecker(), "Ф1(-3)"),
    HANDICAP_HOME_PLUS_3_0((short) 1111, new HandicapChecker(), "Ф1(+3)"),
    HANDICAP_HOME_MINUS_3_5((short) 1112, new HandicapChecker(), "Ф1(-3,5)"),
    HANDICAP_HOME_PLUS_3_5((short) 1113, new HandicapChecker(), "Ф1(+3,5)"),
    HANDICAP_HOME_MINUS_4_0((short) 1114, new HandicapChecker(), "Ф1(-4)"),
    HANDICAP_HOME_PLUS_4_0((short) 1115, new HandicapChecker(), "Ф1(+4)"),
    HANDICAP_HOME_MINUS_4_5((short) 1116, new HandicapChecker(), "Ф1(-4,5)"),
    HANDICAP_HOME_PLUS_4_5((short) 1117, new HandicapChecker(), "Ф1(+4,5)"),
    HANDICAP_HOME_MINUS_5_0((short) 1118, new HandicapChecker(), "Ф1(-5)"),
    HANDICAP_HOME_PLUS_5_0((short) 1119, new HandicapChecker(), "Ф1(+5)"),
    HANDICAP_HOME_MINUS_5_5((short) 1120, new HandicapChecker(), "Ф1(-5,5)"),
    HANDICAP_HOME_PLUS_5_5((short) 1121, new HandicapChecker(), "Ф1(+5,5)"),
    HANDICAP_HOME_MINUS_6_0((short) 1122, new HandicapChecker(), "Ф1(-6)"),
    HANDICAP_HOME_PLUS_6_0((short) 1123, new HandicapChecker(), "Ф1(+6)"),

    //========== 1151-1200. Фора гостей (Away team handicap) ==========
    HANDICAP_AWAY_0((short) 1151, new HandicapChecker(), "Ф2(0)"),
    HANDICAP_AWAY_MINUS_1_0((short) 1152, new HandicapChecker(), "Ф2(-1)"),
    HANDICAP_AWAY_PLUS_1_0((short) 1153, new HandicapChecker(), "Ф2(+1)"),
    HANDICAP_AWAY_MINUS_1_5((short) 1154, new HandicapChecker(), "Ф2(-1.5)"),
    HANDICAP_AWAY_PLUS_1_5((short) 1155, new HandicapChecker(), "Ф2(+1.5)"),
    HANDICAP_AWAY_MINUS_2_0((short) 1156, new HandicapChecker(), "Ф2(-2)"),
    HANDICAP_AWAY_PLUS_2_0((short) 1157, new HandicapChecker(), "Ф2(+2)"),
    HANDICAP_AWAY_MINUS_2_5((short) 1158, new HandicapChecker(), "Ф2(-2.5)"),
    HANDICAP_AWAY_PLUS_2_5((short) 1159, new HandicapChecker(), "Ф2(+2.5)"),
    HANDICAP_AWAY_MINUS_3_0((short) 1160, new HandicapChecker(), "Ф2(-3)"),
    HANDICAP_AWAY_PLUS_3_0((short) 1161, new HandicapChecker(), "Ф2(+3)"),
    HANDICAP_AWAY_MINUS_3_5((short) 1162, new HandicapChecker(), "Ф2(-3.5)"),
    HANDICAP_AWAY_PLUS_3_5((short) 1163, new HandicapChecker(), "Ф2(+3.5)"),
    HANDICAP_AWAY_MINUS_4_0((short) 1164, new HandicapChecker(), "Ф2(-4)"),
    HANDICAP_AWAY_PLUS_4_0((short) 1165, new HandicapChecker(), "Ф2(+4)"),
    HANDICAP_AWAY_MINUS_4_5((short) 1166, new HandicapChecker(), "Ф2(-4.5)"),
    HANDICAP_AWAY_PLUS_4_5((short) 1167, new HandicapChecker(), "Ф2(+4.5)"),
    HANDICAP_AWAY_MINUS_5_0((short) 1168, new HandicapChecker(), "Ф2(-5)"),
    HANDICAP_AWAY_PLUS_5_0((short) 1169, new HandicapChecker(), "Ф2(+5)"),
    HANDICAP_AWAY_MINUS_5_5((short) 1170, new HandicapChecker(), "Ф2(-5.5)"),
    HANDICAP_AWAY_PLUS_5_5((short) 1171, new HandicapChecker(), "Ф2(+5.5)"),
    HANDICAP_AWAY_MINUS_6_0((short) 1172, new HandicapChecker(), "Ф2(-6)"),
    HANDICAP_AWAY_PLUS_6_0((short) 1173, new HandicapChecker(), "Ф2(+6)"),

    //========== 1201-1300. Счёт игры (Game Score) ==========
    GAME_SCORE_0_0((short) 1201, new GameScoreChecker(), "Счёт 0:0"),
    GAME_SCORE_1_0((short) 1202, new GameScoreChecker(), "Счёт 1:0"),
    GAME_SCORE_2_0((short) 1203, new GameScoreChecker(), "Счёт 2:0"),
    GAME_SCORE_3_0((short) 1204, new GameScoreChecker(), "Счёт 3:0"),
    GAME_SCORE_0_1((short) 1205, new GameScoreChecker(), "Счёт 0:1"),
    GAME_SCORE_1_1((short) 1206, new GameScoreChecker(), "Счёт 1:1"),
    GAME_SCORE_2_1((short) 1207, new GameScoreChecker(), "Счёт 2:1"),
    GAME_SCORE_3_1((short) 1208, new GameScoreChecker(), "Счёт 3:1"),
    GAME_SCORE_0_2((short) 1209, new GameScoreChecker(), "Счёт 0:2"),
    GAME_SCORE_1_2((short) 1210, new GameScoreChecker(), "Счёт 1:2"),
    GAME_SCORE_2_2((short) 1211, new GameScoreChecker(), "Счёт 2:2"),
    GAME_SCORE_3_2((short) 1212, new GameScoreChecker(), "Счёт 3:2"),
    GAME_SCORE_0_3((short) 1213, new GameScoreChecker(), "Счёт 0:3"),
    GAME_SCORE_1_3((short) 1214, new GameScoreChecker(), "Счёт 1:3"),
    GAME_SCORE_2_3((short) 1215, new GameScoreChecker(), "Счёт 2:3"),
    GAME_SCORE_3_3((short) 1216, new GameScoreChecker(), "Счёт 3:3"),

    // Полный список от 0:4 до 7:7
    GAME_SCORE_0_4((short) 1251, new GameScoreChecker(), "Счёт 0:4"),
    GAME_SCORE_1_4((short) 1252, new GameScoreChecker(), "Счёт 1:4"),
    GAME_SCORE_2_4((short) 1253, new GameScoreChecker(), "Счёт 2:4"),
    GAME_SCORE_3_4((short) 1254, new GameScoreChecker(), "Счёт 3:4"),
    GAME_SCORE_4_0((short) 1255, new GameScoreChecker(), "Счёт 4:0"),
    GAME_SCORE_4_1((short) 1256, new GameScoreChecker(), "Счёт 4:1"),
    GAME_SCORE_4_2((short) 1257, new GameScoreChecker(), "Счёт 4:2"),
    GAME_SCORE_4_3((short) 1258, new GameScoreChecker(), "Счёт 4:3"),
    GAME_SCORE_4_4((short) 1259, new GameScoreChecker(), "Счёт 4:4"),
    GAME_SCORE_0_5((short) 1260, new GameScoreChecker(), "Счёт 0:5"),
    GAME_SCORE_1_5((short) 1261, new GameScoreChecker(), "Счёт 1:5"),
    GAME_SCORE_2_5((short) 1262, new GameScoreChecker(), "Счёт 2:5"),
    GAME_SCORE_3_5((short) 1263, new GameScoreChecker(), "Счёт 3:5"),
    GAME_SCORE_4_5((short) 1264, new GameScoreChecker(), "Счёт 4:5"),
    GAME_SCORE_5_0((short) 1265, new GameScoreChecker(), "Счёт 5:0"),
    GAME_SCORE_5_1((short) 1266, new GameScoreChecker(), "Счёт 5:1"),
    GAME_SCORE_5_2((short) 1267, new GameScoreChecker(), "Счёт 5:2"),
    GAME_SCORE_5_3((short) 1268, new GameScoreChecker(), "Счёт 5:3"),
    GAME_SCORE_5_4((short) 1269, new GameScoreChecker(), "Счёт 5:4"),
    GAME_SCORE_5_5((short) 1270, new GameScoreChecker(), "Счёт 5:5"),
    GAME_SCORE_0_6((short) 1271, new GameScoreChecker(), "Счёт 0:6"),
    GAME_SCORE_1_6((short) 1272, new GameScoreChecker(), "Счёт 1:6"),
    GAME_SCORE_2_6((short) 1273, new GameScoreChecker(), "Счёт 2:6"),
    GAME_SCORE_3_6((short) 1274, new GameScoreChecker(), "Счёт 3:6"),
    GAME_SCORE_4_6((short) 1275, new GameScoreChecker(), "Счёт 4:6"),
    GAME_SCORE_5_6((short) 1276, new GameScoreChecker(), "Счёт 5:6"),
    GAME_SCORE_6_0((short) 1277, new GameScoreChecker(), "Счёт 6:0"),
    GAME_SCORE_6_1((short) 1278, new GameScoreChecker(), "Счёт 6:1"),
    GAME_SCORE_6_2((short) 1279, new GameScoreChecker(), "Счёт 6:2"),
    GAME_SCORE_6_3((short) 1280, new GameScoreChecker(), "Счёт 6:3"),
    GAME_SCORE_6_4((short) 1281, new GameScoreChecker(), "Счёт 6:4"),
    GAME_SCORE_6_5((short) 1282, new GameScoreChecker(), "Счёт 6:5"),
    GAME_SCORE_6_6((short) 1283, new GameScoreChecker(), "Счёт 6:6"),
    GAME_SCORE_0_7((short) 1284, new GameScoreChecker(), "Счёт 0:7"),
    GAME_SCORE_1_7((short) 1285, new GameScoreChecker(), "Счёт 1:7"),
    GAME_SCORE_2_7((short) 1286, new GameScoreChecker(), "Счёт 2:7"),
    GAME_SCORE_3_7((short) 1287, new GameScoreChecker(), "Счёт 3:7"),
    GAME_SCORE_4_7((short) 1288, new GameScoreChecker(), "Счёт 4:7"),
    GAME_SCORE_5_7((short) 1289, new GameScoreChecker(), "Счёт 5:7"),
    GAME_SCORE_6_7((short) 1290, new GameScoreChecker(), "Счёт 6:7"),
    GAME_SCORE_7_0((short) 1291, new GameScoreChecker(), "Счёт 7:0"),
    GAME_SCORE_7_1((short) 1292, new GameScoreChecker(), "Счёт 7:1"),
    GAME_SCORE_7_2((short) 1293, new GameScoreChecker(), "Счёт 7:2"),
    GAME_SCORE_7_3((short) 1294, new GameScoreChecker(), "Счёт 7:3"),
    GAME_SCORE_7_4((short) 1295, new GameScoreChecker(), "Счёт 7:4"),
    GAME_SCORE_7_5((short) 1296, new GameScoreChecker(), "Счёт 7:5"),
    GAME_SCORE_7_6((short) 1297, new GameScoreChecker(), "Счёт 7:6"),
    GAME_SCORE_7_7((short) 1298, new GameScoreChecker(), "Счёт 7:7"),

    //========== 1301-1350. Голы (Goals) ==========
    BOTH_TEAMS_SCORE((short) 1301, new GoalsChecker(), "Обе забьют"),
    HOME_TEAM_SCORES((short) 1302, new GoalsChecker(), "Хозяева забьют"),
    AWAY_TEAM_SCORES((short) 1303, new GoalsChecker(), "Гости забьют"),

    //========== 1351-1400. Голы по таймам (Goals by halftimes) ==========
    HOME_SCORES_1ST_HALF((short) 1351, new GoalsChecker(), "Хозяева забьют в 1 тайме"),
    HOME_SCORES_2ND_HALF((short) 1352, new GoalsChecker(), "Хозяева забьют во 2 тайме"),
    AWAY_SCORES_1ST_HALF((short) 1353, new GoalsChecker(), "Гости забьют в 1 тайме"),
    AWAY_SCORES_2ND_HALF((short) 1354, new GoalsChecker(), "Гости забьют во 2 тайме"),
    HOME_SCORES_BOTH_HALVES((short) 1355, new GoalsChecker(), "Хозяева забьют в обоих таймах"),
    AWAY_SCORES_BOTH_HALVES((short) 1356, new GoalsChecker(), "Гости забьют в обоих таймах"),
    BOTH_TEAMS_SCORE_1ST_HALF((short) 1357, new GoalsChecker(), "Обе забьют в 1 тайме"),
    BOTH_TEAMS_SCORE_2ND_HALF((short) 1358, new GoalsChecker(), "Обе забьют во 2 тайме"),
    BOTH_TEAMS_SCORE_BOTH_HALVES((short) 1359, new GoalsChecker(), "Обе забьют в обоих таймах"),
    GOALS_IN_BOTH_HALVES((short) 1360, new GoalsChecker(), "Голы в обоих таймах"),

    //========== 1401-1450. Результат матча + Обе забьют (Game result + Both team score) ==========
    HOME_WIN_AND_BOTH_TEAMS_SCORE((short) 1401, new GoalsChecker(), "П1 + Обе забьют"),
    DRAW_AND_BOTH_TEAMS_SCORE((short) 1402, new GoalsChecker(), "Х + Обе забьют"),
    AWAY_WIN_AND_BOTH_TEAMS_SCORE((short) 1403, new GoalsChecker(), "П2 + Обе забьют"),
    HOME_OR_DRAW_AND_BOTH_TEAMS_SCORE((short) 1404, new GoalsChecker(), "1Х + Обе забьют"),
    HOME_OR_AWAY_AND_BOTH_TEAMS_SCORE((short) 1405, new GoalsChecker(), "12 + Обе забьют"),
    AWAY_OR_DRAW_AND_BOTH_TEAMS_SCORE((short) 1406, new GoalsChecker(), "Х2 + Обе забьют"),

    //========== 1451-1500. Любая забьет больше чем (Scores More Than) ==========
    ANY_TEAM_SCORES_2_OR_MORE((short) 1451, new GoalsChecker(), "Любая команда забьет 2 и больше голов"),
    ANY_TEAM_SCORES_3_OR_MORE((short) 1452, new GoalsChecker(), "Любая команда забьет 3 и больше голов"),
    ANY_TEAM_SCORES_4_OR_MORE((short) 1453, new GoalsChecker(), "Любая команда забьет 4 и больше голов"),
    ANY_TEAM_SCORES_5_OR_MORE((short) 1454, new GoalsChecker(), "Любая команда забьет 5 и больше голов"),

    //========== 1501-1550. Обе забьют + тотал меньше (Both Team Score + Goals Amount (Under)) ==========
    BOTH_TEAMS_SCORE_AND_UNDER_1_5((short) 1501, new GoalsChecker(), "ОЗ +  ТМ 1,5"),
    BOTH_TEAMS_SCORE_AND_UNDER_2_0((short) 1502, new GoalsChecker(), "ОЗ +  ТМ 2"),
    BOTH_TEAMS_SCORE_AND_UNDER_2_5((short) 1503, new GoalsChecker(), "ОЗ +  ТМ 2,5"),
    BOTH_TEAMS_SCORE_AND_UNDER_3_0((short) 1504, new GoalsChecker(), "ОЗ +  ТМ 3"),
    BOTH_TEAMS_SCORE_AND_UNDER_3_5((short) 1505, new GoalsChecker(), "ОЗ +  ТМ 3,5"),
    BOTH_TEAMS_SCORE_AND_UNDER_4_0((short) 1506, new GoalsChecker(), "ОЗ +  ТМ 4"),
    BOTH_TEAMS_SCORE_AND_UNDER_4_5((short) 1507, new GoalsChecker(), "ОЗ +  ТМ 4,5"),
    BOTH_TEAMS_SCORE_AND_UNDER_5_0((short) 1508, new GoalsChecker(), "ОЗ +  ТМ 5"),
    BOTH_TEAMS_SCORE_AND_UNDER_5_5((short) 1509, new GoalsChecker(), "ОЗ +  ТМ 5,5"),
    BOTH_TEAMS_SCORE_AND_UNDER_6_0((short) 1510, new GoalsChecker(), "ОЗ +  ТМ 6"),
    BOTH_TEAMS_SCORE_AND_UNDER_6_5((short) 1511, new GoalsChecker(), "ОЗ +  ТМ 6,5"),

    //========== 1551-1600. Обе забьют + тотал больше (Both Team Score + Goals Amount (Over)) ==========
    BOTH_TEAMS_SCORE_AND_OVER_1_5((short) 1551, new GoalsChecker(), "ОЗ +  ТБ 1,5"),
    BOTH_TEAMS_SCORE_AND_OVER_2_0((short) 1552, new GoalsChecker(), "ОЗ +  ТБ 2"),
    BOTH_TEAMS_SCORE_AND_OVER_2_5((short) 1553, new GoalsChecker(), "ОЗ +  ТБ 2,5"),
    BOTH_TEAMS_SCORE_AND_OVER_3_0((short) 1554, new GoalsChecker(), "ОЗ +  ТБ 3"),
    BOTH_TEAMS_SCORE_AND_OVER_3_5((short) 1555, new GoalsChecker(), "ОЗ +  ТБ 3,5"),
    BOTH_TEAMS_SCORE_AND_OVER_4_0((short) 1556, new GoalsChecker(), "ОЗ +  ТБ 4"),
    BOTH_TEAMS_SCORE_AND_OVER_4_5((short) 1557, new GoalsChecker(), "ОЗ +  ТБ 4,5"),
    BOTH_TEAMS_SCORE_AND_OVER_5_0((short) 1558, new GoalsChecker(), "ОЗ +  ТБ 5"),
    BOTH_TEAMS_SCORE_AND_OVER_5_5((short) 1559, new GoalsChecker(), "ОЗ +  ТБ 5,5"),
    BOTH_TEAMS_SCORE_AND_OVER_6_0((short) 1560, new GoalsChecker(), "ОЗ +  ТБ 6"),
    BOTH_TEAMS_SCORE_AND_OVER_6_5((short) 1561, new GoalsChecker(), "ОЗ +  ТБ 6,5"),

    // =================================================================================================
    // Ставки на таймы являются отдельной крупной группой,
    // поэтому под них выделен особый диапазон кодов, начиная с 2000
    // =================================================================================================

    //========== 2001-2100. Результаты по таймам (Half-time Results) ==========
    FIRST_HALF_HOME_WIN((short) 2001, new GameResultChecker(), "1й тайм: П1"),
    FIRST_HALF_DRAW((short) 2002, new GameResultChecker(), "1й тайм: Х"),
    FIRST_HALF_AWAY_WIN((short) 2003, new GameResultChecker(), "1й тайм: П2"),
    FIRST_HALF_HOME_WIN_OR_DRAW((short) 2004, new GameResultChecker(), "1й тайм: 1Х"),
    FIRST_HALF_HOME_OR_AWAY_WIN((short) 2005, new GameResultChecker(), "1й тайм: 12"),
    FIRST_HALF_AWAY_WIN_OR_DRAW((short) 2006, new GameResultChecker(), "1й тайм: Х2"),
    SECOND_HALF_HOME_WIN((short) 2007, new GameResultChecker(), "2й тайм: П1"),
    SECOND_HALF_DRAW((short) 2008, new GameResultChecker(), "2й тайм: Х"),
    SECOND_HALF_AWAY_WIN((short) 2009, new GameResultChecker(), "2й тайм: П2"),
    SECOND_HALF_HOME_WIN_OR_DRAW((short) 2010, new GameResultChecker(), "2й тайм: 1Х"),
    SECOND_HALF_HOME_OR_AWAY_WIN((short) 2011, new GameResultChecker(), "2й тайм: 12"),
    SECOND_HALF_AWAY_WIN_OR_DRAW((short) 2012, new GameResultChecker(), "2й тайм: Х2"),
    ANY_HALF_HOME_WIN((short) 2013, new GameResultChecker(), "Любой тайм: П1"),
    ANY_HALF_DRAW((short) 2014, new GameResultChecker(), "Любой тайм: Х"),
    ANY_HALF_AWAY_WIN((short) 2015, new GameResultChecker(), "Любой тайм: П2"),

    //========== 2101-2150. Тайм/Матч (Half Time / Full Time) ==========
    HALF_FULL_HOME_HOME((short) 2101, new GameResultChecker(), "Тайм/Матч: П1 / П1"),
    HALF_FULL_HOME_DRAW((short) 2102, new GameResultChecker(), "Тайм/Матч: П1 / Х"),
    HALF_FULL_HOME_AWAY((short) 2103, new GameResultChecker(), "Тайм/Матч: П1 / П2"),
    HALF_FULL_DRAW_HOME((short) 2104, new GameResultChecker(), "Тайм/Матч: Х / П1"),
    HALF_FULL_DRAW_DRAW((short) 2105, new GameResultChecker(), "Тайм/Матч: Х / Х"),
    HALF_FULL_DRAW_AWAY((short) 2106, new GameResultChecker(), "Тайм/Матч: Х / П2"),
    HALF_FULL_AWAY_HOME((short) 2107, new GameResultChecker(), "Тайм/Матч: П2 / П1"),
    HALF_FULL_AWAY_DRAW((short) 2108, new GameResultChecker(), "Тайм/Матч: П2 / Х"),
    HALF_FULL_AWAY_AWAY((short) 2109, new GameResultChecker(), "Тайм/Матч: П2 / П2"),

    //========== 2151-2200. 1й/2й Тайм (1st Half / 2nd Half) ==========
    FIRST_SECOND_HOME_HOME((short) 2151, new GameResultChecker(), "1й/2й тайм: П1 / П1"),
    FIRST_SECOND_HOME_DRAW((short) 2152, new GameResultChecker(), "1й/2й тайм: П1 / Х"),
    FIRST_SECOND_HOME_AWAY((short) 2153, new GameResultChecker(), "1й/2й тайм: П1 / П2"),
    FIRST_SECOND_DRAW_HOME((short) 2154, new GameResultChecker(), "1й/2й тайм: Х / П1"),
    FIRST_SECOND_DRAW_DRAW((short) 2155, new GameResultChecker(), "1й/2й тайм: Х / Х"),
    FIRST_SECOND_DRAW_AWAY((short) 2156, new GameResultChecker(), "1й/2й тайм: Х / П2"),
    FIRST_SECOND_AWAY_HOME((short) 2157, new GameResultChecker(), "1й/2й тайм: П2 / П1"),
    FIRST_SECOND_AWAY_DRAW((short) 2158, new GameResultChecker(), "1й/2й тайм: П2 / Х"),
    FIRST_SECOND_AWAY_AWAY((short) 2159, new GameResultChecker(), "1й/2й тайм: П2 / П2"),

    //========== 2201-2250. Счёт 1-го тайма (1st Half Score) ==========
    FIRST_HALF_SCORE_0_0((short) 2201, new GameScoreChecker(), "1й тайм: 0:0"),
    FIRST_HALF_SCORE_1_0((short) 2202, new GameScoreChecker(), "1й тайм: 1:0"),
    FIRST_HALF_SCORE_2_0((short) 2203, new GameScoreChecker(), "1й тайм: 2:0"),
    FIRST_HALF_SCORE_3_0((short) 2204, new GameScoreChecker(), "1й тайм: 3:0"),
    FIRST_HALF_SCORE_0_1((short) 2205, new GameScoreChecker(), "1й тайм: 0:1"),
    FIRST_HALF_SCORE_1_1((short) 2206, new GameScoreChecker(), "1й тайм: 1:1"),
    FIRST_HALF_SCORE_2_1((short) 2207, new GameScoreChecker(), "1й тайм: 2:1"),
    FIRST_HALF_SCORE_3_1((short) 2208, new GameScoreChecker(), "1й тайм: 3:1"),
    FIRST_HALF_SCORE_0_2((short) 2209, new GameScoreChecker(), "1й тайм: 0:2"),
    FIRST_HALF_SCORE_1_2((short) 2210, new GameScoreChecker(), "1й тайм: 1:2"),
    FIRST_HALF_SCORE_2_2((short) 2211, new GameScoreChecker(), "1й тайм: 2:2"),
    FIRST_HALF_SCORE_3_2((short) 2212, new GameScoreChecker(), "1й тайм: 3:2"),
    FIRST_HALF_SCORE_0_3((short) 2213, new GameScoreChecker(), "1й тайм: 0:3"),
    FIRST_HALF_SCORE_1_3((short) 2214, new GameScoreChecker(), "1й тайм: 1:3"),
    FIRST_HALF_SCORE_2_3((short) 2215, new GameScoreChecker(), "1й тайм: 2:3"),
    FIRST_HALF_SCORE_3_3((short) 2216, new GameScoreChecker(), "1й тайм: 3:3"),

    //========== 2251-2300. Счёт 2-го тайма (2nd Half Score) ==========
    SECOND_HALF_SCORE_0_0((short) 2251, new GameScoreChecker(), "2й тайм: 0:0"),
    SECOND_HALF_SCORE_1_0((short) 2252, new GameScoreChecker(), "2й тайм: 1:0"),
    SECOND_HALF_SCORE_2_0((short) 2253, new GameScoreChecker(), "2й тайм: 2:0"),
    SECOND_HALF_SCORE_3_0((short) 2254, new GameScoreChecker(), "2й тайм: 3:0"),
    SECOND_HALF_SCORE_0_1((short) 2255, new GameScoreChecker(), "2й тайм: 0:1"),
    SECOND_HALF_SCORE_1_1((short) 2256, new GameScoreChecker(), "2й тайм: 1:1"),
    SECOND_HALF_SCORE_2_1((short) 2257, new GameScoreChecker(), "2й тайм: 2:1"),
    SECOND_HALF_SCORE_3_1((short) 2258, new GameScoreChecker(), "2й тайм: 3:1"),
    SECOND_HALF_SCORE_0_2((short) 2259, new GameScoreChecker(), "2й тайм: 0:2"),
    SECOND_HALF_SCORE_1_2((short) 2260, new GameScoreChecker(), "2й тайм: 1:2"),
    SECOND_HALF_SCORE_2_2((short) 2261, new GameScoreChecker(), "2й тайм: 2:2"),
    SECOND_HALF_SCORE_3_2((short) 2262, new GameScoreChecker(), "2й тайм: 3:2"),
    SECOND_HALF_SCORE_0_3((short) 2263, new GameScoreChecker(), "2й тайм: 0:3"),
    SECOND_HALF_SCORE_1_3((short) 2264, new GameScoreChecker(), "2й тайм: 1:3"),
    SECOND_HALF_SCORE_2_3((short) 2265, new GameScoreChecker(), "2й тайм: 2:3"),
    SECOND_HALF_SCORE_3_3((short) 2266, new GameScoreChecker(), "2й тайм: 3:3"),

    //========== 2301-2400. Обе забьют + исход по таймам (Both Teams to Score + Half-time Result) ==========
    FIRST_HALF_BOTH_SCORE_AND_HOME_WIN((short) 2301, new GoalsChecker(), "1й тайм: ОЗ + П1"),
    FIRST_HALF_BOTH_SCORE_AND_DRAW((short) 2302, new GoalsChecker(), "1й тайм: ОЗ + Х"),
    FIRST_HALF_BOTH_SCORE_AND_AWAY_WIN((short) 2303, new GoalsChecker(), "1й тайм: ОЗ + П2"),
    FIRST_HALF_BOTH_SCORE_AND_HOME_OR_DRAW((short) 2304, new GoalsChecker(), "1й тайм: ОЗ + 1Х"),
    FIRST_HALF_BOTH_SCORE_AND_HOME_OR_AWAY((short) 2305, new GoalsChecker(), "1й тайм: ОЗ + 12"),
    FIRST_HALF_BOTH_SCORE_AND_AWAY_OR_DRAW((short) 2306, new GoalsChecker(), "1й тайм: ОЗ + Х2"),

    SECOND_HALF_BOTH_SCORE_AND_HOME_WIN((short) 2351, new GoalsChecker(), "2й тайм: ОЗ + П1"),
    SECOND_HALF_BOTH_SCORE_AND_DRAW((short) 2352, new GoalsChecker(), "2й тайм: ОЗ + Х"),
    SECOND_HALF_BOTH_SCORE_AND_AWAY_WIN((short) 2353, new GoalsChecker(), "2й тайм: ОЗ + П2"),
    SECOND_HALF_BOTH_SCORE_AND_HOME_OR_DRAW((short) 2354, new GoalsChecker(), "2й тайм: ОЗ + 1Х"),
    SECOND_HALF_BOTH_SCORE_AND_HOME_OR_AWAY((short) 2355, new GoalsChecker(), "2й тайм: ОЗ + 12"),
    SECOND_HALF_BOTH_SCORE_AND_AWAY_OR_DRAW((short) 2356, new GoalsChecker(), "2й тайм: ОЗ + Х2"),

    //========== 2401-2500. Фора 1й тайм (First Half Handicap) ==========
    FIRST_HALF_HANDICAP_HOME_0((short) 2401, new HandicapChecker(), "1й тайм: Ф1(0)"),
    FIRST_HALF_HANDICAP_HOME_MINUS_1_0((short) 2402, new HandicapChecker(), "1й тайм: Ф1(-1)"),
    FIRST_HALF_HANDICAP_HOME_PLUS_1_0((short) 2403, new HandicapChecker(), "1й тайм: Ф1(+1)"),
    FIRST_HALF_HANDICAP_HOME_MINUS_1_5((short) 2404, new HandicapChecker(), "1й тайм: Ф1(-1,5)"),
    FIRST_HALF_HANDICAP_HOME_PLUS_1_5((short) 2405, new HandicapChecker(), "1й тайм: Ф1(+1,5)"),
    FIRST_HALF_HANDICAP_HOME_MINUS_2_0((short) 2406, new HandicapChecker(), "1й тайм: Ф1(-2)"),
    FIRST_HALF_HANDICAP_HOME_PLUS_2_0((short) 2407, new HandicapChecker(), "1й тайм: Ф1(+2)"),
    FIRST_HALF_HANDICAP_HOME_MINUS_2_5((short) 2408, new HandicapChecker(), "1й тайм: Ф1(-2,5)"),
    FIRST_HALF_HANDICAP_HOME_PLUS_2_5((short) 2409, new HandicapChecker(), "1й тайм: Ф1(+2,5)"),
    FIRST_HALF_HANDICAP_HOME_MINUS_3_0((short) 2410, new HandicapChecker(), "1й тайм: Ф1(-3)"),
    FIRST_HALF_HANDICAP_HOME_PLUS_3_0((short) 2411, new HandicapChecker(), "1й тайм: Ф1(+3)"),
    FIRST_HALF_HANDICAP_HOME_MINUS_3_5((short) 2412, new HandicapChecker(), "1й тайм: Ф1(-3,5)"),
    FIRST_HALF_HANDICAP_HOME_PLUS_3_5((short) 2413, new HandicapChecker(), "1й тайм: Ф1(+3,5)"),

    FIRST_HALF_HANDICAP_AWAY_0((short) 2451, new HandicapChecker(), "1й тайм: Ф2(0)"),
    FIRST_HALF_HANDICAP_AWAY_MINUS_1_0((short) 2452, new HandicapChecker(), "1й тайм: Ф2(-1)"),
    FIRST_HALF_HANDICAP_AWAY_PLUS_1_0((short) 2453, new HandicapChecker(), "1й тайм: Ф2(+1)"),
    FIRST_HALF_HANDICAP_AWAY_MINUS_1_5((short) 2454, new HandicapChecker(), "1й тайм: Ф2(-1,5)"),
    FIRST_HALF_HANDICAP_AWAY_PLUS_1_5((short) 2455, new HandicapChecker(), "1й тайм: Ф2(+1,5)"),
    FIRST_HALF_HANDICAP_AWAY_MINUS_2_0((short) 2456, new HandicapChecker(), "1й тайм: Ф2(-2)"),
    FIRST_HALF_HANDICAP_AWAY_PLUS_2_0((short) 2457, new HandicapChecker(), "1й тайм: Ф2(+2)"),
    FIRST_HALF_HANDICAP_AWAY_MINUS_2_5((short) 2458, new HandicapChecker(), "1й тайм: Ф2(-2,5)"),
    FIRST_HALF_HANDICAP_AWAY_PLUS_2_5((short) 2459, new HandicapChecker(), "1й тайм: Ф2(+2,5)"),
    FIRST_HALF_HANDICAP_AWAY_MINUS_3_0((short) 2460, new HandicapChecker(), "1й тайм: Ф2(-3)"),
    FIRST_HALF_HANDICAP_AWAY_PLUS_3_0((short) 2461, new HandicapChecker(), "1й тайм: Ф2(+3)"),
    FIRST_HALF_HANDICAP_AWAY_MINUS_3_5((short) 2462, new HandicapChecker(), "1й тайм: Ф2(-3,5)"),
    FIRST_HALF_HANDICAP_AWAY_PLUS_3_5((short) 2463, new HandicapChecker(), "1й тайм: Ф2(+3,5)"),

    //========== 2501-2600. Фора 2й тайм (Second Half Handicap) ==========
    SECOND_HALF_HANDICAP_HOME_0((short) 2501, new HandicapChecker(), "2й тайм: Ф1(0)"),
    SECOND_HALF_HANDICAP_HOME_MINUS_1_0((short) 2502, new HandicapChecker(), "2й тайм: Ф1(-1)"),
    SECOND_HALF_HANDICAP_HOME_PLUS_1_0((short) 2503, new HandicapChecker(), "2й тайм: Ф1(+1)"),
    SECOND_HALF_HANDICAP_HOME_MINUS_1_5((short) 2504, new HandicapChecker(), "2й тайм: Ф1(-1,5)"),
    SECOND_HALF_HANDICAP_HOME_PLUS_1_5((short) 2505, new HandicapChecker(), "2й тайм: Ф1(+1,5)"),
    SECOND_HALF_HANDICAP_HOME_MINUS_2_0((short) 2506, new HandicapChecker(), "2й тайм: Ф1(-2)"),
    SECOND_HALF_HANDICAP_HOME_PLUS_2_0((short) 2507, new HandicapChecker(), "2й тайм: Ф1(+2)"),
    SECOND_HALF_HANDICAP_HOME_MINUS_2_5((short) 2508, new HandicapChecker(), "2й тайм: Ф1(-2,5)"),
    SECOND_HALF_HANDICAP_HOME_PLUS_2_5((short) 2509, new HandicapChecker(), "2й тайм: Ф1(+2,5)"),
    SECOND_HALF_HANDICAP_HOME_MINUS_3_0((short) 2510, new HandicapChecker(), "2й тайм: Ф1(-3)"),
    SECOND_HALF_HANDICAP_HOME_PLUS_3_0((short) 2511, new HandicapChecker(), "2й тайм: Ф1(+3)"),
    SECOND_HALF_HANDICAP_HOME_MINUS_3_5((short) 2512, new HandicapChecker(), "2й тайм: Ф1(-3,5)"),
    SECOND_HALF_HANDICAP_HOME_PLUS_3_5((short) 2513, new HandicapChecker(), "2й тайм: Ф1(+3,5)"),

    SECOND_HALF_HANDICAP_AWAY_0((short) 2551, new HandicapChecker(), "2й тайм: Ф2(0)"),
    SECOND_HALF_HANDICAP_AWAY_MINUS_1_0((short) 2552, new HandicapChecker(), "2й тайм: Ф2(-1)"),
    SECOND_HALF_HANDICAP_AWAY_PLUS_1_0((short) 2553, new HandicapChecker(), "2й тайм: Ф2(+1)"),
    SECOND_HALF_HANDICAP_AWAY_MINUS_1_5((short) 2554, new HandicapChecker(), "2й тайм: Ф2(-1,5)"),
    SECOND_HALF_HANDICAP_AWAY_PLUS_1_5((short) 2555, new HandicapChecker(), "2й тайм: Ф2(+1,5)"),
    SECOND_HALF_HANDICAP_AWAY_MINUS_2_0((short) 2556, new HandicapChecker(), "2й тайм: Ф2(-2)"),
    SECOND_HALF_HANDICAP_AWAY_PLUS_2_0((short) 2557, new HandicapChecker(), "2й тайм: Ф2(+2)"),
    SECOND_HALF_HANDICAP_AWAY_MINUS_2_5((short) 2558, new HandicapChecker(), "2й тайм: Ф2(-2,5)"),
    SECOND_HALF_HANDICAP_AWAY_PLUS_2_5((short) 2559, new HandicapChecker(), "2й тайм: Ф2(+2,5)"),
    SECOND_HALF_HANDICAP_AWAY_MINUS_3_0((short) 2560, new HandicapChecker(), "2й тайм: Ф2(-3)"),
    SECOND_HALF_HANDICAP_AWAY_PLUS_3_0((short) 2561, new HandicapChecker(), "2й тайм: Ф2(+3)"),
    SECOND_HALF_HANDICAP_AWAY_MINUS_3_5((short) 2562, new HandicapChecker(), "2й тайм: Ф2(-3,5)"),
    SECOND_HALF_HANDICAP_AWAY_PLUS_3_5((short) 2563, new HandicapChecker(), "2й тайм: Ф2(+3,5)"),

    //========== 2601-2700. Тотал голов 1й тайм (First Half Goals Amount) ==========
    FIRST_HALF_TOTAL_UNDER_0_5((short) 2601, new TotalChecker(), "1й тайм: ТМ 0,5"),
    FIRST_HALF_TOTAL_UNDER_1_0((short) 2602, new TotalChecker(), "1й тайм: ТМ 1"),
    FIRST_HALF_TOTAL_UNDER_1_5((short) 2603, new TotalChecker(), "1й тайм: ТМ 1,5"),
    FIRST_HALF_TOTAL_UNDER_2_0((short) 2604, new TotalChecker(), "1й тайм: ТМ 2"),
    FIRST_HALF_TOTAL_UNDER_2_5((short) 2605, new TotalChecker(), "1й тайм: ТМ 2,5"),
    FIRST_HALF_TOTAL_UNDER_3_0((short) 2606, new TotalChecker(), "1й тайм: ТМ 3"),
    FIRST_HALF_TOTAL_UNDER_3_5((short) 2607, new TotalChecker(), "1й тайм: ТМ 3,5"),
    FIRST_HALF_TOTAL_UNDER_4_0((short) 2608, new TotalChecker(), "1й тайм: ТМ 4"),
    FIRST_HALF_TOTAL_UNDER_4_5((short) 2609, new TotalChecker(), "1й тайм: ТМ 4,5"),

    FIRST_HALF_TOTAL_OVER_0_5((short) 2651, new TotalChecker(), "1й тайм: ТБ 0,5"),
    FIRST_HALF_TOTAL_OVER_1_0((short) 2652, new TotalChecker(), "1й тайм: ТБ 1"),
    FIRST_HALF_TOTAL_OVER_1_5((short) 2653, new TotalChecker(), "1й тайм: ТБ 1,5"),
    FIRST_HALF_TOTAL_OVER_2_0((short) 2654, new TotalChecker(), "1й тайм: ТБ 2"),
    FIRST_HALF_TOTAL_OVER_2_5((short) 2655, new TotalChecker(), "1й тайм: ТБ 2,5"),
    FIRST_HALF_TOTAL_OVER_3_0((short) 2656, new TotalChecker(), "1й тайм: ТБ 3"),
    FIRST_HALF_TOTAL_OVER_3_5((short) 2657, new TotalChecker(), "1й тайм: ТБ 3,5"),
    FIRST_HALF_TOTAL_OVER_4_0((short) 2658, new TotalChecker(), "1й тайм: ТБ 4"),
    FIRST_HALF_TOTAL_OVER_4_5((short) 2659, new TotalChecker(), "1й тайм: ТБ 4,5"),

    //========== 2701-2800. Тотал голов 2й тайм (Second Half Goals Amount) ==========
    SECOND_HALF_TOTAL_UNDER_0_5((short) 2701, new TotalChecker(), "2й тайм: ТМ 0,5"),
    SECOND_HALF_TOTAL_UNDER_1_0((short) 2702, new TotalChecker(), "2й тайм: ТМ 1"),
    SECOND_HALF_TOTAL_UNDER_1_5((short) 2703, new TotalChecker(), "2й тайм: ТМ 1,5"),
    SECOND_HALF_TOTAL_UNDER_2_0((short) 2704, new TotalChecker(), "2й тайм: ТМ 2"),
    SECOND_HALF_TOTAL_UNDER_2_5((short) 2705, new TotalChecker(), "2й тайм: ТМ 2,5"),
    SECOND_HALF_TOTAL_UNDER_3_0((short) 2706, new TotalChecker(), "2й тайм: ТМ 3"),
    SECOND_HALF_TOTAL_UNDER_3_5((short) 2707, new TotalChecker(), "2й тайм: ТМ 3,5"),
    SECOND_HALF_TOTAL_UNDER_4_0((short) 2708, new TotalChecker(), "2й тайм: ТМ 4"),
    SECOND_HALF_TOTAL_UNDER_4_5((short) 2709, new TotalChecker(), "2й тайм: ТМ 4,5"),

    SECOND_HALF_TOTAL_OVER_0_5((short) 2751, new TotalChecker(), "2й тайм: ТБ 0,5"),
    SECOND_HALF_TOTAL_OVER_1_0((short) 2752, new TotalChecker(), "2й тайм: ТБ 1"),
    SECOND_HALF_TOTAL_OVER_1_5((short) 2753, new TotalChecker(), "2й тайм: ТБ 1,5"),
    SECOND_HALF_TOTAL_OVER_2_0((short) 2754, new TotalChecker(), "2й тайм: ТБ 2"),
    SECOND_HALF_TOTAL_OVER_2_5((short) 2755, new TotalChecker(), "2й тайм: ТБ 2,5"),
    SECOND_HALF_TOTAL_OVER_3_0((short) 2756, new TotalChecker(), "2й тайм: ТБ 3"),
    SECOND_HALF_TOTAL_OVER_3_5((short) 2757, new TotalChecker(), "2й тайм: ТБ 3,5"),
    SECOND_HALF_TOTAL_OVER_4_0((short) 2758, new TotalChecker(), "2й тайм: ТБ 4"),
    SECOND_HALF_TOTAL_OVER_4_5((short) 2759, new TotalChecker(), "2й тайм: ТБ 4,5"),

    //========== 2801-2900. ИТ Хозяев 1й тайм (First Half Home Team Goals Amount) ==========
    FIRST_HALF_HOME_TEAM_TOTAL_UNDER_0_5((short) 2801, new TotalChecker(), "1й тайм: Хозяева ИТМ 0,5"),
    FIRST_HALF_HOME_TEAM_TOTAL_UNDER_1_0((short) 2802, new TotalChecker(), "1й тайм: Хозяева ИТМ 1"),
    FIRST_HALF_HOME_TEAM_TOTAL_UNDER_1_5((short) 2803, new TotalChecker(), "1й тайм: Хозяева ИТМ 1,5"),
    FIRST_HALF_HOME_TEAM_TOTAL_UNDER_2_0((short) 2804, new TotalChecker(), "1й тайм: Хозяева ИТМ 2"),
    FIRST_HALF_HOME_TEAM_TOTAL_UNDER_2_5((short) 2805, new TotalChecker(), "1й тайм: Хозяева ИТМ 2,5"),
    FIRST_HALF_HOME_TEAM_TOTAL_UNDER_3_0((short) 2806, new TotalChecker(), "1й тайм: Хозяева ИТМ 3"),

    FIRST_HALF_HOME_TEAM_TOTAL_OVER_0_5((short) 2851, new TotalChecker(), "1й тайм: Хозяева ИТБ 0,5"),
    FIRST_HALF_HOME_TEAM_TOTAL_OVER_1_0((short) 2852, new TotalChecker(), "1й тайм: Хозяева ИТБ 1"),
    FIRST_HALF_HOME_TEAM_TOTAL_OVER_1_5((short) 2853, new TotalChecker(), "1й тайм: Хозяева ИТБ 1,5"),
    FIRST_HALF_HOME_TEAM_TOTAL_OVER_2_0((short) 2854, new TotalChecker(), "1й тайм: Хозяева ИТБ 2"),
    FIRST_HALF_HOME_TEAM_TOTAL_OVER_2_5((short) 2855, new TotalChecker(), "1й тайм: Хозяева ИТБ 2,5"),
    FIRST_HALF_HOME_TEAM_TOTAL_OVER_3_0((short) 2856, new TotalChecker(), "1й тайм: Хозяева ИТБ 3"),

    //========== 2901-3000. ИТ Хозяев 2й тайм (Second Half Home Team Goals Amount) ==========
    SECOND_HALF_HOME_TEAM_TOTAL_UNDER_0_5((short) 2901, new TotalChecker(), "2й тайм: Хозяева ИТМ 0,5"),
    SECOND_HALF_HOME_TEAM_TOTAL_UNDER_1_0((short) 2902, new TotalChecker(), "2й тайм: Хозяева ИТМ 1"),
    SECOND_HALF_HOME_TEAM_TOTAL_UNDER_1_5((short) 2903, new TotalChecker(), "2й тайм: Хозяева ИТМ 1,5"),
    SECOND_HALF_HOME_TEAM_TOTAL_UNDER_2_0((short) 2904, new TotalChecker(), "2й тайм: Хозяева ИТМ 2"),
    SECOND_HALF_HOME_TEAM_TOTAL_UNDER_2_5((short) 2905, new TotalChecker(), "2й тайм: Хозяева ИТМ 2,5"),
    SECOND_HALF_HOME_TEAM_TOTAL_UNDER_3_0((short) 2906, new TotalChecker(), "2й тайм: Хозяева ИТМ 3"),

    SECOND_HALF_HOME_TEAM_TOTAL_OVER_0_5((short) 2951, new TotalChecker(), "2й тайм: Хозяева ИТБ 0,5"),
    SECOND_HALF_HOME_TEAM_TOTAL_OVER_1_0((short) 2952, new TotalChecker(), "2й тайм: Хозяева ИТБ 1"),
    SECOND_HALF_HOME_TEAM_TOTAL_OVER_1_5((short) 2953, new TotalChecker(), "2й тайм: Хозяева ИТБ 1,5"),
    SECOND_HALF_HOME_TEAM_TOTAL_OVER_2_0((short) 2954, new TotalChecker(), "2й тайм: Хозяева ИТБ 2"),
    SECOND_HALF_HOME_TEAM_TOTAL_OVER_2_5((short) 2955, new TotalChecker(), "2й тайм: Хозяева ИТБ 2,5"),
    SECOND_HALF_HOME_TEAM_TOTAL_OVER_3_0((short) 2956, new TotalChecker(), "2й тайм: Хозяева ИТБ 3"),

    //========== 3001-3100. ИТ Гостей 1й тайм (First Half Away Team Goals Amount) ==========
    FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_0_5((short) 3001, new TotalChecker(), "1й тайм: Гости ИТМ 0,5"),
    FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_1_0((short) 3002, new TotalChecker(), "1й тайм: Гости ИТМ 1"),
    FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_1_5((short) 3003, new TotalChecker(), "1й тайм: Гости ИТМ 1,5"),
    FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_2_0((short) 3004, new TotalChecker(), "1й тайм: Гости ИТМ 2"),
    FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_2_5((short) 3005, new TotalChecker(), "1й тайм: Гости ИТМ 2,5"),
    FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_3_0((short) 3006, new TotalChecker(), "1й тайм: Гости ИТМ 3"),

    FIRST_HALF_AWAY_TEAM_TOTAL_OVER_0_5((short) 3051, new TotalChecker(), "1й тайм: Гости ИТБ 0,5"),
    FIRST_HALF_AWAY_TEAM_TOTAL_OVER_1_0((short) 3052, new TotalChecker(), "1й тайм: Гости ИТБ 1"),
    FIRST_HALF_AWAY_TEAM_TOTAL_OVER_1_5((short) 3053, new TotalChecker(), "1й тайм: Гости ИТБ 1,5"),
    FIRST_HALF_AWAY_TEAM_TOTAL_OVER_2_0((short) 3054, new TotalChecker(), "1й тайм: Гости ИТБ 2"),
    FIRST_HALF_AWAY_TEAM_TOTAL_OVER_2_5((short) 3055, new TotalChecker(), "1й тайм: Гости ИТБ 2,5"),
    FIRST_HALF_AWAY_TEAM_TOTAL_OVER_3_0((short) 3056, new TotalChecker(), "1й тайм: Гости ИТБ 3"),

    //========== 3101-3200. ИТ Гостей 2й тайм (Second Half Away Team Goals Amount) ==========
    SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_0_5((short) 3101, new TotalChecker(), "2й тайм: Гости ИТМ 0,5"),
    SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_1_0((short) 3102, new TotalChecker(), "2й тайм: Гости ИТМ 1"),
    SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_1_5((short) 3103, new TotalChecker(), "2й тайм: Гости ИТМ 1,5"),
    SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_2_0((short) 3104, new TotalChecker(), "2й тайм: Гости ИТМ 2"),
    SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_2_5((short) 3105, new TotalChecker(), "2й тайм: Гости ИТМ 2,5"),
    SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_3_0((short) 3106, new TotalChecker(), "2й тайм: Гости ИТМ 3"),

    SECOND_HALF_AWAY_TEAM_TOTAL_OVER_0_5((short) 3151, new TotalChecker(), "2й тайм: Гости ИТБ 0,5"),
    SECOND_HALF_AWAY_TEAM_TOTAL_OVER_1_0((short) 3152, new TotalChecker(), "2й тайм: Гости ИТБ 1"),
    SECOND_HALF_AWAY_TEAM_TOTAL_OVER_1_5((short) 3153, new TotalChecker(), "2й тайм: Гости ИТБ 1,5"),
    SECOND_HALF_AWAY_TEAM_TOTAL_OVER_2_0((short) 3154, new TotalChecker(), "2й тайм: Гости ИТБ 2"),
    SECOND_HALF_AWAY_TEAM_TOTAL_OVER_2_5((short) 3155, new TotalChecker(), "2й тайм: Гости ИТБ 2,5"),
    SECOND_HALF_AWAY_TEAM_TOTAL_OVER_3_0((short) 3156, new TotalChecker(), "2й тайм: Гости ИТБ 3"),

    //========== 3201-3300. 1й тайм: Исход + Тотал Меньше (First Half Result + Total Under) ==========
    FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_1_5((short) 3201, new GameResultWithTotalChecker(), "1й тайм: П1 + ТМ 1,5"),
    FIRST_HALF_DRAW_AND_TOTAL_UNDER_1_5((short) 3202, new GameResultWithTotalChecker(), "1й тайм: Х + ТМ 1,5"),
    FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_1_5((short) 3203, new GameResultWithTotalChecker(), "1й тайм: П2 + ТМ 1,5"),
    FIRST_HALF_1X_AND_TOTAL_UNDER_1_5((short) 3204, new GameResultWithTotalChecker(), "1й тайм: 1Х + ТМ 1,5"),
    FIRST_HALF_12_AND_TOTAL_UNDER_1_5((short) 3205, new GameResultWithTotalChecker(), "1й тайм: 12 + ТМ 1,5"),
    FIRST_HALF_X2_AND_TOTAL_UNDER_1_5((short) 3206, new GameResultWithTotalChecker(), "1й тайм: Х2 + ТМ 1,5"),

    FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_2_0((short) 3211, new GameResultWithTotalChecker(), "1й тайм: П1 + ТМ 2"),
    FIRST_HALF_DRAW_AND_TOTAL_UNDER_2_0((short) 3212, new GameResultWithTotalChecker(), "1й тайм: Х + ТМ 2"),
    FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_2_0((short) 3213, new GameResultWithTotalChecker(), "1й тайм: П2 + ТМ 2"),
    FIRST_HALF_1X_AND_TOTAL_UNDER_2_0((short) 3214, new GameResultWithTotalChecker(), "1й тайм: 1Х + ТМ 2"),
    FIRST_HALF_12_AND_TOTAL_UNDER_2_0((short) 3215, new GameResultWithTotalChecker(), "1й тайм: 12 + ТМ 2"),
    FIRST_HALF_X2_AND_TOTAL_UNDER_2_0((short) 3216, new GameResultWithTotalChecker(), "1й тайм: Х2 + ТМ 2"),

    FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_2_5((short) 3221, new GameResultWithTotalChecker(), "1й тайм: П1 + ТМ 2,5"),
    FIRST_HALF_DRAW_AND_TOTAL_UNDER_2_5((short) 3222, new GameResultWithTotalChecker(), "1й тайм: Х + ТМ 2,5"),
    FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_2_5((short) 3223, new GameResultWithTotalChecker(), "1й тайм: П2 + ТМ 2,5"),
    FIRST_HALF_1X_AND_TOTAL_UNDER_2_5((short) 3224, new GameResultWithTotalChecker(), "1й тайм: 1Х + ТМ 2,5"),
    FIRST_HALF_12_AND_TOTAL_UNDER_2_5((short) 3225, new GameResultWithTotalChecker(), "1й тайм: 12 + ТМ 2,5"),
    FIRST_HALF_X2_AND_TOTAL_UNDER_2_5((short) 3226, new GameResultWithTotalChecker(), "1й тайм: Х2 + ТМ 2,5"),

    FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_3_0((short) 3231, new GameResultWithTotalChecker(), "1й тайм: П1 + ТМ 3"),
    FIRST_HALF_DRAW_AND_TOTAL_UNDER_3_0((short) 3232, new GameResultWithTotalChecker(), "1й тайм: Х + ТМ 3"),
    FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_3_0((short) 3233, new GameResultWithTotalChecker(), "1й тайм: П2 + ТМ 3"),
    FIRST_HALF_1X_AND_TOTAL_UNDER_3_0((short) 3234, new GameResultWithTotalChecker(), "1й тайм: 1Х + ТМ 3"),
    FIRST_HALF_12_AND_TOTAL_UNDER_3_0((short) 3235, new GameResultWithTotalChecker(), "1й тайм: 12 + ТМ 3"),
    FIRST_HALF_X2_AND_TOTAL_UNDER_3_0((short) 3236, new GameResultWithTotalChecker(), "1й тайм: Х2 + ТМ 3"),

    FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_3_5((short) 3241, new GameResultWithTotalChecker(), "1й тайм: П1 + ТМ 3,5"),
    FIRST_HALF_DRAW_AND_TOTAL_UNDER_3_5((short) 3242, new GameResultWithTotalChecker(), "1й тайм: Х + ТМ 3,5"),
    FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_3_5((short) 3243, new GameResultWithTotalChecker(), "1й тайм: П2 + ТМ 3,5"),
    FIRST_HALF_1X_AND_TOTAL_UNDER_3_5((short) 3244, new GameResultWithTotalChecker(), "1й тайм: 1Х + ТМ 3,5"),
    FIRST_HALF_12_AND_TOTAL_UNDER_3_5((short) 3245, new GameResultWithTotalChecker(), "1й тайм: 12 + ТМ 3,5"),
    FIRST_HALF_X2_AND_TOTAL_UNDER_3_5((short) 3246, new GameResultWithTotalChecker(), "1й тайм: Х2 + ТМ 3,5"),

    //========== 3301-3400. 1й тайм: Исход + Тотал Больше (First Half Result + Total Over) ==========
    FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_1_5((short) 3301, new GameResultWithTotalChecker(), "1й тайм: П1 + ТБ 1,5"),
    FIRST_HALF_DRAW_AND_TOTAL_OVER_1_5((short) 3302, new GameResultWithTotalChecker(), "1й тайм: Х + ТБ 1,5"),
    FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_1_5((short) 3303, new GameResultWithTotalChecker(), "1й тайм: П2 + ТБ 1,5"),
    FIRST_HALF_1X_AND_TOTAL_OVER_1_5((short) 3304, new GameResultWithTotalChecker(), "1й тайм: 1Х + ТБ 1,5"),
    FIRST_HALF_12_AND_TOTAL_OVER_1_5((short) 3305, new GameResultWithTotalChecker(), "1й тайм: 12 + ТБ 1,5"),
    FIRST_HALF_X2_AND_TOTAL_OVER_1_5((short) 3306, new GameResultWithTotalChecker(), "1й тайм: Х2 + ТБ 1,5"),

    FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_2_0((short) 3311, new GameResultWithTotalChecker(), "1й тайм: П1 + ТБ 2"),
    FIRST_HALF_DRAW_AND_TOTAL_OVER_2_0((short) 3312, new GameResultWithTotalChecker(), "1й тайм: Х + ТБ 2"),
    FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_2_0((short) 3313, new GameResultWithTotalChecker(), "1й тайм: П2 + ТБ 2"),
    FIRST_HALF_1X_AND_TOTAL_OVER_2_0((short) 3314, new GameResultWithTotalChecker(), "1й тайм: 1Х + ТБ 2"),
    FIRST_HALF_12_AND_TOTAL_OVER_2_0((short) 3315, new GameResultWithTotalChecker(), "1й тайм: 12 + ТБ 2"),
    FIRST_HALF_X2_AND_TOTAL_OVER_2_0((short) 3316, new GameResultWithTotalChecker(), "1й тайм: Х2 + ТБ 2"),

    FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_2_5((short) 3321, new GameResultWithTotalChecker(), "1й тайм: П1 + ТБ 2,5"),
    FIRST_HALF_DRAW_AND_TOTAL_OVER_2_5((short) 3322, new GameResultWithTotalChecker(), "1й тайм: Х + ТБ 2,5"),
    FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_2_5((short) 3323, new GameResultWithTotalChecker(), "1й тайм: П2 + ТБ 2,5"),
    FIRST_HALF_1X_AND_TOTAL_OVER_2_5((short) 3324, new GameResultWithTotalChecker(), "1й тайм: 1Х + ТБ 2,5"),
    FIRST_HALF_12_AND_TOTAL_OVER_2_5((short) 3325, new GameResultWithTotalChecker(), "1й тайм: 12 + ТБ 2,5"),
    FIRST_HALF_X2_AND_TOTAL_OVER_2_5((short) 3326, new GameResultWithTotalChecker(), "1й тайм: Х2 + ТБ 2,5"),

    FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_3_0((short) 3331, new GameResultWithTotalChecker(), "1й тайм: П1 + ТБ 3"),
    FIRST_HALF_DRAW_AND_TOTAL_OVER_3_0((short) 3332, new GameResultWithTotalChecker(), "1й тайм: Х + ТБ 3"),
    FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_3_0((short) 3333, new GameResultWithTotalChecker(), "1й тайм: П2 + ТБ 3"),
    FIRST_HALF_1X_AND_TOTAL_OVER_3_0((short) 3334, new GameResultWithTotalChecker(), "1й тайм: 1Х + ТБ 3"),
    FIRST_HALF_12_AND_TOTAL_OVER_3_0((short) 3335, new GameResultWithTotalChecker(), "1й тайм: 12 + ТБ 3"),
    FIRST_HALF_X2_AND_TOTAL_OVER_3_0((short) 3336, new GameResultWithTotalChecker(), "1й тайм: Х2 + ТБ 3"),

    FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_3_5((short) 3341, new GameResultWithTotalChecker(), "1й тайм: П1 + ТБ 3,5"),
    FIRST_HALF_DRAW_AND_TOTAL_OVER_3_5((short) 3342, new GameResultWithTotalChecker(), "1й тайм: Х + ТБ 3,5"),
    FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_3_5((short) 3343, new GameResultWithTotalChecker(), "1й тайм: П2 + ТБ 3,5"),
    FIRST_HALF_1X_AND_TOTAL_OVER_3_5((short) 3344, new GameResultWithTotalChecker(), "1й тайм: 1Х + ТБ 3,5"),
    FIRST_HALF_12_AND_TOTAL_OVER_3_5((short) 3345, new GameResultWithTotalChecker(), "1й тайм: 12 + ТБ 3,5"),
    FIRST_HALF_X2_AND_TOTAL_OVER_3_5((short) 3346, new GameResultWithTotalChecker(), "1й тайм: Х2 + ТБ 3,5"),

    //========== 3401-3500. 2й тайм: Исход + Тотал Меньше (Second Half Result + Total Under) ==========
    SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_1_5((short) 3401, new GameResultWithTotalChecker(), "2й тайм: П1 + ТМ 1,5"),
    SECOND_HALF_DRAW_AND_TOTAL_UNDER_1_5((short) 3402, new GameResultWithTotalChecker(), "2й тайм: Х + ТМ 1,5"),
    SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_1_5((short) 3403, new GameResultWithTotalChecker(), "2й тайм: П2 + ТМ 1,5"),
    SECOND_HALF_1X_AND_TOTAL_UNDER_1_5((short) 3404, new GameResultWithTotalChecker(), "2й тайм: 1Х + ТМ 1,5"),
    SECOND_HALF_12_AND_TOTAL_UNDER_1_5((short) 3405, new GameResultWithTotalChecker(), "2й тайм: 12 + ТМ 1,5"),
    SECOND_HALF_X2_AND_TOTAL_UNDER_1_5((short) 3406, new GameResultWithTotalChecker(), "2й тайм: Х2 + ТМ 1,5"),

    SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_2_0((short) 3411, new GameResultWithTotalChecker(), "2й тайм: П1 + ТМ 2"),
    SECOND_HALF_DRAW_AND_TOTAL_UNDER_2_0((short) 3412, new GameResultWithTotalChecker(), "2й тайм: Х + ТМ 2"),
    SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_2_0((short) 3413, new GameResultWithTotalChecker(), "2й тайм: П2 + ТМ 2"),
    SECOND_HALF_1X_AND_TOTAL_UNDER_2_0((short) 3414, new GameResultWithTotalChecker(), "2й тайм: 1Х + ТМ 2"),
    SECOND_HALF_12_AND_TOTAL_UNDER_2_0((short) 3415, new GameResultWithTotalChecker(), "2й тайм: 12 + ТМ 2"),
    SECOND_HALF_X2_AND_TOTAL_UNDER_2_0((short) 3416, new GameResultWithTotalChecker(), "2й тайм: Х2 + ТМ 2"),

    SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_2_5((short) 3421, new GameResultWithTotalChecker(), "2й тайм: П1 + ТМ 2,5"),
    SECOND_HALF_DRAW_AND_TOTAL_UNDER_2_5((short) 3422, new GameResultWithTotalChecker(), "2й тайм: Х + ТМ 2,5"),
    SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_2_5((short) 3423, new GameResultWithTotalChecker(), "2й тайм: П2 + ТМ 2,5"),
    SECOND_HALF_1X_AND_TOTAL_UNDER_2_5((short) 3424, new GameResultWithTotalChecker(), "2й тайм: 1Х + ТМ 2,5"),
    SECOND_HALF_12_AND_TOTAL_UNDER_2_5((short) 3425, new GameResultWithTotalChecker(), "2й тайм: 12 + ТМ 2,5"),
    SECOND_HALF_X2_AND_TOTAL_UNDER_2_5((short) 3426, new GameResultWithTotalChecker(), "2й тайм: Х2 + ТМ 2,5"),

    SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_3_0((short) 3431, new GameResultWithTotalChecker(), "2й тайм: П1 + ТМ 3"),
    SECOND_HALF_DRAW_AND_TOTAL_UNDER_3_0((short) 3432, new GameResultWithTotalChecker(), "2й тайм: Х + ТМ 3"),
    SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_3_0((short) 3433, new GameResultWithTotalChecker(), "2й тайм: П2 + ТМ 3"),
    SECOND_HALF_1X_AND_TOTAL_UNDER_3_0((short) 3434, new GameResultWithTotalChecker(), "2й тайм: 1Х + ТМ 3"),
    SECOND_HALF_12_AND_TOTAL_UNDER_3_0((short) 3435, new GameResultWithTotalChecker(), "2й тайм: 12 + ТМ 3"),
    SECOND_HALF_X2_AND_TOTAL_UNDER_3_0((short) 3436, new GameResultWithTotalChecker(), "2й тайм: Х2 + ТМ 3"),

    SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_3_5((short) 3441, new GameResultWithTotalChecker(), "2й тайм: П1 + ТМ 3,5"),
    SECOND_HALF_DRAW_AND_TOTAL_UNDER_3_5((short) 3442, new GameResultWithTotalChecker(), "2й тайм: Х + ТМ 3,5"),
    SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_3_5((short) 3443, new GameResultWithTotalChecker(), "2й тайм: П2 + ТМ 3,5"),
    SECOND_HALF_1X_AND_TOTAL_UNDER_3_5((short) 3444, new GameResultWithTotalChecker(), "2й тайм: 1Х + ТМ 3,5"),
    SECOND_HALF_12_AND_TOTAL_UNDER_3_5((short) 3445, new GameResultWithTotalChecker(), "2й тайм: 12 + ТМ 3,5"),
    SECOND_HALF_X2_AND_TOTAL_UNDER_3_5((short) 3446, new GameResultWithTotalChecker(), "2й тайм: Х2 + ТМ 3,5"),

    //========== 3501-3600. 2й тайм: Исход + Тотал Больше (Second Half Result + Total Over) ==========
    SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_1_5((short) 3501, new GameResultWithTotalChecker(), "2й тайм: П1 + ТБ 1,5"),
    SECOND_HALF_DRAW_AND_TOTAL_OVER_1_5((short) 3502, new GameResultWithTotalChecker(), "2й тайм: Х + ТБ 1,5"),
    SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_1_5((short) 3503, new GameResultWithTotalChecker(), "2й тайм: П2 + ТБ 1,5"),
    SECOND_HALF_1X_AND_TOTAL_OVER_1_5((short) 3504, new GameResultWithTotalChecker(), "2й тайм: 1Х + ТБ 1,5"),
    SECOND_HALF_12_AND_TOTAL_OVER_1_5((short) 3505, new GameResultWithTotalChecker(), "2й тайм: 12 + ТБ 1,5"),
    SECOND_HALF_X2_AND_TOTAL_OVER_1_5((short) 3506, new GameResultWithTotalChecker(), "2й тайм: Х2 + ТБ 1,5"),

    SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_2_0((short) 3511, new GameResultWithTotalChecker(), "2й тайм: П1 + ТБ 2"),
    SECOND_HALF_DRAW_AND_TOTAL_OVER_2_0((short) 3512, new GameResultWithTotalChecker(), "2й тайм: Х + ТБ 2"),
    SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_2_0((short) 3513, new GameResultWithTotalChecker(), "2й тайм: П2 + ТБ 2"),
    SECOND_HALF_1X_AND_TOTAL_OVER_2_0((short) 3514, new GameResultWithTotalChecker(), "2й тайм: 1Х + ТБ 2"),
    SECOND_HALF_12_AND_TOTAL_OVER_2_0((short) 3515, new GameResultWithTotalChecker(), "2й тайм: 12 + ТБ 2"),
    SECOND_HALF_X2_AND_TOTAL_OVER_2_0((short) 3516, new GameResultWithTotalChecker(), "2й тайм: Х2 + ТБ 2"),

    SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_2_5((short) 3521, new GameResultWithTotalChecker(), "2й тайм: П1 + ТБ 2,5"),
    SECOND_HALF_DRAW_AND_TOTAL_OVER_2_5((short) 3522, new GameResultWithTotalChecker(), "2й тайм: Х + ТБ 2,5"),
    SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_2_5((short) 3523, new GameResultWithTotalChecker(), "2й тайм: П2 + ТБ 2,5"),
    SECOND_HALF_1X_AND_TOTAL_OVER_2_5((short) 3524, new GameResultWithTotalChecker(), "2й тайм: 1Х + ТБ 2,5"),
    SECOND_HALF_12_AND_TOTAL_OVER_2_5((short) 3525, new GameResultWithTotalChecker(), "2й тайм: 12 + ТБ 2,5"),
    SECOND_HALF_X2_AND_TOTAL_OVER_2_5((short) 3526, new GameResultWithTotalChecker(), "2й тайм: Х2 + ТБ 2,5"),

    SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_3_0((short) 3531, new GameResultWithTotalChecker(), "2й тайм: П1 + ТБ 3"),
    SECOND_HALF_DRAW_AND_TOTAL_OVER_3_0((short) 3532, new GameResultWithTotalChecker(), "2й тайм: Х + ТБ 3"),
    SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_3_0((short) 3533, new GameResultWithTotalChecker(), "2й тайм: П2 + ТБ 3"),
    SECOND_HALF_1X_AND_TOTAL_OVER_3_0((short) 3534, new GameResultWithTotalChecker(), "2й тайм: 1Х + ТБ 3"),
    SECOND_HALF_12_AND_TOTAL_OVER_3_0((short) 3535, new GameResultWithTotalChecker(), "2й тайм: 12 + ТБ 3"),
    SECOND_HALF_X2_AND_TOTAL_OVER_3_0((short) 3536, new GameResultWithTotalChecker(), "2й тайм: Х2 + ТБ 3"),

    SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_3_5((short) 3541, new GameResultWithTotalChecker(), "2й тайм: П1 + ТБ 3,5"),
    SECOND_HALF_DRAW_AND_TOTAL_OVER_3_5((short) 3542, new GameResultWithTotalChecker(), "2й тайм: Х + ТБ 3,5"),
    SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_3_5((short) 3543, new GameResultWithTotalChecker(), "2й тайм: П2 + ТБ 3,5"),
    SECOND_HALF_1X_AND_TOTAL_OVER_3_5((short) 3544, new GameResultWithTotalChecker(), "2й тайм: 1Х + ТБ 3,5"),
    SECOND_HALF_12_AND_TOTAL_OVER_3_5((short) 3545, new GameResultWithTotalChecker(), "2й тайм: 12 + ТБ 3,5"),
    SECOND_HALF_X2_AND_TOTAL_OVER_3_5((short) 3546, new GameResultWithTotalChecker(), "2й тайм: Х2 + ТБ 3,5"),

    //========== 3601-3700. Тайм с большим количеством голов (Half With More Goals) ==========
    TOTAL_GOALS_FIRST_HALF_MORE_THAN_SECOND((short) 3601, new TotalChecker(), "Тотал голов: 1й > 2й тайм"),
    TOTAL_GOALS_EQUAL_IN_BOTH_HALVES((short) 3602, new TotalChecker(), "Тотал голов: Поровну"),
    TOTAL_GOALS_FIRST_HALF_LESS_THAN_SECOND((short) 3603, new TotalChecker(), "Тотал голов: 1й < 2й тайм"),
    HOME_GOALS_FIRST_HALF_MORE_THAN_SECOND((short) 3604, new TotalChecker(), "Тотал голов: (Хозяева) 1й > 2й тайм"),
    HOME_GOALS_EQUAL_IN_BOTH_HALVES((short) 3605, new TotalChecker(), "Тотал голов: (Хозяева) Поровну"),
    HOME_GOALS_FIRST_HALF_LESS_THAN_SECOND((short) 3606, new TotalChecker(), "Тотал голов: (Хозяева) 1й < 2й тайм"),
    AWAY_GOALS_FIRST_HALF_MORE_THAN_SECOND((short) 3607, new TotalChecker(), "Тотал голов: (Гости) 1й > 2й тайм"),
    AWAY_GOALS_EQUAL_IN_BOTH_HALVES((short) 3608, new TotalChecker(), "Тотал голов: (Гости) Поровну"),
    AWAY_GOALS_FIRST_HALF_LESS_THAN_SECOND((short) 3609, new TotalChecker(), "Тотал голов: (Гости) 1й < 2й тайм"),


    // =================================================================================================
    // Ставки на особые события являются отдельной группой,
    // поэтому под них выделен особый диапазон кодов, начиная с 5000
    // =================================================================================================

    //========== 5001-5100. Победа всухую (Clean Win) ==========
    CLEAN_WIN_HOME((short) 5001, new SpecialBetsChecker(), "Хозяева - победа всухую"),
    CLEAN_WIN_AWAY((short) 5002, new SpecialBetsChecker(), "Гости - победа всухую"),
    CLEAN_WIN_ANY((short) 5003, new SpecialBetsChecker(), "Любая - победа всухую"),

    //========== 5101-5200. Разница в голах (Goals Difference) ==========
    GOALS_DIFF_HOME_WIN_1((short) 5101, new SpecialBetsChecker(), "П1 в 1 гол"),
    GOALS_DIFF_AWAY_WIN_1((short) 5102, new SpecialBetsChecker(), "П2 в 1 гол"),
    GOALS_DIFF_HOME_WIN_2((short) 5103, new SpecialBetsChecker(), "П1 в 2 гола"),
    GOALS_DIFF_AWAY_WIN_2((short) 5104, new SpecialBetsChecker(), "П2 в 2 гола"),
    GOALS_DIFF_HOME_WIN_3((short) 5105, new SpecialBetsChecker(), "П1 в 3 гола"),
    GOALS_DIFF_AWAY_WIN_3((short) 5106, new SpecialBetsChecker(), "П2 в 3 гола"),
    GOALS_DIFF_HOME_OR_AWAY_WIN_1((short) 5107, new SpecialBetsChecker(), "П1 или П2 в 1 гол"),
    GOALS_DIFF_HOME_OR_AWAY_WIN_2((short) 5108, new SpecialBetsChecker(), "П1 или П2 в 2 гола"),
    GOALS_DIFF_HOME_OR_AWAY_WIN_3((short) 5109, new SpecialBetsChecker(), "П1 или П2 в 3 гола"),

    //========== 5201-5300. Playoff Outcomes (Особые исходы playoff) ==========
    PLAYOFF_EXTRA_TIME((short) 5201, new SpecialBetsChecker(), "Дополнительное время"),
    PLAYOFF_PENALTIES((short) 5202, new SpecialBetsChecker(), "Послематчевые пенальти"),
    PLAYOFF_HOME_WIN_REGULAR((short) 5203, new SpecialBetsChecker(), "П1 в осн.время"),
    PLAYOFF_AWAY_WIN_REGULAR((short) 5204, new SpecialBetsChecker(), "П2 в осн.время"),
    PLAYOFF_HOME_OR_AWAY_REGULAR((short) 5205, new SpecialBetsChecker(), "12 в осн.время"),
    PLAYOFF_HOME_WIN_OVERTIME((short) 5206, new SpecialBetsChecker(), "П1 в доп.время"),
    PLAYOFF_AWAY_WIN_OVERTIME((short) 5207, new SpecialBetsChecker(), "П2 в доп.время"),
    PLAYOFF_HOME_OR_AWAY_OVERTIME((short) 5208, new SpecialBetsChecker(), "12 в доп.время"),
    PLAYOFF_HOME_WIN_PENALTIES((short) 5209, new SpecialBetsChecker(), "П1 по пенальти"),
    PLAYOFF_AWAY_WIN_PENALTIES((short) 5210, new SpecialBetsChecker(), "П2 по пенальти"),
    PLAYOFF_HOME_OR_AWAY_PENALTIES((short) 5211, new SpecialBetsChecker(), "12 по пенальти"),
    PLAYOFF_HOME_ADVANCE_NEXT_STAGE((short) 5212, new SpecialBetsChecker(), "Хозяева - выход в след.стадию"),
    PLAYOFF_AWAY_ADVANCE_NEXT_STAGE((short) 5213, new SpecialBetsChecker(), "Гости - выход в след.стадию"),
    PLAYOFF_HOME_ADVANCE_FINAL((short) 5214, new SpecialBetsChecker(), "Хозяева - выход в финал"),
    PLAYOFF_AWAY_ADVANCE_FINAL((short) 5215, new SpecialBetsChecker(), "Гости - выход в финал"),
    PLAYOFF_HOME_WIN_TOURNAMENT((short) 5216, new SpecialBetsChecker(), "Хозяева - победитель турнира"),
    PLAYOFF_AWAY_WIN_TOURNAMENT((short) 5217, new SpecialBetsChecker(), "Гости - победитель турнира");

    private final short code;
    private final BetChecker checker;
    private final String label;

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
            throw new IllegalStateException("No BetChecker defined for code: " + code);
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
