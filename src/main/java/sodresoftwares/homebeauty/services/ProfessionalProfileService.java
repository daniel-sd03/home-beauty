package sodresoftwares.homebeauty.services;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.dto.ProfessionalRegisterDTO;
import sodresoftwares.homebeauty.dto.ProfessionalUpgradeDTO;
import sodresoftwares.homebeauty.model.ProfessionalProfile;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;
import sodresoftwares.homebeauty.repositories.ProfessionalProfileRepository;
import sodresoftwares.homebeauty.repositories.UserRepository;

@Service
public class ProfessionalProfileService {

    private final UserRepository userRepository;
    private final ProfessionalProfileRepository profileRepository;

    public ProfessionalProfileService(UserRepository userRepository, ProfessionalProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public void registerNewProfessional(ProfessionalRegisterDTO data) {
        // Check if user already exists
        if (userRepository.findByLogin(data.login()) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists with this login");
        }

        // Create user
        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        User newUser = User.builder()
                .login(data.login())
                .password(encryptedPassword)
                .role(UserRole.PROFESSIONAL)
                .name(data.name())
                .phone(data.phone())
                .build();
        newUser = userRepository.save(newUser);

        // Create profile
        ProfessionalProfile profile = ProfessionalProfile.builder()
                .description(data.description())
                .homeService(data.isHomeService())
                .user(newUser)
                .build();
        profileRepository.save(profile);
    }

    @Transactional
    public void upgradeToProfessional(ProfessionalUpgradeDTO data) {
        // Get current user from token JWT
        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        // Check if professional profile already exists
        if (profileRepository.findByUserId(currentUser.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has a professional profile");
        }

        // Upgrade role to professional
        currentUser.setRole(UserRole.PROFESSIONAL);
        userRepository.save(currentUser);

        // Create professional profile
        ProfessionalProfile profile = ProfessionalProfile.builder()
                .description(data.description())
                .homeService(data.isHomeService())
                .user(currentUser)
                .build();
        profileRepository.save(profile);
    }
}