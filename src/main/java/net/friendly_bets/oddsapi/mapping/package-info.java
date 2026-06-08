/**
 * Пайплайн odds-api.io: сырые JSON букмекеров → канонические {@link net.friendly_bets.models.BetTitle} → UI.
 *
 * <h2>Этап 1 — маппинг каждой БК отдельно</h2>
 * <p>
 * {@link OddsMappingPipeline} обходит {@code bookmakers} из ответа API по одной колонке.
 * Для каждого канонического имени (1xbet, Bet365) — свой {@link OddsBookmakerAdapter}.
 * Рынки форы (Spread, Asian Handicap) odds-api <strong>не маппятся</strong> — prod-форы только из Marathonbet SSE.
 * </p>
 * <p>Результат: {@link MappedOddsQuote} с {@code betTitle} = enum {@link net.friendly_bets.models.enums.BetTitleCode}.</p>
 *
 * <h2>Этап 2 — union по BetTitle + сверка кэфов</h2>
 * <p>
 * {@link OddsMerger} группирует по {@link BetTitleKey} (code + isNot).
 * Относительное расхождение кэфов между БК &gt; 50% — mismatch, fail-closed для этой ставки.
 * </p>
 */
package net.friendly_bets.oddsapi.mapping;
