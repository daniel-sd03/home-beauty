package sodresoftwares.homebeauty.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.dto.CompleteUserProfileDTO;
import sodresoftwares.homebeauty.dto.UpdateUserFieldsDTO;
import sodresoftwares.homebeauty.dto.UserResponseDTO;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;
import sodresoftwares.homebeauty.repositories.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User mockUser;

    private CompleteUserProfileDTO completeDto;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id("123")
                .firstName("Daniel")
                .lastName("Sodre")
                .login("daniel@test.com")
                .role(UserRole.USER)
                .build();

        // Ordem do record: phone, cpf, birthDate, gender
        completeDto = new CompleteUserProfileDTO("11999999999", "12345678909", LocalDate.of(1990, 1, 1), "Male");
    }

    // ==================== COMPLETE PROFILE TESTS ====================

    @Test
    @DisplayName("Should complete user profile successfully")
    void completeUserProfileSuccess() {
        when(userRepository.findById("123")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        User result = userService.completeUserProfile("123", completeDto);

        assertThat(result.getPhone()).isEqualTo("11999999999");
        assertThat(result.getCpf()).isEqualTo("12345678909");
        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(result.getGender()).isEqualTo("Male");

        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("Should throw 404 when trying to complete profile of a non-existent user")
    void completeUserProfileThrowsNotFound() {
        when(userRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.completeUserProfile("999", completeDto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User not found")
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);

        verify(userRepository, never()).save(any());
    }

    // ==================== PARTIAL UPDATE TESTS ====================

    @Test
    @DisplayName("Should perform partial update successfully")
    void partialUpdateSuccess() {
        UpdateUserFieldsDTO dto = new UpdateUserFieldsDTO("Neymar", null, null, "11888888888");

        when(userRepository.findById("123")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        User result = userService.partialUpdate("123", dto);

        assertThat(result.getFirstName()).isEqualTo("Neymar");
        assertThat(result.getLastName()).isEqualTo("Sodre");
        assertThat(result.getPhone()).isEqualTo("11888888888");

        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("Should throw 404 when trying to update non-existent user")
    void partialUpdateThrowsNotFound() {
        UpdateUserFieldsDTO dto = new UpdateUserFieldsDTO("Neymar", null, null, null);

        when(userRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.partialUpdate("999", dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User not found")
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);

        verify(userRepository, never()).save(any());
    }

    // ==================== SEARCH TESTS ====================

    @Test
    @DisplayName("Should search users and return fully mapped DTOs")
    void searchUsersSuccess() {
        when(userRepository.searchUsers("Daniel")).thenReturn(List.of(mockUser));

        List<UserResponseDTO> result = userService.searchUsers("Daniel");

        assertThat(result).hasSize(1);

        UserResponseDTO mappedDto = result.get(0);

        assertThat(mappedDto.id()).isEqualTo("123");
        assertThat(mappedDto.name()).isEqualTo("Daniel Sodre");
        assertThat(mappedDto.login()).isEqualTo("daniel@test.com");
        assertThat(mappedDto.role()).isEqualTo(UserRole.USER);
    }
}