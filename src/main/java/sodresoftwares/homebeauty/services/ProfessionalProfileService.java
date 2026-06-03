package sodresoftwares.homebeauty.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.dto.CompleteUserProfileDTO;
import sodresoftwares.homebeauty.dto.ProfessionalOnboardingDTO;
import sodresoftwares.homebeauty.dto.UpdateProfessionalProfileDTO;
import sodresoftwares.homebeauty.model.ProfessionalProfile;
import sodresoftwares.homebeauty.model.Specialty;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.repositories.ProfessionalProfileRepository;
import sodresoftwares.homebeauty.repositories.SpecialtyRepository;

import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
public class ProfessionalProfileService {

    private final ProfessionalProfileRepository profileRepository;
    private final SpecialtyRepository specialtyRepository;
    private final UserService userService;

    public ProfessionalProfileService(
            ProfessionalProfileRepository profileRepository,
            SpecialtyRepository specialtyRepository,
            UserService userService) {
        this.profileRepository = profileRepository;
        this.specialtyRepository = specialtyRepository;
        this.userService = userService;
    }

    @Transactional
    public ProfessionalProfile onboardProfessional(String userId, ProfessionalOnboardingDTO dto) {
        log.info("Starting onboarding process for user ID: {}", userId);

        // Validate professional profile
        if (profileRepository.findByUserId(userId).isPresent()) {
            log.warn("Onboarding blocked: Professional profile already exists for user ID: {}", userId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Professional profile already exists. Use PATCH to update it.");
        }

        // Complete User profile
        CompleteUserProfileDTO userDto = new CompleteUserProfileDTO(
                dto.phone(),
                dto.cpf(),
                dto.birthDate(),
                dto.gender()
        );

        User updatedUser = userService.completeUserProfile(userId, userDto);

        // Validate specialties
        List<Specialty> foundSpecialties = specialtyRepository.findAllById(dto.specialtyIds());
        if (foundSpecialties.size() != dto.specialtyIds().size()) {
            log.warn("Onboarding blocked: Invalid specialty IDs for user ID: {}. Expected {}, found {}",
                    userId, dto.specialtyIds().size(), foundSpecialties.size());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more provided specialty IDs are invalid");
        }

        //creat professional profile
        ProfessionalProfile profile = ProfessionalProfile.builder()
                .user(updatedUser)
                .serviceRadiusKm(dto.serviceRadiusKm())
                .specialties(new HashSet<>(foundSpecialties))
                .build();

        // Only update fields that are provided (non-null and non-empty)
        applyOptionalFields(profile, dto.description(), dto.whatsapp(), dto.instagramHandle());

        ProfessionalProfile savedProfile = profileRepository.save(profile);
        log.info("Successfully onboarded professional profile for user ID: {}", userId);

        return savedProfile;
    }

    @Transactional
    public ProfessionalProfile partialUpdate(String userId, UpdateProfessionalProfileDTO dto) {
        log.info("Starting partial update for professional profile of user ID: {}", userId);

        ProfessionalProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Partial update failed: Professional profile not found for user ID: {}", userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Professional profile not found");
                });

        // Only update fields that are provided (non-null and non-empty)
        applyOptionalFields(profile, dto.description(), dto.whatsapp(), dto.instagramHandle());

        if (dto.serviceRadiusKm() != null) {
            profile.setServiceRadiusKm(dto.serviceRadiusKm());
        }

        ProfessionalProfile updatedProfile = profileRepository.save(profile);
        log.info("Successfully updated professional profile for user ID: {}", userId);

        return updatedProfile;
    }

    private void applyOptionalFields(ProfessionalProfile profile, String description, String whatsapp, String instagramHandle) {
        if (description != null && !description.trim().isEmpty()) {
            profile.setDescription(description.trim());
        }

        if (whatsapp != null && !whatsapp.trim().isEmpty()) {
            profile.setWhatsapp(whatsapp.trim());
        }

        if (instagramHandle != null && !instagramHandle.trim().isEmpty()) {
            profile.setInstagramHandle(instagramHandle.trim());
        }
    }
}