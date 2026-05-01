package sodresoftwares.homebeauty.model;

import jakarta.persistence.*;
import lombok.*;
import sodresoftwares.homebeauty.enums.ServiceLocationType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "provided_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ProvidedService {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false)
    private ServiceLocationType locationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    private ProfessionalProfile professional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "providedService", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ServiceImage> images = new ArrayList<>();

    // Helper method to add an image to the service
    public void addImage(String url) {
        ServiceImage image = ServiceImage.builder()
                .imageUrl(url)
                .providedService(this)
                .build();
        this.images.add(image);
    }
}
