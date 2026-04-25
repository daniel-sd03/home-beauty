package sodresoftwares.homebeauty.model;

import jakarta.persistence.*;
import lombok.*;
import sodresoftwares.homebeauty.model.user.User;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "professional_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ProfessionalProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String description;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProvidedService> services;

    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkingHour> workingHours;

    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL)
    private List<ProfessionalBlock> blocks;
}
