package sodresoftwares.homebeauty.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sodresoftwares.homebeauty.dto.ProfessionalUpgradeDTO;
import sodresoftwares.homebeauty.services.ProfessionalProfileService;

@RestController
@RequestMapping("/professionals/profile")
public class ProfessionalProfileController {

    private final ProfessionalProfileService professionalService;

    public ProfessionalProfileController(ProfessionalProfileService professionalService) {
        this.professionalService = professionalService;
    }

    @PostMapping("/upgrade")
    public ResponseEntity<Void> upgradeToProfessional(@RequestBody @Valid ProfessionalUpgradeDTO data) {
        professionalService.upgradeToProfessional(data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}