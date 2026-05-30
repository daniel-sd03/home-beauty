package sodresoftwares.homebeauty.model;

import jakarta.persistence.*;
import lombok.*;
import sodresoftwares.homebeauty.model.user.User;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String description;

    private String whatsapp;

    private String instagramHandle;

    @Builder.Default
    @Column(name = "service_radius_km")
    private Integer serviceRadiusKm = 10;

    @Builder.Default
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.valueOf(0.00);

    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProvidedService> services;

    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkingHour> workingHours;

    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL)
    private List<ProfessionalBlock> blocks;

    @ManyToMany
    @JoinTable(
            name = "professional_specialties",
            joinColumns = @JoinColumn(name = "professional_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "specialty_id")
    )
    private Set<Specialty> specialties = new HashSet<>();
}
