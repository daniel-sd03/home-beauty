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
import sodresoftwares.homebeauty.dto.SpecialtyRequestDTO;
import sodresoftwares.homebeauty.dto.SpecialtyResponseDTO;
import sodresoftwares.homebeauty.infra.security.SecurityFilter;
import sodresoftwares.homebeauty.service.SpecialtyService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SpecialtyController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class SpecialtyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpecialtyService service;

    @Autowired
    private ObjectMapper objectMapper;

    private final String MOCK_ID = "123e4567-e89b-12d3-a456-426614174000";

    private SpecialtyRequestDTO requestDTO;
    private SpecialtyResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new SpecialtyRequestDTO("Manicure");
        responseDTO = new SpecialtyResponseDTO(MOCK_ID, "Manicure");
    }

    // ==================== CREATE TESTS ====================

    @Test
    @DisplayName("POST /specialties - Should return 200 OK")
    void create_Returns200() throws Exception {
        // Arrange
        when(service.create(any(SpecialtyRequestDTO.class))).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/specialties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MOCK_ID))
                .andExpect(jsonPath("$.name").value("Manicure"));
    }

    @Test
    @DisplayName("POST /specialties - Should return 400 Bad Request when name is blank")
    void create_Returns400_WhenNameIsBlank() throws Exception {
        // Arrange:
        SpecialtyRequestDTO invalidRequest = new SpecialtyRequestDTO("");

        // Act & Assert
        mockMvc.perform(post("/specialties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ==================== FIND ALL TESTS ====================

    @Test
    @DisplayName("GET /specialties - Should return 200 OK and list of specialties")
    void findAll_Returns200() throws Exception {
        // Arrange
        SpecialtyResponseDTO spec2 = new SpecialtyResponseDTO("id-2", "Cabeleireira");
        when(service.findAll()).thenReturn(List.of(responseDTO, spec2));

        // Act & Assert
        mockMvc.perform(get("/specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("Manicure"))
                .andExpect(jsonPath("$[1].name").value("Cabeleireira"));
    }

    // ==================== FIND BY ID TESTS ====================

    @Test
    @DisplayName("GET /specialties/{id} - Should return 200 OK and the specialty")
    void findById_Returns200() throws Exception {
        // Arrange
        when(service.findById(MOCK_ID)).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(get("/specialties/{id}", MOCK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MOCK_ID))
                .andExpect(jsonPath("$.name").value("Manicure"));
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("PUT /specialties/{id} - Should return 200 OK and updated specialty")
    void update_Returns200() throws Exception {
        // Arrange
        SpecialtyRequestDTO updateReq = new SpecialtyRequestDTO("Esteticista");
        SpecialtyResponseDTO updateRes = new SpecialtyResponseDTO(MOCK_ID, "Esteticista");

        when(service.update(eq(MOCK_ID), any(SpecialtyRequestDTO.class))).thenReturn(updateRes);

        // Act & Assert
        mockMvc.perform(put("/specialties/{id}", MOCK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Esteticista"));
    }

    @Test
    @DisplayName("PUT /specialties/{id} - Should return 400 Bad Request when name is blank")
    void update_Returns400_WhenNameIsBlank() throws Exception {
        // Arrange:
        SpecialtyRequestDTO invalidRequest = new SpecialtyRequestDTO("   ");

        // Act & Assert
        mockMvc.perform(put("/specialties/{id}", MOCK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("DELETE /specialties/{id} - Should return 204 No Content")
    void delete_Returns204() throws Exception {
        // Arrange
        doNothing().when(service).delete(MOCK_ID);

        // Act & Assert
        mockMvc.perform(delete("/specialties/{id}", MOCK_ID))
                .andExpect(status().isNoContent());
    }
}