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
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "full_title_ru")
    private String fullTitleRu;

    @Column(name = "full_title_en")
    private String fullTitleEn;

    @Column(name = "country")
    private String country;

    @Column(name = "logo")
    private String logo;

    @ManyToMany(mappedBy = "teams", fetch = FetchType.LAZY)
    private List<League> leagues;
}
