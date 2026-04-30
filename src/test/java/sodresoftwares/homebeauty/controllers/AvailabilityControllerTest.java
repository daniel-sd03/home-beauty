package sodresoftwares.homebeauty.controllers;

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
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.infra.security.SecurityFilter;
import sodresoftwares.homebeauty.services.AvailabilityService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AvailabilityController
 *
 * Focuses strictly on HTTP layer interactions: parameter parsing,
 * status code mapping, and JSON serialization/deserialization.
 */
@WebMvcTest(
        controllers = AvailabilityController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AvailabilityController Tests")
class AvailabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvailabilityService availabilityService;

    private LocalDate testDate;
    private List<LocalDateTime> availableSlots;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now().plusDays(1);
        availableSlots = List.of(
                testDate.atTime(9, 0),
                testDate.atTime(9, 30)
        );
    }

    @Test
    @DisplayName("Should return available slots successfully (HTTP 200)")
    void testGetAvailableSlots_Success() throws Exception {
        // Arrange
        when(availabilityService.getAvailableSlots("prof-123", "serv-456", testDate))
                .thenReturn(availableSlots);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        // Act & Assert
        mockMvc.perform(get("/availability/{professionalId}", "prof-123")
                        .param("serviceId", "serv-456")
                        .param("date", testDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]", is(testDate.atTime(9, 0).format(formatter))))
                .andExpect(jsonPath("$[1]", is(testDate.atTime(9, 30).format(formatter))));
    }

    @Test
    @DisplayName("Should return empty list when no slots available (HTTP 200)")
    void testGetAvailableSlots_EmptySlots() throws Exception {
        // Arrange
        when(availabilityService.getAvailableSlots("prof-123", "serv-456", testDate))
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/availability/{professionalId}", "prof-123")
                        .param("serviceId", "serv-456")
                        .param("date", testDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return 400 Bad Request for missing required parameters")
    void testGetAvailableSlots_MissingParameters() throws Exception {
        // Act & Assert - Missing serviceId param
        mockMvc.perform(get("/availability/{professionalId}", "prof-123")
                        .param("date", testDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for invalid date format")
    void testGetAvailableSlots_InvalidDateFormat() throws Exception {
        // Act & Assert - "not-a-date" cannot be parsed to LocalDate
        mockMvc.perform(get("/availability/{professionalId}", "prof-123")
                        .param("serviceId", "serv-456")
                        .param("date", "not-a-date")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when Service throws validation error")
    void testGetAvailableSlots_ServiceThrowsBadRequest() throws Exception {
        // Arrange
        when(availabilityService.getAvailableSlots("prof-123", "serv-456", testDate))
                .thenThrow(new ResponseStatusException(BAD_REQUEST, "Cannot fetch availability for past dates."));

        // Act & Assert
        mockMvc.perform(get("/availability/{professionalId}", "prof-123")
                        .param("serviceId", "serv-456")
                        .param("date", testDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}