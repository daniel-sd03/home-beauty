package sodresoftwares.homebeauty.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.homebeauty.dto.SpecialtyRequestDTO;
import sodresoftwares.homebeauty.dto.SpecialtyResponseDTO;
import sodresoftwares.homebeauty.services.SpecialtyService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/specialties")
public class SpecialtyController {

    private final SpecialtyService service;

    @PostMapping
    public ResponseEntity<SpecialtyResponseDTO> create(@RequestBody @Valid SpecialtyRequestDTO data) {
        SpecialtyResponseDTO created = service.create(data);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public ResponseEntity<List<SpecialtyResponseDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpecialtyResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpecialtyResponseDTO> update(@PathVariable String id, @RequestBody @Valid SpecialtyRequestDTO data) {
        return ResponseEntity.ok(service.update(id, data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}