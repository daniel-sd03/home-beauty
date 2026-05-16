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
import sodresoftwares.homebeauty.dto.ProfessionalBlockDTO;
import sodresoftwares.homebeauty.dto.ProfessionalBlockResponseDTO;
import sodresoftwares.homebeauty.dto.ProvidedServiceDTO;
import sodresoftwares.homebeauty.dto.WorkingHourDTO;
import sodresoftwares.homebeauty.enums.ServiceLocationType;
import sodresoftwares.homebeauty.infra.security.SecurityFilter;
import sodresoftwares.homebeauty.services.ProfessionalCatalogService;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProfessionalCatalogController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProfessionalCatalogController Tests")
class ProfessionalCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProfessionalCatalogService catalogService;

    private ProvidedServiceDTO providedServiceDTO;
    private WorkingHourDTO workingHourDTO;
    private ProfessionalBlockDTO blockDTO;
    private ProfessionalBlockResponseDTO blockResponseDTO;

    @BeforeEach
    void setUp() {
        providedServiceDTO = new ProvidedServiceDTO(
                null, "Hair Cut", "Professional hair cut", ServiceLocationType.CLIENT_LOCATION_ONLY,
                BigDecimal.valueOf(50.0), 30, "cat-hair", List.of("img.jpg")
        );

        workingHourDTO = new WorkingHourDTO(
                null, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)
        );

        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime endDate = startDate.plusDays(5).withNano(0);

        blockDTO = new ProfessionalBlockDTO("Vacation", startDate, endDate);
        blockResponseDTO = new ProfessionalBlockResponseDTO("block-123", "Vacation", startDate, endDate);
    }

    // ==================== PROVIDED SERVICE TESTS ====================

    @Test
    @DisplayName("Should add service successfully (HTTP 201)")
    void testAddService_Success() throws Exception {
        mockMvc.perform(post("/professionals/catalog/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(providedServiceDTO)))
                .andExpect(status().isCreated());

        verify(catalogService).addProvidedService(any(ProvidedServiceDTO.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when service DTO is invalid")
    void testAddService_ValidationErrors() throws Exception {
        ProvidedServiceDTO invalidDTO = new ProvidedServiceDTO(
                null, "", "Desc", null, BigDecimal.valueOf(-10), 0, "", null
        );

        mockMvc.perform(post("/professionals/catalog/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(catalogService);
    }

    @Test
    @DisplayName("Should get my services successfully (HTTP 200)")
    void testGetMyServices_Success() throws Exception {
        when(catalogService.getMyProvidedServices()).thenReturn(List.of(providedServiceDTO));

        mockMvc.perform(get("/professionals/catalog/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Hair Cut")));
    }

    @Test
    @DisplayName("Should update service successfully (HTTP 204)")
    void testUpdateService_Success() throws Exception {
        mockMvc.perform(put("/professionals/catalog/services/serv-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(providedServiceDTO)))
                .andExpect(status().isNoContent());

        verify(catalogService).updateService(eq("serv-123"), any(ProvidedServiceDTO.class));
    }

    @Test
    @DisplayName("Should delete service successfully (HTTP 204)")
    void testDeleteService_Success() throws Exception {
        mockMvc.perform(delete("/professionals/catalog/services/serv-123"))
                .andExpect(status().isNoContent());

        verify(catalogService).deleteService("serv-123");
    }

    // ==================== WORKING HOUR TESTS ====================

    @Test
    @DisplayName("Should add working hour successfully (HTTP 201)")
    void testAddWorkingHour_Success() throws Exception {
        mockMvc.perform(post("/professionals/catalog/working-hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workingHourDTO)))
                .andExpect(status().isCreated());

        verify(catalogService).addWorkingHour(any(WorkingHourDTO.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when working hour DTO is invalid")
    void testAddWorkingHour_ValidationErrors() throws Exception {
        // Passando null para campos obrigatórios (simulando um JSON quebrado)
        WorkingHourDTO invalidDTO = new WorkingHourDTO(
                null, null, null, LocalTime.of(17, 0)
        );

        mockMvc.perform(post("/professionals/catalog/working-hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        // Garante que a requisição não passou da barreira do Controller
        verifyNoInteractions(catalogService);
    }

    @Test
    @DisplayName("Should get my working hours successfully (HTTP 200)")
    void testGetMyWorkingHours_Success() throws Exception {
        when(catalogService.getMyWorkingHours()).thenReturn(List.of(workingHourDTO));

        mockMvc.perform(get("/professionals/catalog/working-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].dayOfWeek", is("MONDAY")));
    }

    @Test
    @DisplayName("Should update working hour successfully (HTTP 204)")
    void testUpdateWorkingHour_Success() throws Exception {
        mockMvc.perform(put("/professionals/catalog/working-hours/wh-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workingHourDTO)))
                .andExpect(status().isNoContent());

        verify(catalogService).updateWorkingHour(eq("wh-123"), any(WorkingHourDTO.class));
    }

    @Test
    @DisplayName("Should delete working hour successfully (HTTP 204)")
    void testDeleteWorkingHour_Success() throws Exception {
        mockMvc.perform(delete("/professionals/catalog/working-hours/wh-123"))
                .andExpect(status().isNoContent());

        verify(catalogService).deleteWorkingHour("wh-123");
    }

    // ==================== BLOCK TESTS ====================

    @Test
    @DisplayName("Should create block successfully (HTTP 201)")
    void testCreateBlock_Success() throws Exception {
        mockMvc.perform(post("/professionals/catalog/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockDTO)))
                .andExpect(status().isCreated());

        verify(catalogService).createBlock(any(ProfessionalBlockDTO.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when block DTO is invalid")
    void testCreateBlock_ValidationErrors() throws Exception {
        ProfessionalBlockDTO invalidDTO = new ProfessionalBlockDTO("", null, null);

        mockMvc.perform(post("/professionals/catalog/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(catalogService);
    }

    @Test
    @DisplayName("Should get my blocks successfully (HTTP 200)")
    void testGetMyBlocks_Success() throws Exception {
        when(catalogService.getMyBlocks()).thenReturn(List.of(blockResponseDTO));

        mockMvc.perform(get("/professionals/catalog/blocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("block-123")))
                .andExpect(jsonPath("$[0].title", is("Vacation")));
    }
}