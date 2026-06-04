package sodresoftwares.homebeauty.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.homebeauty.dto.ProfessionalOnboardingDTO;
import sodresoftwares.homebeauty.dto.UpdateProfessionalProfileDTO;
import sodresoftwares.homebeauty.model.ProfessionalProfile;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.services.ProfessionalProfileService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/professionals/profile")
public class ProfessionalProfileController {

    private final ProfessionalProfileService service;

    @PostMapping("/me/onboarding")
    public ResponseEntity<ProfessionalProfile> onboardMyProfile(
            @RequestBody @Valid ProfessionalOnboardingDTO dto,
            @AuthenticationPrincipal User loggedInUser) {

        ProfessionalProfile updatedProfile = service.onboardProfessional(loggedInUser.getId(), dto);

        return ResponseEntity.ok(updatedProfile);
    }


    @PatchMapping("/me")
    public ResponseEntity<ProfessionalProfile> updateMyProfile(
            @RequestBody @Valid UpdateProfessionalProfileDTO dto,
            @AuthenticationPrincipal User loggedInUser) {

        ProfessionalProfile updatedProfile = service.partialUpdate(loggedInUser.getId(), dto);

        return ResponseEntity.ok(updatedProfile);
    }
}