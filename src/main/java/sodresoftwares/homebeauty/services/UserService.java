package sodresoftwares.homebeauty.services;

import org.springframework.stereotype.Service;
import sodresoftwares.homebeauty.dto.UserResponseDTO;
import sodresoftwares.homebeauty.repositories.UserRepository;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponseDTO> searchUsers(String query) {
        return userRepository.findByNameContainingIgnoreCaseOrLoginContainingIgnoreCase(query, query)
                .stream()
                .map(user -> new UserResponseDTO(user.getId(), user.getName(), user.getLogin(), user.getRole()))
                .toList();
    }
}
