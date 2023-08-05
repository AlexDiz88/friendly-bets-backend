package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name = "leagues")
public class League {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "league_name")
    private String name;

    @Column(name = "display_name_ru")
    private String displayNameRu;

    @Column(name = "display_name_en")
    private String displayNameEn;

    @Column(name = "short_name_ru")
    private String shortNameRu;

    @Column(name = "short_name_en")
    private String shortNameEn;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "league_team",
            joinColumns = @JoinColumn(name = "league_id"),
            inverseJoinColumns = @JoinColumn(name = "team_id"))
    private List<Team> teams;

    @OneToMany(mappedBy = "league", fetch = FetchType.LAZY)
    private List<Bet> bets;
}
