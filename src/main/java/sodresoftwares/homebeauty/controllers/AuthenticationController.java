package sodresoftwares.homebeauty.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.homebeauty.dto.AuthenticationDTO;
import sodresoftwares.homebeauty.dto.LoginResponseDTO;
import sodresoftwares.homebeauty.dto.ProfessionalRegisterDTO;
import sodresoftwares.homebeauty.dto.RegisterDTO;
import sodresoftwares.homebeauty.services.AuthService;
import sodresoftwares.homebeauty.services.ProfessionalProfileService;


@RestController
@RequestMapping("auth")
public class AuthenticationController {

	private final AuthService authService;
	private final ProfessionalProfileService professionalProfileService;

	public AuthenticationController(AuthService authService, ProfessionalProfileService professionalProfileService) {
		this.authService = authService;
		this.professionalProfileService = professionalProfileService;
	}
	
	@PostMapping("/login")
	public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data) {
		LoginResponseDTO response = authService.login(data);
		return ResponseEntity.ok(response);
	}
	
	@PostMapping("/register")
	public ResponseEntity<Void> register(@RequestBody @Valid RegisterDTO data ) {
		 authService.register(data);
		 return ResponseEntity.ok().build();
	}

	@PostMapping("/register/professional")
	public ResponseEntity<Void> registerNewProfessional(@RequestBody @Valid ProfessionalRegisterDTO data) {
		professionalProfileService.registerNewProfessional(data);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@PatchMapping("/{id}/role/admin")
	public ResponseEntity<Void> promoteToAdmin(@PathVariable String id) {
		authService.promoteToAdmin(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/role/demote")
	public ResponseEntity<Void> demoteFromAdmin(@PathVariable String id) {
		authService.demoteFromAdmin(id);
		return ResponseEntity.noContent().build();
	}
}
