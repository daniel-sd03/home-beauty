package sodresoftwares.homebeauty.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.homebeauty.dto.AddressDTO;
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
    public ResponseEntity<Void> create(@RequestBody @Valid AddressDTO data) {
        addressService.createAddress(data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<AddressDTO>> getMyAddresses() {
        var addresses = addressService.getMyAddresses();
        return ResponseEntity.ok(addresses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateAddress(@PathVariable String id, @RequestBody @Valid AddressDTO data) {
        addressService.updateAddress(id, data);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
