package net.friendly_bets.providers;

import java.util.EnumSet;
import java.util.Set;

/**
 * Declares which layers a provider bean can serve.
 */
public interface ExternalDataProvider {

    /** Logical provider id, e.g. {@code soccer365.ru}, {@code marathonbet}, {@code 24score.pro}. */
    String providerId();

    Set<ExternalDataLayer> capabilities();

    default boolean supports(ExternalDataLayer layer) {
        return capabilities() != null && capabilities().contains(layer);
    }

    static Set<ExternalDataLayer> of(ExternalDataLayer... layers) {
        return EnumSet.copyOf(java.util.Arrays.asList(layers));
    }
}
