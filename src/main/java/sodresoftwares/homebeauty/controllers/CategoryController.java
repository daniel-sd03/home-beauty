package sodresoftwares.homebeauty.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.homebeauty.dto.CategoryDTO;
import sodresoftwares.homebeauty.model.Category;
import sodresoftwares.homebeauty.repositories.CategoryRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryRepository repository;

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody @Valid CategoryDTO data) {
        Category category = Category.builder()
                .name(data.name())
                .iconName(data.iconName())
                .build();

        repository.save(category);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        var categories = repository.findAll();

        var response = categories.stream()
                .map(category -> new CategoryDTO(
                        category.getId(),
                        category.getName(),
                        category.getIconName()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }
}
