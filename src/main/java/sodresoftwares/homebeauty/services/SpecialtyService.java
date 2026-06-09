package sodresoftwares.homebeauty.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.dto.SpecialtyRequestDTO;
import sodresoftwares.homebeauty.dto.SpecialtyResponseDTO;
import sodresoftwares.homebeauty.model.Specialty;
import sodresoftwares.homebeauty.repositories.SpecialtyRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialtyService {

    private final SpecialtyRepository repository;

    @Transactional
    public SpecialtyResponseDTO create(SpecialtyRequestDTO data) {
        if (repository.existsByNameIgnoreCase(data.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Specialty name already exists.");
        }

        Specialty specialty = Specialty.builder()
                .name(data.name())
                .build();

        return new SpecialtyResponseDTO(repository.save(specialty));
    }

    public List<SpecialtyResponseDTO> findAll() {
        return repository.findAll().stream()
                .map(SpecialtyResponseDTO::new)
                .toList();
    }

    public SpecialtyResponseDTO findById(String id) {
        Specialty specialty = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Specialty not found."));
        return new SpecialtyResponseDTO(specialty);
    }

    @Transactional
    public SpecialtyResponseDTO update(String id, SpecialtyRequestDTO data) {
        Specialty specialty = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Specialty not found."));

        if (!specialty.getName().equalsIgnoreCase(data.name()) && repository.existsByNameIgnoreCase(data.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Specialty name already exists.");
        }

        specialty.setName(data.name());
        return new SpecialtyResponseDTO(repository.save(specialty));
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Specialty not found.");
        }
        repository.deleteById(id);
    }
}