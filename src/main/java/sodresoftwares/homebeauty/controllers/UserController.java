package sodresoftwares.homebeauty.controllers;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.homebeauty.dto.CompleteUserProfileDTO;
import sodresoftwares.homebeauty.dto.UpdateUserFieldsDTO;
import sodresoftwares.homebeauty.dto.UserResponseDTO;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.services.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/me/profile")
    public ResponseEntity<UserResponseDTO> completeMyProfile(
            @RequestBody @Valid CompleteUserProfileDTO dto,
            @AuthenticationPrincipal User loggedInUser) {

        User updatedUser = userService.completeUserProfile(loggedInUser.getId(), dto);

        return ResponseEntity.ok(new UserResponseDTO(
                updatedUser.getId(),
                updatedUser.getFullName(),
                updatedUser.getLogin(),
                updatedUser.getRole()
        ));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMyProfile(
            @RequestBody @Valid UpdateUserFieldsDTO dto,
            @AuthenticationPrincipal User loggedInUser) {

        User updatedUser = userService.partialUpdate(loggedInUser.getId(), dto);

        return ResponseEntity.ok(new UserResponseDTO(
                updatedUser.getId(),
                updatedUser.getFullName(),
                updatedUser.getLogin(),
                updatedUser.getRole()
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponseDTO>> searchUsers(@RequestParam String name) {
        var users = userService.searchUsers(name);
        return ResponseEntity.ok(users);
    }
}
