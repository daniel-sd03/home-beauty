package sodresoftwares.homebeauty.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provided_services_id", nullable = false)
    private ProvidedService providedService;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;
}