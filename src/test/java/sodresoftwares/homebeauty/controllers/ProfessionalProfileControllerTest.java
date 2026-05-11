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
import sodresoftwares.homebeauty.dto.ProfessionalUpgradeDTO;
import sodresoftwares.homebeauty.infra.security.SecurityFilter;
import sodresoftwares.homebeauty.services.ProfessionalProfileService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProfessionalProfileController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProfessionalProfileController Tests")
class ProfessionalProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProfessionalProfileService professionalProfileService;

    private ProfessionalUpgradeDTO upgradeDTO;

    @BeforeEach
    void setUp() {
        upgradeDTO = new ProfessionalUpgradeDTO("Expert in hair styling and treatments");
    }

    @Test
    @DisplayName("Should upgrade user to professional successfully (HTTP 201)")
    void testUpgradeToProfessional_Success() throws Exception {
        // Arrange
        doNothing().when(professionalProfileService).upgradeToProfessional(any(ProfessionalUpgradeDTO.class));

        // Act & Assert
        mockMvc.perform(post("/professionals/profile/upgrade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upgradeDTO)))
                .andExpect(status().isCreated());

        verify(professionalProfileService).upgradeToProfessional(any(ProfessionalUpgradeDTO.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when description is invalid")
    void testUpgradeToProfessional_ValidationErrors() throws Exception {
        ProfessionalUpgradeDTO invalidDTO = new ProfessionalUpgradeDTO("");

        mockMvc.perform(post("/professionals/profile/upgrade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(professionalProfileService);
    }
}