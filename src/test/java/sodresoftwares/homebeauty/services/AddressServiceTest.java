package sodresoftwares.homebeauty.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AddressService
 *
 * Tests business logic for address management including creation, retrieval, and updates.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddressService Tests")
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AddressService addressService;

    private User currentUser;
    private State testState;
    private City testCity;
    private Address testAddress;
    private AddressDTO baseCreateDTO;
    private AddressDTO baseUpdateDTO;


    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id("user-123")
                .login("test@example.com")
                .build();

        testState = State.builder()
                .id("state-123")
                .name("São Paulo")
                .uf("SP")
                .build();

        testCity = City.builder()
                .id("city-123")
                .name("São Paulo")
                .state(testState)
                .build();

        testAddress = Address.builder()
                .id("addr-123")
                .street("Rua das Flores")
                .number("123")
                .complement("Apto 45")
                .neighborhood("Centro")
                .zipCode("01234567")
                .city(testCity)
                .user(currentUser)
                .build();

        // Base DTO for creation scenarios (ID is always null on creation)
        baseCreateDTO = new AddressDTO(
                null,
                "Rua Nova",
                "456",
                "Casa",
                "Bairro Novo",
                "09876543",
                "Rio de Janeiro",
                "RJ",
                "Rio de Janeiro"
        );

        // Base DTO for update scenarios
        baseUpdateDTO = new AddressDTO(
                "addr-123",
                "Rua Nova Esperança",
                "777",
                "Apto 45",
                "Centro",
                "01234567",
                "São Paulo",
                "SP",
                "São Paulo"
        );

        // Mock security context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(currentUser);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should create address successfully")
    void shouldCreateAddressSuccessfully() {
        // Arrange
        State newState = State.builder().id("state-456").name("Rio de Janeiro").uf("RJ").build();
        City newCity = City.builder().id("city-456").name("Rio de Janeiro").state(newState).build();

        when(stateRepository.findByUfIgnoreCase("RJ")).thenReturn(Optional.empty());
        when(stateRepository.save(any(State.class))).thenReturn(newState);
        when(cityRepository.findByNameIgnoreCaseAndStateId("Rio de Janeiro", "state-456")).thenReturn(Optional.empty());
        when(cityRepository.save(any(City.class))).thenReturn(newCity);

        when(addressRepository.save(any(Address.class))).thenReturn(null);

        // Act
        addressService.createAddress(baseCreateDTO);

        // Assert
        verify(stateRepository).findByUfIgnoreCase("RJ");
        verify(stateRepository).save(any(State.class));
        verify(cityRepository).findByNameIgnoreCaseAndStateId("Rio de Janeiro", "state-456");
        verify(cityRepository).save(any(City.class));

        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(addressCaptor.capture());

        Address savedAddress = addressCaptor.getValue();

        assertThat(savedAddress.getStreet()).isEqualTo("Rua Nova");
        assertThat(savedAddress.getCity().getName()).isEqualTo("Rio de Janeiro");
        assertThat(savedAddress.getUser().getId()).isEqualTo("user-123");
    }

    @Test
    @DisplayName("Should create address with existing state and city")
    void shouldCreateAddressWithExistingStateAndCity() {
        // Arrange
        AddressDTO existingLocationDTO = new AddressDTO(
                null,
                "Rua Existente",
                "789",
                null,
                "Centro",
                "01234567",
                "São Paulo",
                "SP",
                "São Paulo"
        );

        when(stateRepository.findByUfIgnoreCase("SP")).thenReturn(Optional.of(testState));
        when(cityRepository.findByNameIgnoreCaseAndStateId("São Paulo", "state-123")).thenReturn(Optional.of(testCity));
        when(addressRepository.save(any(Address.class))).thenReturn(null);

        // Act
        addressService.createAddress(existingLocationDTO);

        // Assert
        verify(stateRepository).findByUfIgnoreCase("SP");
        verify(stateRepository, never()).save(any(State.class));
        verify(cityRepository).findByNameIgnoreCaseAndStateId("São Paulo", "state-123");
        verify(cityRepository, never()).save(any(City.class));

        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(addressCaptor.capture());

        Address savedAddress = addressCaptor.getValue();
        assertThat(savedAddress.getStreet()).isEqualTo("Rua Existente");
        assertThat(savedAddress.getCity().getName()).isEqualTo("São Paulo");
        assertThat(savedAddress.getCity().getState().getUf()).isEqualTo("SP");
    }

    @Test
    @DisplayName("Should get user's addresses successfully")
    void shouldGetUserAddressesSuccessfully() {
        // Arrange
        List<Address> userAddresses = List.of(testAddress);
        when(addressRepository.findByUserId("user-123")).thenReturn(userAddresses);

        // Act
        List<AddressDTO> result = addressService.getMyAddresses();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("addr-123");
        assertThat(result.get(0).street()).isEqualTo("Rua das Flores");
        assertThat(result.get(0).number()).isEqualTo("123");
        assertThat(result.get(0).complement()).isEqualTo("Apto 45");
        assertThat(result.get(0).neighborhood()).isEqualTo("Centro");
        assertThat(result.get(0).zipCode()).isEqualTo("01234567");
        assertThat(result.get(0).city()).isEqualTo("São Paulo");
        assertThat(result.get(0).stateUf()).isEqualTo("SP");
        assertThat(result.get(0).stateName()).isEqualTo("São Paulo");

        verify(addressRepository).findByUserId("user-123");
    }

    @Test
    @DisplayName("Should return empty list when user has no addresses")
    void shouldReturnEmptyListWhenNoAddresses() {
        // Arrange
        when(addressRepository.findByUserId("user-123")).thenReturn(List.of());

        // Act
        List<AddressDTO> result = addressService.getMyAddresses();

        // Assert
        assertThat(result).isEmpty();
        verify(addressRepository).findByUserId("user-123");
    }

    @Test
    @DisplayName("Should update address partially (e.g., only street and number)")
    void shouldUpdateAddressPartially() {
        when(addressRepository.findById("addr-123")).thenReturn(Optional.of(testAddress));

        // Como a cidade foi mantida, o Service vai buscar "SP" e vai achar na memória falsa
        when(stateRepository.findByUfIgnoreCase("SP")).thenReturn(Optional.of(testState));
        when(cityRepository.findByNameIgnoreCaseAndStateId("São Paulo", "state-123")).thenReturn(Optional.of(testCity));

        when(addressRepository.save(any(Address.class))).thenReturn(null);

        // Act
        addressService.updateAddress("addr-123", baseUpdateDTO);

        // Assert
        verify(addressRepository).findById("addr-123");

        // Garante que NÃO salvou um estado/cidade duplicado durante o update
        verify(stateRepository, never()).save(any(State.class));
        verify(cityRepository, never()).save(any(City.class));

        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(addressCaptor.capture());

        Address updatedAddress = addressCaptor.getValue();

        assertThat(updatedAddress.getStreet()).isEqualTo("Rua Nova Esperança");
        assertThat(updatedAddress.getNumber()).isEqualTo("777");
        assertThat(updatedAddress.getComplement()).isEqualTo("Apto 45");
        assertThat(updatedAddress.getNeighborhood()).isEqualTo("Centro");
        assertThat(updatedAddress.getZipCode()).isEqualTo("01234567");
        assertThat(updatedAddress.getCity().getName()).isEqualTo("São Paulo");
        assertThat(updatedAddress.getCity().getState().getUf()).isEqualTo("SP");
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when address does not exist")
    void shouldThrowNotFoundWhenAddressDoesNotExist() {
        // Arrange
        when(addressRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> addressService.updateAddress("non-existent", baseUpdateDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessage("404 NOT_FOUND \"Address not found\"");

        verify(addressRepository).findById("non-existent");
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when updating address of another user")
    void shouldThrowForbiddenWhenUpdatingOtherUserAddress() {
        // Arrange
        User otherUser = User.builder().id("other-user").build();
        Address otherUserAddress = Address.builder()
                .id("other-addr")
                .user(otherUser)
                .build();

        when(addressRepository.findById("other-addr")).thenReturn(Optional.of(otherUserAddress));

        // Act & Assert
        assertThatThrownBy(() -> addressService.updateAddress("other-addr", baseUpdateDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasMessage("403 FORBIDDEN \"You do not have permission to edit this address\"");

        verify(addressRepository).findById("other-addr");
        verify(addressRepository, never()).save(any(Address.class));
    }
}