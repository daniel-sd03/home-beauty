package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sodresoftwares.homebeauty.model.WorkingHour;

public interface WorkingHourRepository extends JpaRepository<WorkingHour, String> {
}