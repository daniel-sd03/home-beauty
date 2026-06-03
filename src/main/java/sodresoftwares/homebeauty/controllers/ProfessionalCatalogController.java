package sodresoftwares.homebeauty.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.homebeauty.dto.ProfessionalBlockDTO;
import sodresoftwares.homebeauty.dto.ProfessionalBlockResponseDTO;
import sodresoftwares.homebeauty.dto.ProvidedServiceDTO;
import sodresoftwares.homebeauty.dto.WorkingHourDTO;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.services.ProfessionalCatalogService;

import java.util.List;

@RestController
@RequestMapping("/professionals/catalog")
public class ProfessionalCatalogController {

    private final ProfessionalCatalogService catalogService;

    public ProfessionalCatalogController(ProfessionalCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/services")
    public ResponseEntity<Void> addService(
            @AuthenticationPrincipal User loggedInUser,
            @RequestBody @Valid ProvidedServiceDTO data) {
        catalogService.addProvidedService(loggedInUser, data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/services")
    public ResponseEntity<List<ProvidedServiceDTO>> getMyServices(
            @AuthenticationPrincipal User loggedInUser) {
        List<ProvidedServiceDTO> services = catalogService.getMyProvidedServices(loggedInUser);
        return ResponseEntity.ok(services);
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<Void> updateService(
            @AuthenticationPrincipal User loggedInUser,
            @PathVariable String id,
            @RequestBody @Valid ProvidedServiceDTO data) {
        catalogService.updateService(loggedInUser, id, data);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/working-hours")
    public ResponseEntity<Void> addWorkingHour(
            @AuthenticationPrincipal User loggedInUser,
            @RequestBody @Valid WorkingHourDTO data) {
        catalogService.addWorkingHour(loggedInUser, data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/working-hours")
    public ResponseEntity<List<WorkingHourDTO>> getMyWorkingHours(
            @AuthenticationPrincipal User loggedInUser) {
        var workingHours = catalogService.getMyWorkingHours(loggedInUser);
        return ResponseEntity.ok(workingHours);
    }

    @PutMapping("/working-hours/{id}")
    public ResponseEntity<Void> updateWorkingHour(
            @AuthenticationPrincipal User loggedInUser,
            @PathVariable String id,
            @RequestBody @Valid WorkingHourDTO data) {
        catalogService.updateWorkingHour(loggedInUser, id, data);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deleteService(
            @AuthenticationPrincipal User loggedInUser,
            @PathVariable String id) {
        catalogService.deleteService(loggedInUser, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/working-hours/{id}")
    public ResponseEntity<Void> deleteWorkingHour(
            @AuthenticationPrincipal User loggedInUser,
            @PathVariable String id) {
        catalogService.deleteWorkingHour(loggedInUser, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/blocks")
    public ResponseEntity<Void> createBlock(
            @AuthenticationPrincipal User loggedInUser,
            @RequestBody @Valid ProfessionalBlockDTO data) {
        catalogService.createBlock(loggedInUser, data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/blocks")
    public ResponseEntity<List<ProfessionalBlockResponseDTO>> getMyBlocks(
            @AuthenticationPrincipal User loggedInUser) {
        List<ProfessionalBlockResponseDTO> blocks = catalogService.getMyBlocks(loggedInUser);
        return ResponseEntity.ok(blocks);
    }
}