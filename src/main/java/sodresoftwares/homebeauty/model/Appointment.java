package sodresoftwares.homebeauty.model;

import jakarta.persistence.*;
import lombok.*;
import sodresoftwares.homebeauty.enums.AppointmentStatus;
import sodresoftwares.homebeauty.enums.AppointmentType;
import sodresoftwares.homebeauty.model.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // --- Date Operational ---
    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_type", nullable = false)
    private AppointmentType appointmentType;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    private String notes;

    // --- Date Snapshot ---
    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "professional_name", nullable = false)
    private String professionalName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // --- Renationalisation ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_user_id", nullable = false)
    private User professionalUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provided_services_id")
    private ProvidedService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;
}