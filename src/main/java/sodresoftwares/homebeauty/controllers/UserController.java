package sodresoftwares.homebeauty.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sodresoftwares.homebeauty.dto.UserResponseDTO;
import sodresoftwares.homebeauty.services.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponseDTO>> searchUsers(@RequestParam String name) {
        var users = userService.searchUsers(name);
        return ResponseEntity.ok(users);
    }
}
