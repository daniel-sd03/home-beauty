	package sodresoftwares.homebeauty.controllers;

	import jakarta.validation.Valid;
	import lombok.RequiredArgsConstructor;
	import lombok.extern.slf4j.Slf4j;
	import org.springframework.http.HttpStatus;
	import org.springframework.http.ResponseEntity;
	import org.springframework.web.bind.annotation.*;
	import sodresoftwares.homebeauty.dto.*;
	import sodresoftwares.homebeauty.services.AuthService;

	@RestController
	@Slf4j
	@RequiredArgsConstructor
	@RequestMapping("/auth")
	public class AuthenticationController {

		private final AuthService authService;

		@PostMapping("/login")
		public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data) {
			log.info("Login attempt for {}", data.login());
			LoginResponseDTO response = authService.login(data);
			return ResponseEntity.ok(response);
		}

		@PostMapping("/register")
		public ResponseEntity<Void> register(@RequestBody @Valid RegisterDTO data ) {
			log.info("Registering new user {}", data.login());
			 authService.register(data);
			 return ResponseEntity.status(HttpStatus.CREATED).build();
		}

		@PostMapping("/verify")
		public ResponseEntity<Void> verifyAccount(@RequestBody @Valid VerifyCodeDTO data) {
			log.info("Verifying account {}", data.login());
			authService.verifyAccount(data);
			return ResponseEntity.ok().build();
		}

		@PostMapping("/resend-code")
		public ResponseEntity<Void> resendCode(@RequestBody @Valid ResendCodeDTO data) {
			log.info("Resend verification code for {}", data.login());
			authService.resendVerificationCode(data.login());
			return ResponseEntity.ok().build();
		}

		@PatchMapping("/{id}/role/admin")
		public ResponseEntity<Void> promoteToAdmin(@PathVariable String id) {
			log.info("Promoting user {} to admin", id);
			authService.promoteToAdmin(id);
			return ResponseEntity.noContent().build();
		}

		@PatchMapping("/{id}/role/demote")
		public ResponseEntity<Void> demoteFromAdmin(@PathVariable String id) {
			log.info("Demoting user {} from admin", id);
			authService.demoteFromAdmin(id);
			return ResponseEntity.noContent().build();
		}
	}
