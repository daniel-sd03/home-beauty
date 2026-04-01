package sodresoftwares.homebeauty.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sodresoftwares.homebeauty.dto.AuthenticationDTO;
import sodresoftwares.homebeauty.dto.LoginResponseDTO;
import sodresoftwares.homebeauty.dto.ProfessionalRegisterDTO;
import sodresoftwares.homebeauty.dto.RegisterDTO;
import sodresoftwares.homebeauty.infra.security.TokenService;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.repositories.UserRepository;
import sodresoftwares.homebeauty.services.ProfessionalProfileService;


@RestController
@RequestMapping("auth")	
public class AuthenticationController {

	private final TokenService tokenService;
	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final ProfessionalProfileService professionalProfileService;

	public AuthenticationController(AuthenticationManager authenticationManager, UserRepository userRepository,
			TokenService tokenService, ProfessionalProfileService professionalProfileService) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.tokenService = tokenService;
		this.professionalProfileService = professionalProfileService;
	}
	
	@PostMapping("/login")
	public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data) {
		var usernamePassword = new  UsernamePasswordAuthenticationToken(data.login(), data.password());
		var auth = this.authenticationManager.authenticate(usernamePassword);
		
		var token = tokenService.generateToken((User) auth.getPrincipal());
		
		return ResponseEntity.ok(new LoginResponseDTO(token));
	}
	
	@PostMapping("/register")
	public ResponseEntity<Void> register(@RequestBody @Valid RegisterDTO data ) {
		 if(this.userRepository.findByLogin(data.login()) != null)
			 return ResponseEntity.badRequest().build();
		 
		 String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
		User newUser = User.builder()
				.login(data.login())
				.password(encryptedPassword)
				.role(data.role())
				.name(data.name())
				.phone(data.phone())
				.build();
		
		 this.userRepository.save(newUser);
		 return ResponseEntity.ok().build();
	}

	@PostMapping("/register/professional")
	public ResponseEntity<Void> registerNewProfessional(@RequestBody @Valid ProfessionalRegisterDTO data) {
		professionalProfileService.registerNewProfessional(data);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}
}
