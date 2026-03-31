package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sodresoftwares.homebeauty.model.City;

import java.util.Optional;

public interface CityRepository extends JpaRepository<City, String> {
    Optional<City> findByNameIgnoreCaseAndStateId(String name, String stateId);
}