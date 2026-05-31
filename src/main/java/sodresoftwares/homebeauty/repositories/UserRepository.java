package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import sodresoftwares.homebeauty.model.user.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {
    UserDetails findByLogin(String login);
    boolean existsByLogin(String login);

    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.login) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchUsers(@Param("query") String query);
}