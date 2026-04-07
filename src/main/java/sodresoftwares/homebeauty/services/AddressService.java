package sodresoftwares.homebeauty.services;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.dto.AddressDTO;
import sodresoftwares.homebeauty.model.Address;
import sodresoftwares.homebeauty.model.City;
import sodresoftwares.homebeauty.model.State;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.repositories.AddressRepository;
import sodresoftwares.homebeauty.repositories.CityRepository;
import sodresoftwares.homebeauty.repositories.StateRepository;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final CityRepository cityRepository;
    private final StateRepository stateRepository;

    public AddressService(AddressRepository addressRepository, CityRepository cityRepository, StateRepository stateRepository) {
        this.addressRepository = addressRepository;
        this.cityRepository = cityRepository;
        this.stateRepository = stateRepository;
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private City resolveCityAndState(String cityName, String stateUf, String stateName) {
        State state = stateRepository.findByUfIgnoreCase(stateUf)
                .orElseGet(() -> stateRepository.save(
                        State.builder().uf(stateUf.toUpperCase()).name(stateName).build()
                ));

        return cityRepository.findByNameIgnoreCaseAndStateId(cityName, state.getId())
                .orElseGet(() -> cityRepository.save(
                        City.builder().name(cityName).state(state).build()
                ));
    }

    @Transactional
    public void createAddress(AddressDTO data) {
        User user = getCurrentUser();
        City city = resolveCityAndState(data.city(), data.stateUf(), data.stateName());

        Address address = Address.builder()
                .street(data.street())
                .number(data.number())
                .complement(data.complement())
                .neighborhood(data.neighborhood())
                .zipCode(data.zipCode())
                .city(city)
                .user(user)
                .build();

        addressRepository.save(address);
    }

    public List<AddressDTO> getMyAddresses() {
        User user = getCurrentUser();

        // Fetch addresses for the logged-in user and map them to DTOs
        return addressRepository.findByUserId(user.getId()).stream()
                .map(address -> new AddressDTO(
                        address.getId(),
                        address.getStreet(),
                        address.getNumber(),
                        address.getComplement(),
                        address.getNeighborhood(),
                        address.getZipCode(),
                        address.getCity().getName(),
                        address.getCity().getState().getUf(),
                        address.getCity().getState().getName()
                ))
                .toList();
    }

    @Transactional
    public void updateAddress(String addressId, AddressDTO data) {
        User user = getCurrentUser();

        // 1. Find the address
        Address existingAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        // 2. SECURITY: Verify if the address belongs to the logged-in user
        if (!existingAddress.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to edit this address");
        }

        // 3. Resolve the city and state (in case the user moved to another city/state)
        City city = resolveCityAndState(data.city(), data.stateUf(), data.stateName());

        // 4. Update the fields
        existingAddress.setStreet(data.street());
        existingAddress.setNumber(data.number());
        existingAddress.setComplement(data.complement());
        existingAddress.setNeighborhood(data.neighborhood());
        existingAddress.setZipCode(data.zipCode());
        existingAddress.setCity(city);

        // 5. Save changes
        addressRepository.save(existingAddress);
    }
}