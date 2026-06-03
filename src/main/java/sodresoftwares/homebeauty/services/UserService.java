package sodresoftwares.homebeauty.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.dto.CompleteUserProfileDTO;
import sodresoftwares.homebeauty.dto.UpdateUserFieldsDTO;
import sodresoftwares.homebeauty.dto.UserResponseDTO;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.repositories.UserRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User completeUserProfile(String userId, CompleteUserProfileDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPhone(dto.phone());
        user.setCpf(dto.cpf());
        user.setBirthDate(dto.birthDate());
        user.setGender(dto.gender());

        return userRepository.save(user);
    }

    @Transactional
    public User partialUpdate(String userId, UpdateUserFieldsDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (dto.firstName() != null && !dto.firstName().trim().isEmpty()) {
            user.setFirstName(dto.firstName().trim());
        }
        if (dto.lastName() != null && !dto.lastName().trim().isEmpty()) {
            user.setLastName(dto.lastName().trim());
        }
        if (dto.phone() != null && !dto.phone().trim().isEmpty()) {
            user.setPhone(dto.phone().trim());
        }
        if (dto.gender() != null && !dto.gender().trim().isEmpty()) {
            user.setGender(dto.gender().trim());
        }

        return userRepository.save(user);
    }

    public List<UserResponseDTO> searchUsers(String query) {
        return userRepository.searchUsers(query)
                .stream()
                .map(user -> new UserResponseDTO(user.getId(), user.getFullName(), user.getLogin(), user.getRole()))
                .toList();
    }
}
