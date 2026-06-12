package net.friendly_bets.fourscore;

public enum FourScoreLeagueSection {
    WORLD_CUP("Чемпионат мира"),
    FRIENDLIES("Товарищеские матчи");

    private final String leagueName;

    FourScoreLeagueSection(String leagueName) {
        this.leagueName = leagueName;
    }

    public String leagueName() {
        return leagueName;
    }
}
