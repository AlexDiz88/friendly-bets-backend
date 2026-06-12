package net.friendly_bets.wc26;

import java.util.Map;

/** IANA timezone per WC26 venue — mirrors frontend {@code wc26Venues.ts}. */
public final class Wc26VenueTimezones {

    private static final Map<String, String> BY_VENUE_KEY = Map.ofEntries(
            Map.entry("mexicoCity", "America/Mexico_City"),
            Map.entry("guadalajara", "America/Mexico_City"),
            Map.entry("toronto", "America/Toronto"),
            Map.entry("losAngeles", "America/Los_Angeles"),
            Map.entry("boston", "America/New_York"),
            Map.entry("vancouver", "America/Vancouver"),
            Map.entry("newYork", "America/New_York"),
            Map.entry("sanFrancisco", "America/Los_Angeles"),
            Map.entry("philadelphia", "America/New_York"),
            Map.entry("houston", "America/Chicago"),
            Map.entry("dallas", "America/Chicago"),
            Map.entry("monterrey", "America/Monterrey"),
            Map.entry("miami", "America/New_York"),
            Map.entry("atlanta", "America/New_York"),
            Map.entry("seattle", "America/Los_Angeles"),
            Map.entry("kansasCity", "America/Chicago")
    );

    private Wc26VenueTimezones() {
    }

    public static String forVenueKey(String venueKey) {
        if (venueKey == null || venueKey.isBlank()) {
            return "UTC";
        }
        return BY_VENUE_KEY.getOrDefault(venueKey, "UTC");
    }
}
