package sodresoftwares.homebeauty.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.homebeauty.dto.AddressDTO;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.services.AddressService;

import java.util.List;

@RestController
@RequestMapping("/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal User loggedInUser,
            @RequestBody @Valid AddressDTO data) {
        addressService.createAddress(loggedInUser, data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<AddressDTO>> getMyAddresses(
            @AuthenticationPrincipal User loggedInUser) {
        var addresses = addressService.getMyAddresses(loggedInUser);
        return ResponseEntity.ok(addresses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateAddress(
            @AuthenticationPrincipal User loggedInUser,
            @PathVariable String id,
            @RequestBody @Valid AddressDTO data) {
        addressService.updateAddress(loggedInUser, id, data);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
