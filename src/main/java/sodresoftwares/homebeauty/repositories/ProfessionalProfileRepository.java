package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sodresoftwares.homebeauty.model.ProfessionalProfile;
import java.util.Optional;

public interface ProfessionalProfileRepository extends JpaRepository<ProfessionalProfile, String> {
    Optional<ProfessionalProfile> findByUserId(String userId);
}