package net.friendly_bets.oddsapi.mapping;

import net.friendly_bets.models.BetTitle;

import java.util.Objects;

public record BetTitleKey(short code, boolean isNot) {

    public static BetTitleKey from(BetTitle betTitle) {
        if (betTitle == null) {
            return null;
        }
        return new BetTitleKey(betTitle.getCode(), betTitle.isNot());
    }

    public String storageKey() {
        return code + ":" + (isNot ? "1" : "0");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BetTitleKey that)) {
            return false;
        }
        return code == that.code && isNot == that.isNot;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, isNot);
    }
}
