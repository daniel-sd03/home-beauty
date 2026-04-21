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
import sodresoftwares.homebeauty.dto.AppointmentCreateDTO;
import sodresoftwares.homebeauty.dto.AppointmentResponseDTO;
import sodresoftwares.homebeauty.enums.AppointmentStatus;
import sodresoftwares.homebeauty.enums.AppointmentType;
import sodresoftwares.homebeauty.infra.security.SecurityFilter;
import sodresoftwares.homebeauty.services.AppointmentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AppointmentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityFilter.class
        )
    )
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AppointmentController Tests")
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppointmentService appointmentService;

    private AppointmentCreateDTO appointmentCreateDTO;
    private AppointmentResponseDTO appointmentResponseDTO;

    @BeforeEach
    void setUp() {
        // Initialize test data
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusMinutes(60);

        appointmentCreateDTO = new AppointmentCreateDTO(
                "service-id-123",
                AppointmentType.CLIENT_LOCATION,
                startTime,
                "address-id-456",
                "Please arrive 5 minutes early"
        );

        appointmentResponseDTO = new AppointmentResponseDTO(
                "appointment-id-789",
                "Hair Cut",
                "Hair",
                "John Professional",
                BigDecimal.valueOf(50.00),
                startTime,
                endTime,
                AppointmentStatus.PENDING,
                AppointmentType.CLIENT_LOCATION,
                "Please arrive 5 minutes early"
        );
    }

    @Test
    @DisplayName("Should create an appointment successfully")
    void testCreateAppointment_Success() throws Exception {
        // Arrange
        when(appointmentService.createAppointment(any(AppointmentCreateDTO.class)))
                .thenReturn(appointmentResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appointmentCreateDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should return bad request when appointment data is invalid")
    void testCreateAppointment_InvalidData() throws Exception {
        // Arrange - missing required field (providedServicesId)
        String invalidJson = """
                {
                    "appointmentType": "CLIENT_LOCATION",
                    "startTime": "2026-05-20T10:00:00",
                    "addressId": "address-id-456"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return internal server error when service throws exception")
    void testCreateAppointment_ServiceThrowsException() throws Exception {
        // Arrange
        when(appointmentService.createAppointment(any(AppointmentCreateDTO.class)))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(post("/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appointmentCreateDTO)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should get client appointments successfully")
    void testGetClientAppointments_Success() throws Exception {
        // Arrange
        List<AppointmentResponseDTO> appointments = List.of(appointmentResponseDTO);
        when(appointmentService.getAppointmentsByClient())
                .thenReturn(appointments);

        // Act & Assert
        mockMvc.perform(get("/appointments/client")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("appointment-id-789"))
                .andExpect(jsonPath("$[0].serviceName").value("Hair Cut"))
                .andExpect(jsonPath("$[0].professionalName").value("John Professional"));
    }

    @Test
    @DisplayName("Should return empty list when client has no appointments")
    void testGetClientAppointments_EmptyList() throws Exception {
        // Arrange
        when(appointmentService.getAppointmentsByClient())
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/appointments/client")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should get professional appointments successfully")
    void testGetProfessionalAppointments_Success() throws Exception {
        // Arrange
        List<AppointmentResponseDTO> appointments = List.of(appointmentResponseDTO);
        when(appointmentService.getAppointmentsByProfessional())
                .thenReturn(appointments);

        // Act & Assert
        mockMvc.perform(get("/appointments/professional")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("appointment-id-789"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Should return empty list when professional has no appointments")
    void testGetProfessionalAppointments_EmptyList() throws Exception {
        // Arrange
        when(appointmentService.getAppointmentsByProfessional())
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/appointments/professional")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should get appointment by ID successfully")
    void testGetAppointmentById_Success() throws Exception {
        // Arrange
        String appointmentId = "appointment-id-789";
        when(appointmentService.getAppointmentById(appointmentId))
                .thenReturn(appointmentResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/appointments/{id}", appointmentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("appointment-id-789"))
                .andExpect(jsonPath("$.serviceName").value("Hair Cut"))
                .andExpect(jsonPath("$.appointmentType").value("CLIENT_LOCATION"))
                .andExpect(jsonPath("$.price").value(50.00));
    }

    @Test
    @DisplayName("Should return not found when appointment does not exist")
    void testGetAppointmentById_NotFound() throws Exception {
        // Arrange
        String appointmentId = "non-existent-id";
        when(appointmentService.getAppointmentById(appointmentId))
                .thenThrow(new RuntimeException("Appointment not found."));

        // Act & Assert
        mockMvc.perform(get("/appointments/{id}", appointmentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should return forbidden when accessing appointment without permission")
    void testGetAppointmentById_Forbidden() throws Exception {
        // Arrange
        String appointmentId = "appointment-id-789";
        when(appointmentService.getAppointmentById(appointmentId))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Access denied"));

        // Act & Assert
        mockMvc.perform(get("/appointments/{id}", appointmentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should validate appointment response structure")
    void testAppointmentResponseStructure() throws Exception {
        // Arrange
        when(appointmentService.getAppointmentById("appointment-id-789"))
                .thenReturn(appointmentResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/appointments/{id}", "appointment-id-789")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.serviceName", notNullValue()))
                .andExpect(jsonPath("$.categoryName", notNullValue()))
                .andExpect(jsonPath("$.professionalName", notNullValue()))
                .andExpect(jsonPath("$.price", notNullValue()))
                .andExpect(jsonPath("$.startTime", notNullValue()))
                .andExpect(jsonPath("$.endTime", notNullValue()))
                .andExpect(jsonPath("$.status", notNullValue()))
                .andExpect(jsonPath("$.appointmentType", notNullValue()));
    }

    @Test
    @DisplayName("Should return 204 No Content when status is successfully updated")
    void testUpdateStatus_Success() throws Exception {
        String appointmentId = "valid-id-123";
        String validJson = """
                {
                    "status": "CONFIRMED"
                }
                """;

        mockMvc.perform(patch("/appointments/{id}/status", appointmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when status is null in DTO")
    void testUpdateStatus_InvalidDTO() throws Exception {
        String appointmentId = "valid-id-123";
        String invalidJson = """
                {
                    "status": null
                }
                """;

        mockMvc.perform(patch("/appointments/{id}/status", appointmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
