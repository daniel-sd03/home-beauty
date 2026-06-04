package sodresoftwares.homebeauty.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.dto.AuthenticationDTO;
import sodresoftwares.homebeauty.dto.LoginResponseDTO;
import sodresoftwares.homebeauty.dto.RegisterDTO;
import sodresoftwares.homebeauty.dto.VerifyCodeDTO;
import sodresoftwares.homebeauty.infra.security.TokenService;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;
import sodresoftwares.homebeauty.repositories.ProfessionalProfileRepository;
import sodresoftwares.homebeauty.repositories.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Service
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final ProfessionalProfileRepository profileRepository;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       TokenService tokenService, PasswordEncoder passwordEncoder, ProfessionalProfileRepository profileRepository, EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.profileRepository = profileRepository;
        this.emailService = emailService;
    }

    public LoginResponseDTO login(AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        User loggedUser = (User) auth.getPrincipal();
        var token = tokenService.generateToken(loggedUser);

        boolean isProfileComplete = switch (loggedUser.getRole()) {
            case PROFESSIONAL -> profileRepository.findByUserId(loggedUser.getId()).isPresent();
            case USER -> loggedUser.hasCompleteUserProfile();
            case ADMIN -> true;
        };

        if (!isProfileComplete) {
            log.warn("User ID {} logged in with incomplete profile. Role: {}", loggedUser.getId(), loggedUser.getRole());
        } else {
            log.info("User ID {} authenticated successfully", loggedUser.getId());
        }

        return new LoginResponseDTO(token, loggedUser.getRole(), isProfileComplete);
    }

    @Transactional
    public void register(RegisterDTO data) {
        // Check if user already exists
        if (this.userRepository.existsByLogin(data.login())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        // Encrypt the password
        String encryptedPassword = passwordEncoder.encode(data.password());

        // Generate a random 6-digit verification code
        String code = generateVerificationCode();

        // Ensure only USER or PROFESSIONAL roles can be assigned at registration
        UserRole assignedRole = data.role();
        if (assignedRole != UserRole.USER && assignedRole != UserRole.PROFESSIONAL) {
            assignedRole = UserRole.USER;
        }

        User newUser = User.builder()
                .firstName(data.firstName())
                .lastName(data.lastName())
                .login(data.login())
                .password(encryptedPassword)
                .role(assignedRole)
                .isActive(false)
                .verificationCode(code)
                .verificationCodeExpiry(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10))
                .build();

        this.userRepository.save(newUser);


        log.info("New user registered and verification code generated: ID {}", newUser.getId());
        // Send verification email
        emailService.sendVerificationCode(newUser.getLogin(), newUser.getFirstName(), code);
    }

    @Transactional
    public void verifyAccount(VerifyCodeDTO data) {
        User user = (User) userRepository.findByLogin(data.login());

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (user.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is already active");
        }

        if (!data.code().equals(user.getVerificationCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
        }

        if (LocalDateTime.now(ZoneOffset.UTC).isAfter(user.getVerificationCodeExpiry())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code expired");
        }

        // Activate account
        user.setActive(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);

        userRepository.save(user);

        log.info("Account successfully activated for user ID: {}", user.getId());
    }

    @Transactional
    public void resendVerificationCode(String login) {
        // Find the user in the database
        User user = (User) userRepository.findByLogin(login);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
        }

        // Check if the user's account is already activated
        if (user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This account is already activated.");
        }

        // Generate a new verification code
        String newCode = generateVerificationCode();

        // Update the token and reset the expiration time (using UTC to match registration)
        user.setVerificationCode(newCode);
        user.setVerificationCodeExpiry(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));

        userRepository.save(user);

        // Send the new verification code via email
        emailService.sendVerificationCode(user.getLogin(), user.getFirstName(), newCode);

        log.info("A new verification code was generated and sent to user ID: {}", user.getId());
    }

    private String generateVerificationCode() {
        int code = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    @Transactional
    public void promoteToAdmin(String userId) {
        // search user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        //set role
        user.setRole(UserRole.ADMIN);

        userRepository.save(user);

        log.info("User promoted to ADMIN role: ID {}", userId);
    }

    @Transactional
    public void demoteFromAdmin(String userId) {
        //  search user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // check if professional profile exists
        boolean isProfessional = profileRepository.findByUserId(user.getId()).isPresent();

        // set role based on isProfessional
        if (isProfessional) {
            user.setRole(UserRole.PROFESSIONAL);
        } else {
            user.setRole(UserRole.USER);
        }
        userRepository.save(user);

        log.info("User demoted from ADMIN to {} role: ID {}", user.getRole(), userId);
    }
}