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
import sodresoftwares.homebeauty.dto.AuthenticationDTO;
import sodresoftwares.homebeauty.dto.LoginResponseDTO;
import sodresoftwares.homebeauty.dto.ProfessionalRegisterDTO;
import sodresoftwares.homebeauty.dto.RegisterDTO;
import sodresoftwares.homebeauty.infra.security.SecurityFilter;
import sodresoftwares.homebeauty.services.AuthService;
import sodresoftwares.homebeauty.services.ProfessionalProfileService;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthenticationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthenticationController Tests")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private ProfessionalProfileService professionalProfileService;

    private AuthenticationDTO authenticationDTO;
    private LoginResponseDTO loginResponseDTO;
    private RegisterDTO registerDTO;
    private ProfessionalRegisterDTO professionalRegisterDTO;

    @BeforeEach
    void setUp() {
        authenticationDTO = new AuthenticationDTO("user@test.com", "password123");
        loginResponseDTO = new LoginResponseDTO("jwt-token-example");
        registerDTO = new RegisterDTO("user@test.com", "password123", "John Doe", "11999999999");
        professionalRegisterDTO = new ProfessionalRegisterDTO(
                "professional@test.com", "password123", "Jane Prof", "11988888888", "Expert"
        );
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Should login successfully and return token (HTTP 200)")
    void testLogin_Success() throws Exception {
        when(authService.login(any(AuthenticationDTO.class))).thenReturn(loginResponseDTO);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authenticationDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("jwt-token-example")));

        verify(authService).login(any(AuthenticationDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when login or password are blank")
    void testLogin_ValidationErrors() throws Exception {
        AuthenticationDTO invalidDTO = new AuthenticationDTO("", "");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("Should register new user successfully (HTTP 200)")
    void testRegister_Success() throws Exception {
        doNothing().when(authService).register(any(RegisterDTO.class));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isOk());

        verify(authService).register(any(RegisterDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when register fields are blank")
    void testRegister_ValidationErrors() throws Exception {
        RegisterDTO invalidDTO = new RegisterDTO("", "", "", "");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // ==================== REGISTER PROFESSIONAL TESTS ====================

    @Test
    @DisplayName("Should register new professional successfully (HTTP 201)")
    void testRegisterProfessional_Success() throws Exception {
        doNothing().when(professionalProfileService).registerNewProfessional(any(ProfessionalRegisterDTO.class));

        mockMvc.perform(post("/auth/register/professional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(professionalRegisterDTO)))
                .andExpect(status().isCreated());

        verify(professionalProfileService).registerNewProfessional(any(ProfessionalRegisterDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when professional register fields are blank")
    void testRegisterProfessional_ValidationErrors() throws Exception {
        ProfessionalRegisterDTO invalidDTO = new ProfessionalRegisterDTO("", "", "", "", "");

        mockMvc.perform(post("/auth/register/professional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(professionalProfileService);
    }

    // ==================== ROLE MANAGEMENT TESTS ====================

    @Test
    @DisplayName("Should promote user to admin (HTTP 204)")
    void testPromoteToAdmin_Success() throws Exception {
        doNothing().when(authService).promoteToAdmin("user-123");

        mockMvc.perform(patch("/auth/user-123/role/admin"))
                .andExpect(status().isNoContent());

        verify(authService).promoteToAdmin("user-123");
    }

    @Test
    @DisplayName("Should demote user from admin (HTTP 204)")
    void testDemoteFromAdmin_Success() throws Exception {
        doNothing().when(authService).demoteFromAdmin("user-123");

        mockMvc.perform(patch("/auth/user-123/role/demote"))
                .andExpect(status().isNoContent());

        verify(authService).demoteFromAdmin("user-123");
    }
}