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
@Table(name = "seasons")
public class Season {

    public enum Status {
        CREATED, SCHEDULED, ACTIVE, PAUSED, FINISHED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "title")
    private String title;

    @Column(name = "bet_count_per_match_day")
    private Integer betCountPerMatchDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @ManyToMany(mappedBy = "seasons", fetch = FetchType.LAZY)
    private List<User> players;

    @ManyToMany(mappedBy = "seasons", fetch = FetchType.LAZY)
    private List<League> leagues;
}
