package net.friendly_bets.wc26;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Wc26ScheduleSeedDto {

    private int id;
    private String date;
    private String timeLocal;
    private String stage;
    private String group;
    private String home;
    private String away;
    private String labelKey;
    private String venueKey;
}
