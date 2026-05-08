package sodresoftwares.homebeauty.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sodresoftwares.homebeauty.dto.CategoryDTO;
import sodresoftwares.homebeauty.infra.security.SecurityFilter;
import sodresoftwares.homebeauty.model.Category;
import sodresoftwares.homebeauty.repositories.CategoryRepository;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CategoryController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CategoryController Tests")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryRepository categoryRepository;

    private CategoryDTO categoryDTO;
    private Category category;

    @BeforeEach
    void setUp() {
        categoryDTO = new CategoryDTO(null, "Hair Care", "scissors");
        category = Category.builder()
                .id("category-id-123")
                .name("Hair Care")
                .iconName("scissors")
                .build();
    }

    @Test
    @DisplayName("Should create a category successfully")
    void testCreateCategory_Success() throws Exception {
        // Arrange
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // Act & Assert
        mockMvc.perform(post("/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isCreated());

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should return bad request when category data is invalid")
    void testCreateCategory_InvalidData() throws Exception {
        // Arrange - missing required field (name)
        String invalidJson = """
                {
                    "iconName": "scissors"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return bad request when name is blank")
    void testCreateCategory_BlankName() throws Exception {
        // Arrange
        String invalidJson = """
                {
                    "name": "",
                    "iconName": "scissors"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return bad request when iconName is blank")
    void testCreateCategory_BlankIconName() throws Exception {
        // Arrange
        String invalidJson = """
                {
                    "name": "Hair Care",
                    "iconName": ""
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get all categories successfully")
    void testGetAllCategories_Success() throws Exception {
        // Arrange
        List<Category> categories = List.of(category);
        when(categoryRepository.findAll()).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("category-id-123"))
                .andExpect(jsonPath("$[0].name").value("Hair Care"))
                .andExpect(jsonPath("$[0].iconName").value("scissors"));
    }

    @Test
    @DisplayName("Should return empty list when no categories exist")
    void testGetAllCategories_EmptyList() throws Exception {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
