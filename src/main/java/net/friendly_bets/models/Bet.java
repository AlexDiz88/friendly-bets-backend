package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name = "bets")
public class Bet {

    public enum BetStatus {
        OPENED, WON, RETURNED, LOST, EMPTY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name = "match_day")
    private String matchDay;

    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "game_date")
    private LocalDateTime gameDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    @Column(name = "bet_title")
    private String betTitle;

    @Column(name = "bet_odds")
    private Double betOdds;

    @Column(name = "bet_size")
    private Integer betSize;

    @Column(name = "game_result")
    private String gameResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "bet_status")
    private BetStatus betStatus;

    @Column(name = "balance_change")
    private Double balanceChange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id")
    private League league;
}
