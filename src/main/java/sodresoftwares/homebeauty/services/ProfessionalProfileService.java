package sodresoftwares.homebeauty.services;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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
    public void upgradeToProfessional(ProfessionalUpgradeDTO data) {
        // Get current user from token JWT
        User authenticatedUser = (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        // get current user from database to ensure we have the latest data
        User currentUser = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

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
                .user(currentUser)
                .build();
        profileRepository.save(profile);
    }
}