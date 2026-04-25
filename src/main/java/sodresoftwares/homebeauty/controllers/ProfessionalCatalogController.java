package sodresoftwares.homebeauty.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.homebeauty.dto.ProfessionalBlockDTO;
import sodresoftwares.homebeauty.dto.ProfessionalBlockResponseDTO;
import sodresoftwares.homebeauty.dto.ProvidedServiceDTO;
import sodresoftwares.homebeauty.dto.WorkingHourDTO;
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
    public ResponseEntity<Void> addService(@RequestBody @Valid ProvidedServiceDTO data) {
        catalogService.addProvidedService(data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/services")
    public ResponseEntity<List<ProvidedServiceDTO>> getMyServices() {
        List<ProvidedServiceDTO> services = catalogService.getMyProvidedServices();
        return ResponseEntity.ok(services);
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<Void> updateService(@PathVariable String id, @RequestBody @Valid ProvidedServiceDTO data) {
        catalogService.updateService(id, data);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/working-hours")
    public ResponseEntity<Void> addWorkingHour(@RequestBody @Valid WorkingHourDTO data) {
        catalogService.addWorkingHour(data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/working-hours")
    public ResponseEntity<List<WorkingHourDTO>> getMyWorkingHours() {
        var workingHours = catalogService.getMyWorkingHours();
        return ResponseEntity.ok(workingHours);
    }

    @PutMapping("/working-hours/{id}")
    public ResponseEntity<Void> updateWorkingHour(@PathVariable String id, @RequestBody @Valid WorkingHourDTO data) {
        catalogService.updateWorkingHour(id, data);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable String id) {
        catalogService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/working-hours/{id}")
    public ResponseEntity<Void> deleteWorkingHour(@PathVariable String id) {
        catalogService.deleteWorkingHour(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/blocks")
    public ResponseEntity<Void> createBlock(@RequestBody @Valid ProfessionalBlockDTO data) {
        catalogService.createBlock(data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/blocks")
    public ResponseEntity<List<ProfessionalBlockResponseDTO>> getMyBlocks() {
        List<ProfessionalBlockResponseDTO> blocks = catalogService.getMyBlocks();
        return ResponseEntity.ok(blocks);
    }
}