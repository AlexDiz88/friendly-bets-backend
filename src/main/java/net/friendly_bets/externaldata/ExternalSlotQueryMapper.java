package net.friendly_bets.externaldata;

import net.friendly_bets.models.ExpandedMatchdaySlot;

import java.util.Optional;

/**
 * Maps a FriendlyBets slot ({@code Bet.match_day}) to a provider-specific query.
 */
public interface ExternalSlotQueryMapper {

    String providerId();

    Optional<ExternalSlotQuery> map(ExpandedMatchdaySlot slot, String competitionCode);
}
