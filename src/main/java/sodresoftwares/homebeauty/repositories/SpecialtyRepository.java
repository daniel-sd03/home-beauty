package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sodresoftwares.homebeauty.model.Specialty;

public interface SpecialtyRepository extends JpaRepository<Specialty, String> {
    boolean existsByNameIgnoreCase(String name);
}
