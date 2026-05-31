package sodresoftwares.homebeauty.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("UserRepository - Query tests")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // 1. Creating test users with strategic data to test the query
         user1 = User.builder()
                .firstName("Ney")
                .lastName("Junior")
                .login("ney.dev@test.com")
                .password("123456")
                .role(UserRole.USER)
                .build();

         user2 = User.builder()
                .firstName("Maria")
                .lastName("Silva")
                .login("maria.silva@test.com")
                .password("123456")
                .role(UserRole.PROFESSIONAL)
                .build();

         user3 = User.builder()
                .firstName("Joao")
                .lastName("Junior")
                .login("joao.new@test.com")
                .password("123456")
                .role(UserRole.USER)
                .build();

        userRepository.saveAll(List.of(user1, user2, user3));
    }

    @Test
    @DisplayName("Should find user by first name ignoring case and using partial words")
    void searchUsersByFirstName() {
        // Passing "DANI" (uppercase) to ensure the database ignores the case
        List<User> result = userRepository.searchUsers("NEY");

        assertThat(result).hasSize(1);
        assertThat(result)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
                .containsExactlyInAnyOrder(user1);
    }

    @Test
    @DisplayName("Should find multiple users by last name")
    void searchUsersByLastName() {
        // Passing "sodre" to ensure it finds both Daniel and Joao
        List<User> result = userRepository.searchUsers("junior");

        assertThat(result).hasSize(2);
        assertThat(result)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
                .containsExactlyInAnyOrder(user1, user3);
    }

    @Test
    @DisplayName("Should find user by a partial email (login)")
    void searchUsersByLoginPartial() {
        List<User> result = userRepository.searchUsers("maria.silva");

        assertThat(result).hasSize(1);
        assertThat(result)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
                .containsExactlyInAnyOrder(user2);
    }

    @Test
    @DisplayName("Should return an empty list when the term is not found in any field")
    void searchUsersNotFound() {
        List<User> result = userRepository.searchUsers("xpto123");

        assertThat(result).isEmpty();
    }
}