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
import sodresoftwares.homebeauty.dto.CompleteUserProfileDTO;
import sodresoftwares.homebeauty.dto.UpdateUserFieldsDTO;
import sodresoftwares.homebeauty.dto.UserResponseDTO;
import sodresoftwares.homebeauty.infra.security.SecurityFilter;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;
import sodresoftwares.homebeauty.services.UserService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController - Unit Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private User loggedInUser;

    @BeforeEach
    void setUp() {
        loggedInUser = User.builder()
                .id("123")
                .firstName("Daniel")
                .lastName("Sodre")
                .login("daniel@test.com")
                .role(UserRole.USER)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loggedInUser, null, loggedInUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== PUT /users/me/profile ====================

    @Test
    @DisplayName("Should return 200 OK when completing profile with valid data (Success)")
    void completeProfileSuccess() throws Exception {
        CompleteUserProfileDTO dto = new CompleteUserProfileDTO("11999999999", "12345678909", LocalDate.of(1990, 1, 1), "Male");

        // O Mock recebe o '123' provando que o @AuthenticationPrincipal funcionou
        when(userService.completeUserProfile(eq("123"), any(CompleteUserProfileDTO.class))).thenReturn(loggedInUser);

        mockMvc.perform(put("/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.name").value("Daniel Sodre"))
                .andExpect(jsonPath("$.login").value("daniel@test.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when completing profile with missing/invalid data (Validation Error)")
    void completeProfileValidationError() throws Exception {
        // Passando CPF inválido, telefone vazio e data nula para forçar o erro do @Valid
        CompleteUserProfileDTO invalidDto = new CompleteUserProfileDTO("", "cpf-invalido", null, " ");

        mockMvc.perform(put("/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    // ==================== PATCH /users/me ====================

    @Test
    @DisplayName("Should return 200 OK when performing partial update (Success)")
    void updateProfileSuccess() throws Exception {
        UpdateUserFieldsDTO dto = new UpdateUserFieldsDTO("Neymar", null, null, "11888888888");

        User updatedUser = User.builder()
                .id("123")
                .firstName("Neymar")
                .lastName("Sodre")
                .login("daniel@test.com")
                .role(UserRole.USER)
                .build();

        when(userService.partialUpdate(eq("123"), any(UpdateUserFieldsDTO.class))).thenReturn(updatedUser);

        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.name").value("Neymar Sodre"))
                .andExpect(jsonPath("$.login").value("daniel@test.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when phone format is invalid (Validation Error)")
    void updateProfileValidationError() throws Exception {
        // Passando um telefone fora do padrão @Pattern (tem que ter 10 ou 11 dígitos, passamos apenas 3)
        UpdateUserFieldsDTO invalidDto = new UpdateUserFieldsDTO("Neymar", null, null, "123");

        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /users/search ====================

    @Test
    @DisplayName("Should return 200 OK with users list (Success)")
    void searchUsersSuccess() throws Exception {
        UserResponseDTO responseDTO = new UserResponseDTO("123", "Daniel Sodre", "daniel@test.com", UserRole.USER);

        when(userService.searchUsers("Daniel")).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/users/search")
                        .param("name", "Daniel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("123"))
                .andExpect(jsonPath("$[0].name").value("Daniel Sodre"))
                .andExpect(jsonPath("$[0].login").value("daniel@test.com"))
                .andExpect(jsonPath("$[0].role").value("USER"));
    }
}