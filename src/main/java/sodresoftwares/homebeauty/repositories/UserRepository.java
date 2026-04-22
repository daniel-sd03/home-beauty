package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import sodresoftwares.homebeauty.model.user.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {
    UserDetails findByLogin(String login);
    List<User> findByNameContainingIgnoreCaseOrLoginContainingIgnoreCase(String name, String login);
}