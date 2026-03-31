package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sodresoftwares.homebeauty.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
}