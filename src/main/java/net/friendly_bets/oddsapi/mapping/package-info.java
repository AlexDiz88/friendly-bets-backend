/**
 * Пайплайн odds-api.io: сырые JSON букмекеров → канонические {@link net.friendly_bets.models.BetTitle} → UI.
 *
 * <h2>Этап 1 — маппинг каждой БК отдельно</h2>
 * <p>
 * {@link OddsMappingPipeline} обходит {@code bookmakers} из ответа API по одной колонке.
 * Для каждого канонического имени (1xbet, Bet365) — свой {@link OddsBookmakerAdapter}.
 * Названия рынков в JSON (Spread, Asian Handicap, …) влияют только на этап 1, не на ключ мержа.
 * </p>
 * <ul>
 *   <li>{@link XbetOddsAdapter} — у гостей на форе инвертируется знак {@code hdp}.</li>
 *   <li>{@link Bet365OddsAdapter} — инверсия {@code hdp} у гостей; prod фор — {@link XbetOddsAdapter}.</li>
 * </ul>
 * <p>Результат: {@link MappedOddsQuote} с {@code betTitle} = enum {@link net.friendly_bets.models.enums.BetTitleCode}.</p>
 *
 * <h2>Этап 2 — union по BetTitle + сверка кэфов</h2>
 * <p>
 * {@link OddsMerger} группирует только по {@link BetTitleKey} (code + isNot). Один и тот же
 * BetTitleCode от Spread (1xbet) и Alternative Asian Handicap (Bet365) — одна строка в UI.
 * </p>
 * <ul>
 *   <li>Линия есть только у одной БК — строка всё равно в итоге (колонка одной БК).</li>
 *   <li>Несколько БК с тем же BetTitle — {@code bookmakerOdds}, best odds = max кэф.</li>
 *   <li>Относительное расхождение кэфов между БК &gt; 50% — mismatch, fail-closed для этой ставки.</li>
 * </ul>
 */
package net.friendly_bets.oddsapi.mapping;
