package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sodresoftwares.homebeauty.model.Category;

public interface CategoryRepository extends JpaRepository<Category, String> {
}