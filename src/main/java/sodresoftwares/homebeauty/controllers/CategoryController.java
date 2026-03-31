package sodresoftwares.homebeauty.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sodresoftwares.homebeauty.dto.CategoryRequestDTO;
import sodresoftwares.homebeauty.model.Category;
import sodresoftwares.homebeauty.repositories.CategoryRepository;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryRepository repository;

    public CategoryController(CategoryRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody @Valid CategoryRequestDTO data) {
        Category category = Category.builder()
                .name(data.name())
                .build();

        repository.save(category);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
