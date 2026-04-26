package sodresoftwares.homebeauty.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.homebeauty.services.AvailabilityService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/{professionalId}")
    public ResponseEntity<List<LocalDateTime>> getAvailableSlots(
            @PathVariable String professionalId,
            @RequestParam String serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<LocalDateTime> availableSlots = availabilityService.getAvailableSlots(professionalId, serviceId, date);

        return ResponseEntity.ok(availableSlots);
    }
}