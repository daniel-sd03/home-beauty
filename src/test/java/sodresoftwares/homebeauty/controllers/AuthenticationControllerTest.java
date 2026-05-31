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
import sodresoftwares.homebeauty.dto.*;
import sodresoftwares.homebeauty.infra.security.SecurityFilter;
import sodresoftwares.homebeauty.model.user.UserRole;
import sodresoftwares.homebeauty.services.AuthService;

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

    private AuthenticationDTO authenticationDTO;
    private LoginResponseDTO loginResponseDTO;
    private RegisterDTO registerDTO;
    private VerifyCodeDTO verifyCodeDTO;
    private ResendCodeDTO resendCodeDTO;

    @BeforeEach
    void setUp() {
        authenticationDTO = new AuthenticationDTO("user@test.com", "password123");
        loginResponseDTO = new LoginResponseDTO("jwt-token-example", UserRole.USER, true);
        registerDTO = new RegisterDTO("user@test.com", "password123", "John", "Doe", UserRole.USER);
        verifyCodeDTO = new VerifyCodeDTO("user@test.com", "123456");
        resendCodeDTO = new ResendCodeDTO("user@test.com");
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
    @DisplayName("Should register new user successfully (HTTP 201)")
    void testRegister_Success() throws Exception {
        doNothing().when(authService).register(any(RegisterDTO.class));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated());

        verify(authService).register(any(RegisterDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when register fields are blank")
    void testRegister_ValidationErrors() throws Exception {
        RegisterDTO invalidDTO = new RegisterDTO("", "", "", "", null);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // ==================== VERIFY ACCOUNT TESTS ====================

    @Test
    @DisplayName("Should verify account successfully (HTTP 200)")
    void testVerifyAccount_Success() throws Exception {
        doNothing().when(authService).verifyAccount(any(VerifyCodeDTO.class));

        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyCodeDTO)))
                .andExpect(status().isOk());

        verify(authService).verifyAccount(any(VerifyCodeDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when verify fields are invalid")
    void testVerifyAccount_ValidationErrors() throws Exception {
        VerifyCodeDTO invalidDTO = new VerifyCodeDTO("", "");

        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // ==================== RESEND CODE TESTS ====================

    @Test
    @DisplayName("Should resend verification code successfully (HTTP 200)")
    void testResendCode_Success() throws Exception {
        doNothing().when(authService).resendVerificationCode(anyString());

        mockMvc.perform(post("/auth/resend-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendCodeDTO)))
                .andExpect(status().isOk());

        verify(authService).resendVerificationCode(resendCodeDTO.login());
    }

    @Test
    @DisplayName("Should return 400 when resend code email is invalid")
    void testResendCode_ValidationErrors() throws Exception {
        ResendCodeDTO invalidDTO = new ResendCodeDTO("email-invalido");

        mockMvc.perform(post("/auth/resend-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
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