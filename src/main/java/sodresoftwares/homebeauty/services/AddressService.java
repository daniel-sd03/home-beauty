package sodresoftwares.homebeauty.services;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.homebeauty.dto.AddressRequestDTO;
import sodresoftwares.homebeauty.model.Address;
import sodresoftwares.homebeauty.model.City;
import sodresoftwares.homebeauty.model.State;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.repositories.AddressRepository;
import sodresoftwares.homebeauty.repositories.CityRepository;
import sodresoftwares.homebeauty.repositories.StateRepository;

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

    @Transactional
    public Address createAddress(AddressRequestDTO data) {
        //1  get user from Security Context
        User user = (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        // 2. find or create state
        State state = stateRepository.findByUfIgnoreCase(data.stateUf())
                .orElseGet(() -> stateRepository.save(
                        State.builder().uf(data.stateUf().toUpperCase()).name(data.stateName()).build()
                ));

        // 3. find or create City
        City city = cityRepository.findByNameIgnoreCaseAndStateId(data.city(), state.getId())
                .orElseGet(() -> cityRepository.save(
                        City.builder().name(data.city()).state(state).build()
                ));

        // 4. save address
        Address address = Address.builder()
                .street(data.street())
                .number(data.number())
                .complement(data.complement())
                .neighborhood(data.neighborhood())
                .zipCode(data.zipCode())
                .city(city)
                .user(user)
                .build();

        return addressRepository.save(address);
    }
}
