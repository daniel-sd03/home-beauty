package sodresoftwares.homebeauty.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.homebeauty.dto.AppointmentCreateDTO;
import sodresoftwares.homebeauty.dto.AppointmentResponseDTO;
import sodresoftwares.homebeauty.services.AppointmentService;

import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Creates a new appointment.
     * The Service automatically identifies the logged-in client.
     */
    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> createAppointment(@RequestBody @Valid AppointmentCreateDTO dto) {
        AppointmentResponseDTO response = appointmentService.createAppointment(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Returns the list of appointments where the logged-in user is the CLIENT.
     * Ordered by start time (Ascending).
     */
    @GetMapping("/client")
    public ResponseEntity<List<AppointmentResponseDTO>> getClientAppointments() {
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByClient();
        return ResponseEntity.ok(appointments);
    }

    /**
     * Returns the list of appointments where the logged-in user is the PROFESSIONAL.
     * Ordered by start time (Ascending).
     */
    @GetMapping("/professional")
    public ResponseEntity<List<AppointmentResponseDTO>> getProfessionalAppointments() {
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByProfessional();
        return ResponseEntity.ok(appointments);
    }
    /**
     * Returns a specific appointment by its ID.
     * Security: Only the client or professional involved can access it.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> getAppointmentById(@PathVariable String id) {
        AppointmentResponseDTO appointment = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(appointment);
    }
}