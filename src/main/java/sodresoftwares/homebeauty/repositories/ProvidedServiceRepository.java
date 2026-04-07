package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sodresoftwares.homebeauty.model.ProvidedService;

public interface ProvidedServiceRepository extends JpaRepository<ProvidedService, String> {
}