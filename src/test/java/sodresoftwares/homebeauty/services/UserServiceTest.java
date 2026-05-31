package sodresoftwares.homebeauty.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sodresoftwares.homebeauty.dto.UserResponseDTO;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;
import sodresoftwares.homebeauty.repositories.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should search users and map them to UserResponseDTO")
    void shouldSearchAndMapUsersSuccessfully() {
        // Arrange
        String query = "test";
        User user = User.builder()
                .id("123")
                .firstName("Test")
                .lastName("User")
                .login("test@test.com")
                .role(UserRole.USER)
                .build();

        when(userRepository.searchUsers(query))
                .thenReturn(List.of(user));

        // Act
        List<UserResponseDTO> result = userService.searchUsers(query);

        // Assert
        assertThat(result).hasSize(1);
        UserResponseDTO dto = result.get(0);
        assertThat(dto.id()).isEqualTo("123");
        assertThat(dto.name()).isEqualTo("Test User");
        assertThat(dto.login()).isEqualTo("test@test.com");
        assertThat(dto.role()).isEqualTo(UserRole.USER);

        verify(userRepository).searchUsers(query);
    }
}