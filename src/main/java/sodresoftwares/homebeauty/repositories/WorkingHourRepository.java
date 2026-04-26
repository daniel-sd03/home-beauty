package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sodresoftwares.homebeauty.model.WorkingHour;

import java.time.DayOfWeek;
import java.util.Optional;

public interface WorkingHourRepository extends JpaRepository<WorkingHour, String> {
    Optional<WorkingHour> findByProfessionalIdAndDayOfWeek(String professional_id, DayOfWeek dayOfWeek);
}