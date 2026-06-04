package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sodresoftwares.homebeauty.model.Review;

public interface ReviewRepository extends JpaRepository<Review, String> {
}
