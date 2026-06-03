package sodresoftwares.homebeauty.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sodresoftwares.homebeauty.dto.AddressDTO;
import sodresoftwares.homebeauty.infra.security.SecurityFilter;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;
import sodresoftwares.homebeauty.services.AddressService;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AddressController
 *
 * Focuses strictly on HTTP layer interactions: parameter parsing,
 * status code mapping, and JSON serialization/deserialization.
 */
@WebMvcTest(
        controllers = AddressController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AddressController Tests")
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AddressService addressService;

    private AddressDTO validAddressDTO;
    private AddressDTO invalidAddressDTO;
    private List<AddressDTO> addressList;

    @BeforeEach
    void setUp() {

        User loggedInUser = User.builder()
                .id("123")
                .firstName("Daniel")
                .lastName("Sodre")
                .login("daniel@test.com")
                .role(UserRole.USER)
                .build();

        validAddressDTO = new AddressDTO(
                null,
                "Rua das Flores",
                "123",
                "Apto 45",
                "Centro",
                "01234567",
                "São Paulo",
                "SP",
                "São Paulo"
        );

        invalidAddressDTO = new AddressDTO(
                null,
                "", // Invalid: blank street
                "", // Invalid: blank number
                null,
                "", // Invalid: blank neighborhood
                "123", // Invalid: wrong size
                "", // Invalid: blank city
                "S", // Invalid: wrong size
                "" // Invalid: blank state name
        );

        AddressDTO address1 = new AddressDTO(
                "addr-1",
                "Rua A",
                "100",
                null,
                "Centro",
                "01234567",
                "São Paulo",
                "SP",
                "São Paulo"
        );

        AddressDTO address2 = new AddressDTO(
                "addr-2",
                "Rua B",
                "200",
                "Casa",
                "Bairro Novo",
                "09876543",
                "Rio de Janeiro",
                "RJ",
                "Rio de Janeiro"
        );

        addressList = List.of(address1, address2);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loggedInUser, null, loggedInUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Should create address successfully (HTTP 201)")
    void testCreateAddress_Success() throws Exception {
        // Arrange
        doNothing().when(addressService).createAddress(any(User.class), any(AddressDTO.class));

        // Act & Assert
        mockMvc.perform(post("/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAddressDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        verify(addressService).createAddress(any(User.class), any(AddressDTO.class));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return 400 Bad Request for invalid address data")
    void testCreateAddress_InvalidData() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAddressDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/addresses"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for malformed JSON")
    void testCreateAddress_MalformedJson() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request. Please verify the data format, such as correct date/time patterns (e.g., 'HH:mm'), exact Enum values, and proper JSON syntax."))
                .andExpect(jsonPath("$.path").value("/addresses"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should get user's addresses successfully (HTTP 200)")
    void testGetMyAddresses_Success() throws Exception {
        // Arrange
        when(addressService.getMyAddresses(any(User.class))).thenReturn(addressList);

        // Act & Assert
        mockMvc.perform(get("/addresses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("addr-1"))
                .andExpect(jsonPath("$[0].street").value("Rua A"))
                .andExpect(jsonPath("$[0].number").value("100"))
                .andExpect(jsonPath("$[0].complement").isEmpty())
                .andExpect(jsonPath("$[0].neighborhood").value("Centro"))
                .andExpect(jsonPath("$[0].zipCode").value("01234567"))
                .andExpect(jsonPath("$[0].city").value("São Paulo"))
                .andExpect(jsonPath("$[0].stateUf").value("SP"))
                .andExpect(jsonPath("$[0].stateName").value("São Paulo"))
                .andExpect(jsonPath("$[1].id").value("addr-2"))
                .andExpect(jsonPath("$[1].street").value("Rua B"))
                .andExpect(jsonPath("$[1].number").value("200"))
                .andExpect(jsonPath("$[1].complement").value("Casa"))
                .andExpect(jsonPath("$[1].neighborhood").value("Bairro Novo"))
                .andExpect(jsonPath("$[1].zipCode").value("09876543"))
                .andExpect(jsonPath("$[1].city").value("Rio de Janeiro"))
                .andExpect(jsonPath("$[1].stateUf").value("RJ"))
                .andExpect(jsonPath("$[1].stateName").value("Rio de Janeiro"));
    }

    @Test
    @DisplayName("Should return empty list when user has no addresses (HTTP 200)")
    void testGetMyAddresses_EmptyList() throws Exception {
        // Arrange
        when(addressService.getMyAddresses(any(User.class))).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/addresses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should update address successfully (HTTP 204)")
    void testUpdateAddress_Success() throws Exception {
        // Arrange
        AddressDTO updateDTO = new AddressDTO(
                "addr-123",
                "Rua Atualizada",
                "999",
                "Novo Complemento",
                "Novo Bairro",
                "99999999",
                "Rio de Janeiro",
                "RJ",
                "Rio de Janeiro"
        );

        doNothing().when(addressService).updateAddress(any(User.class), eq("addr-123"), any(AddressDTO.class));

        // Act & Assert
        mockMvc.perform(put("/addresses/addr-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(addressService).updateAddress(any(User.class), eq("addr-123"), any(AddressDTO.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request for invalid update data")
    void testUpdateAddress_InvalidData() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/addresses/addr-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAddressDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/addresses/addr-123"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
