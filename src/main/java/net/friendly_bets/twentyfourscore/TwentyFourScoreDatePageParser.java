package net.friendly_bets.twentyfourscore;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TwentyFourScoreDatePageParser {

    private static final Pattern SCORE = Pattern.compile("(\\d{1,2})\\s*:\\s*(\\d{1,2})");
    private static final Pattern HT = Pattern.compile("\\((\\d{1,2})\\s*:\\s*(\\d{1,2})\\)");
    private static final Pattern LIVE_MIN = Pattern.compile("(\\d{1,3})\\s*'");
    private static final Pattern MATCH_ID = Pattern.compile("/football/match/(\\d+)");

    public TwentyFourScoreParsedDatePage parse(String html) {
        Document doc = Jsoup.parse(html != null ? html : "");
        List<TwentyFourScoreParsedDatePage.CompetitionBlock> blocks = new ArrayList<>();
        TwentyFourScoreParsedDatePage.CompetitionBlock current = null;

        Elements rows = doc.select("table.daymatches tr, table.fbl tr");
        for (Element row : rows) {
            Element header = row.selectFirst("th.champheader, .champheader_title");
            if (header != null || row.selectFirst("th") != null && row.text().length() > 0 && row.select("td").isEmpty()) {
                String title = textOrEmpty(row.selectFirst("a, .champheader_title, th"));
                if (!title.isBlank()) {
                    current = TwentyFourScoreParsedDatePage.CompetitionBlock.builder()
                            .title(title)
                            .matches(new ArrayList<>())
                            .build();
                    blocks.add(current);
                }
                continue;
            }
            if (current == null) {
                continue;
            }
            parseMatchRow(row).ifPresent(current.getMatches()::add);
        }
        return TwentyFourScoreParsedDatePage.builder().competitions(blocks).build();
    }

    private java.util.Optional<TwentyFourScoreParsedDatePage.MatchRow> parseMatchRow(Element row) {
        Element home = row.selectFirst("span.tm1");
        Element away = row.selectFirst("span.tm2");
        if (home == null || away == null) {
            Elements teamCells = row.select("td.team");
            if (teamCells.size() >= 2) {
                if (home == null) {
                    home = teamCells.get(0).selectFirst("a, span");
                    if (home == null) {
                        home = teamCells.get(0);
                    }
                }
                if (away == null) {
                    away = teamCells.get(1).selectFirst("a, span");
                    if (away == null) {
                        away = teamCells.get(1);
                    }
                }
            }
        }
        Element scoreCell = row.selectFirst("td.score, .score");
        if (home == null || away == null) {
            return java.util.Optional.empty();
        }
        String homeName = textOrEmpty(home);
        String awayName = textOrEmpty(away);
        if (homeName.isBlank() || awayName.isBlank()) {
            return java.util.Optional.empty();
        }
        String scoreText = scoreCell != null ? textOrEmpty(scoreCell) : textOrEmpty(row.selectFirst("td.time"));
        String matchId = null;
        Element link = row.selectFirst("a[href*=/football/match/]");
        if (link != null) {
            Matcher idMatcher = MATCH_ID.matcher(link.attr("href"));
            if (idMatcher.find()) {
                matchId = idMatcher.group(1);
            }
        }
        if (matchId == null && row.id() != null && row.id().startsWith("row_")) {
            matchId = row.id().substring(4);
        }

        String fullTime = null;
        String firstTime = null;
        Matcher scoreMatcher = SCORE.matcher(scoreText);
        if (scoreMatcher.find()) {
            fullTime = scoreMatcher.group(1) + ":" + scoreMatcher.group(2);
        }
        Matcher htMatcher = HT.matcher(scoreText);
        if (htMatcher.find()) {
            firstTime = htMatcher.group(1) + ":" + htMatcher.group(2);
        }

        String liveMinute = null;
        Matcher liveMatcher = LIVE_MIN.matcher(scoreText);
        if (liveMatcher.find()) {
            liveMinute = liveMatcher.group(1) + "'";
        }

        String status = resolveStatus(scoreText, fullTime, liveMinute);
        return java.util.Optional.of(TwentyFourScoreParsedDatePage.MatchRow.builder()
                .externalMatchId(matchId)
                .homeName(homeName)
                .awayName(awayName)
                .scoreText(scoreText)
                .fullTimeScore(fullTime)
                .firstTimeScore(firstTime)
                .liveMinuteLabel(liveMinute)
                .status(status)
                .build());
    }

    private static String resolveStatus(String scoreText, String fullTime, String liveMinute) {
        String lower = scoreText != null ? scoreText.toLowerCase(Locale.ROOT) : "";
        if (liveMinute != null) {
            return "LIVE";
        }
        if (lower.contains("перер") || lower.contains("half")) {
            return "PAUSED";
        }
        if (fullTime != null) {
            return "FINISHED";
        }
        if (lower.contains("—") || lower.contains("-") || lower.isBlank()) {
            return "SCHEDULED";
        }
        return "SCHEDULED";
    }

    private static String textOrEmpty(Element el) {
        return el == null ? "" : el.text().replace('\u00a0', ' ').trim();
    }
}
