package sodresoftwares.homebeauty.services;

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
import sodresoftwares.homebeauty.infra.security.TokenService;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;
import sodresoftwares.homebeauty.repositories.ProfessionalProfileRepository;
import sodresoftwares.homebeauty.repositories.UserRepository;


@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final ProfessionalProfileRepository profileRepository;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       TokenService tokenService, PasswordEncoder passwordEncoder, ProfessionalProfileRepository profileRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.profileRepository = profileRepository;
    }

    public LoginResponseDTO login(AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());

        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((User) auth.getPrincipal());

        return new LoginResponseDTO(token);
    }

    @Transactional
    public void register(RegisterDTO data) {
        // Check if user already exists
        if (this.userRepository.findByLogin(data.login()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        // Create user
        String encryptedPassword = passwordEncoder.encode(data.password());
        User newUser = User.builder()
                .login(data.login())
                .password(encryptedPassword)
                .role(UserRole.USER)
                .name(data.name())
                .phone(data.phone())
                .build();

        this.userRepository.save(newUser);
    }

    @Transactional
    public void promoteToAdmin(String userId) {
        // search user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        //set role
        user.setRole(UserRole.ADMIN);

        userRepository.save(user);
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
    }
}