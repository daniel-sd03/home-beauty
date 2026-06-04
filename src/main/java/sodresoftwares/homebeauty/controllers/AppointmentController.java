package sodresoftwares.homebeauty.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.homebeauty.dto.AppointmentCreateDTO;
import sodresoftwares.homebeauty.dto.AppointmentResponseDTO;
import sodresoftwares.homebeauty.dto.AppointmentStatusUpdateDTO;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.services.AppointmentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Creates a new appointment.
     * The Service automatically identifies the logged-in client.
     */
    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> createAppointment(
            @AuthenticationPrincipal User loggedInUser,
            @RequestBody @Valid AppointmentCreateDTO dto) {
        AppointmentResponseDTO response = appointmentService.createAppointment(loggedInUser, dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Returns the list of appointments where the logged-in user is the CLIENT.
     * Ordered by start time (Ascending).
     */
    @GetMapping("/client")
    public ResponseEntity<List<AppointmentResponseDTO>> getClientAppointments(
            @AuthenticationPrincipal User loggedInUser) {
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByClient(loggedInUser);
        return ResponseEntity.ok(appointments);
    }

    /**
     * Returns the list of appointments where the logged-in user is the PROFESSIONAL.
     * Ordered by start time (Ascending).
     */
    @GetMapping("/professional")
    public ResponseEntity<List<AppointmentResponseDTO>> getProfessionalAppointments(
            @AuthenticationPrincipal User loggedInUser) {
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByProfessional(loggedInUser);
        return ResponseEntity.ok(appointments);
    }
    /**
     * Returns a specific appointment by its ID.
     * Security: Only the client or professional involved can access it.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> getAppointmentById(
            @AuthenticationPrincipal User loggedInUser,
            @PathVariable String id) {
        AppointmentResponseDTO appointment = appointmentService.getAppointmentById(loggedInUser, id);
        return ResponseEntity.ok(appointment);
    }

    /**
     * Updates only the status of an existing appointment.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @AuthenticationPrincipal User loggedInUser,
            @PathVariable String id,
            @Valid @RequestBody AppointmentStatusUpdateDTO dto) {

        appointmentService.updateStatus(loggedInUser, id, dto);

        return ResponseEntity.noContent().build();
    }
}