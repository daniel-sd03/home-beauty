package sodresoftwares.homebeauty.services;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.homebeauty.infra.exception.AppException;
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


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessionalProfileService {

    private final ProfessionalProfileRepository profileRepository;
    private final SpecialtyRepository specialtyRepository;
    private final UserService userService;


    @Transactional
    public ProfessionalProfile onboardProfessional(String userId, ProfessionalOnboardingDTO dto) {
        // Validate professional profile
        if (profileRepository.findByUserId(userId).isPresent()) {
            throw new AppException(HttpStatus.CONFLICT, "PROFILE_ALREADY_EXISTS", "Professional profile already exists. Use PATCH to update it.");
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
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SPECIALTY_IDS",
                    "One or more provided specialty IDs are invalid. Expected %d, but found %d."
                            .formatted(dto.specialtyIds().size(), foundSpecialties.size())
            );
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
        ProfessionalProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    return new AppException(HttpStatus.NOT_FOUND, "PROFESSIONAL_PROFILE_NOT_FOUND", "Professional profile not found");
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