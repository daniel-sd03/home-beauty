package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sodresoftwares.homebeauty.model.State;

import java.util.Optional;

public interface StateRepository extends JpaRepository<State, String> {
    Optional<State> findByUfIgnoreCase(String uf);
}