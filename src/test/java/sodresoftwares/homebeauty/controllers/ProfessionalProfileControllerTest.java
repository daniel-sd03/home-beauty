package sodresoftwares.homebeauty.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sodresoftwares.homebeauty.dto.ProfessionalOnboardingDTO;
import sodresoftwares.homebeauty.dto.UpdateProfessionalProfileDTO;
import sodresoftwares.homebeauty.infra.security.SecurityFilter;
import sodresoftwares.homebeauty.model.ProfessionalProfile;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;
import sodresoftwares.homebeauty.services.ProfessionalProfileService;

import java.time.LocalDate;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    private ProfessionalProfileService service;

    private ProfessionalProfile mockProfile;

    @BeforeEach
    void setUp() {
        User loggedInUser = User.builder()
                .id("123")
                .firstName("Daniel")
                .lastName("Sodre")
                .login("daniel@test.com")
                .role(UserRole.PROFESSIONAL)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loggedInUser, null, loggedInUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockProfile = ProfessionalProfile.builder()
                .id("prof-999")
                .user(loggedInUser)
                .description("Excelente profissional")
                .whatsapp("11999999999")
                .instagramHandle("danielsodre")
                .serviceRadiusKm(10)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== POST /professionals/me/onboarding ====================

    @Test
    @DisplayName("Should return 200 OK when onboarding with valid data (Success)")
    void onboardProfileSuccess() throws Exception {
        ProfessionalOnboardingDTO dto = new ProfessionalOnboardingDTO(
                "11888888888", "12345678909", LocalDate.of(1990, 1, 1), "Male",
                "Excelente profissional", "11999999999", "danielsodre", 10, Set.of("spec-1")
        );

        when(service.onboardProfessional(eq("123"), any(ProfessionalOnboardingDTO.class))).thenReturn(mockProfile);

        mockMvc.perform(post("/professionals/profile/me/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prof-999"))
                .andExpect(jsonPath("$.description").value("Excelente profissional"))
                .andExpect(jsonPath("$.instagramHandle").value("danielsodre"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when onboarding misses mandatory fields (Validation Error)")
    void onboardProfileValidationError() throws Exception {
        ProfessionalOnboardingDTO invalidDto = new ProfessionalOnboardingDTO(
                null, "cpf-invalido", null, null,
                null, null, null, 0, Set.of()
        );

        mockMvc.perform(post("/professionals/profile/me/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    // ==================== PATCH /professionals/me ====================

    @Test
    @DisplayName("Should return 200 OK when performing partial update (Success)")
    void updateProfileSuccess() throws Exception {
        UpdateProfessionalProfileDTO dto = new UpdateProfessionalProfileDTO(
                "Nova descrição", null, "@novo_insta", null
        );

        ProfessionalProfile updatedProfile = ProfessionalProfile.builder()
                .id("prof-999")
                .description("Nova descrição")
                .instagramHandle("novo_insta")
                .build();

        when(service.partialUpdate(eq("123"), any(UpdateProfessionalProfileDTO.class))).thenReturn(updatedProfile);

        mockMvc.perform(patch("/professionals/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Nova descrição"))
                .andExpect(jsonPath("$.instagramHandle").value("novo_insta"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when phone format is invalid on update (Validation Error)")
    void updateProfileValidationError() throws Exception {
        UpdateProfessionalProfileDTO invalidDto = new UpdateProfessionalProfileDTO(
                null, "123", null, null
        );

        mockMvc.perform(patch("/professionals/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }
}