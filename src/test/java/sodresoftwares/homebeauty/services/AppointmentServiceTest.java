package sodresoftwares.homebeauty.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import sodresoftwares.homebeauty.dto.AppointmentCreateDTO;
import sodresoftwares.homebeauty.dto.AppointmentResponseDTO;
import sodresoftwares.homebeauty.dto.AppointmentStatusUpdateDTO;
import sodresoftwares.homebeauty.enums.AppointmentStatus;
import sodresoftwares.homebeauty.enums.AppointmentType;
import sodresoftwares.homebeauty.enums.ServiceLocationType;
import sodresoftwares.homebeauty.infra.exception.AppException;
import sodresoftwares.homebeauty.model.*;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;
import sodresoftwares.homebeauty.repositories.AddressRepository;
import sodresoftwares.homebeauty.repositories.AppointmentRepository;
import sodresoftwares.homebeauty.repositories.ProvidedServiceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppointmentService
 *
 * Tests business logic for appointment management including creation, retrieval, and status updates.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService Tests")
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ProvidedServiceRepository serviceRepository;

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private User client;
    private User professional;
    private ProvidedService providedService;
    private Address clientAddress;
    private Address professionalAddress;
    private Appointment testAppointment;
    private AppointmentCreateDTO validCreateDTO;
    private LocalDateTime testStartTime;

    @BeforeEach
    void setUp() {
        client = User.builder()
                .id("client-123")
                .login("client@example.com")
                .firstName("Cliente")
                .lastName("Teste")
                .role(UserRole.USER)
                .build();

        professional = User.builder()
                .id("prof-123")
                .login("prof@example.com")
                .firstName("Profissional")
                .lastName("Teste")
                .role(UserRole.PROFESSIONAL)
                .build();

        Category category = Category.builder()
                .id("cat-123")
                .name("Cabelo")
                .build();

        ProfessionalProfile profile = ProfessionalProfile.builder()
                .id("profile-123")
                .user(professional)
                .build();

        providedService = ProvidedService.builder()
                .id("service-123")
                .name("Corte de Cabelo")
                .durationMinutes(60)
                .price(BigDecimal.valueOf(50.00))
                .locationType(ServiceLocationType.PROVIDER_LOCATION_ONLY)
                .category(category)
                .professional(profile)
                .build();

        clientAddress = Address.builder()
                .id("addr-client")
                .street("Rua Cliente")
                .number("100")
                .neighborhood("Centro")
                .zipCode("01234567")
                .user(client)
                .build();

        professionalAddress = Address.builder()
                .id("addr-prof")
                .street("Rua Profissional")
                .number("200")
                .neighborhood("Zona Sul")
                .zipCode("09876543")
                .user(professional)
                .build();

        testStartTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

        validCreateDTO = new AppointmentCreateDTO(
                "service-123",
                AppointmentType.PROVIDER_LOCATION,
                testStartTime,
                "addr-prof",
                "Apenas fazer uma manutenção"
        );

        testAppointment = Appointment.builder()
                .id("apt-123")
                .client(client)
                .professionalUser(professional)
                .service(providedService)
                .address(professionalAddress)
                .appointmentType(AppointmentType.PROVIDER_LOCATION)
                .startTime(testStartTime)
                .endTime(testStartTime.plusMinutes(60))
                .status(AppointmentStatus.PENDING)
                .notes("Apenas fazer uma manutenção")
                .serviceName("Corte de Cabelo")
                .categoryName("Cabelo")
                .professionalName("Profissional Teste")
                .price(BigDecimal.valueOf(50.00))
                .build();
    }

    @Test
    @DisplayName("Should create appointment successfully")
    void shouldCreateAppointmentSuccessfully() {
        // Arrange
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(providedService));
        when(addressRepository.findById("addr-prof")).thenReturn(Optional.of(professionalAddress));
        when(appointmentRepository.hasOverlappingAppointments(
                "prof-123", testStartTime, testStartTime.plusMinutes(60), AppointmentStatus.CANCELLED))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);

        // Act
        AppointmentResponseDTO result = appointmentService.createAppointment(client, validCreateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("apt-123");
        assertThat(result.status()).isEqualTo(AppointmentStatus.PENDING);

        verify(serviceRepository).findById("service-123");
        verify(addressRepository).findById("addr-prof");
        verify(appointmentRepository).hasOverlappingAppointments(eq("prof-123"), any(), any(), any());

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());

        Appointment savedAppointment = captor.getValue();

        assertThat(savedAppointment.getEndTime()).isEqualTo(testStartTime.plusMinutes(60));

        assertThat(savedAppointment.getPrice()).isEqualTo(BigDecimal.valueOf(50.00));
        assertThat(savedAppointment.getServiceName()).isEqualTo("Corte de Cabelo");
        assertThat(savedAppointment.getProfessionalName()).isEqualTo("Profissional Teste");
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when provided service does not exist")
    void shouldThrowNotFoundWhenServiceNotFound() {
        // Arrange
        when(serviceRepository.findById("service-123")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(client, validCreateDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "SERVICE_NOT_FOUND")
                .hasMessage("Provided Service not found.");

        verify(serviceRepository).findById("service-123");
        verify(addressRepository, never()).findById(any());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when appointment type not allowed for service")
    void shouldThrowBadRequestWhenAppointmentTypeNotAllowed() {
        // Arrange
        providedService.setLocationType(ServiceLocationType.CLIENT_LOCATION_ONLY);
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(providedService));

        AppointmentCreateDTO invalidDTO = new AppointmentCreateDTO(
                "service-123",
                AppointmentType.PROVIDER_LOCATION, // Not allowed for this service
                testStartTime,
                "addr-prof",
                null
        );

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(client, invalidDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "LOCATION_TYPE_MISMATCH")
                .hasMessage("This service is only available at the client's location.");
        
        verify(serviceRepository).findById("service-123");
        verify(addressRepository, never()).findById(any());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when address ID is null")
    void shouldThrowBadRequestWhenAddressIdIsNull() {
        // Arrange
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(providedService));

        AppointmentCreateDTO invalidDTO = new AppointmentCreateDTO(
                "service-123",
                AppointmentType.PROVIDER_LOCATION,
                testStartTime,
                null, // Address ID is null
                null
        );

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(client, invalidDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "MISSING_ADDRESS_ID")
                .hasMessage("Address ID is required.");

        verify(serviceRepository).findById("service-123");
        verify(addressRepository, never()).findById(any());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when address does not exist")
    void shouldThrowNotFoundWhenAddressNotFound() {
        // Arrange
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(providedService));
        when(addressRepository.findById("addr-prof")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(client, validCreateDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "ADDRESS_NOT_FOUND")
                .hasMessage("Address not found.");

        verify(serviceRepository).findById("service-123");
        verify(addressRepository).findById("addr-prof");
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when client location address belongs to professional")
    void shouldThrowForbiddenWhenClientLocationAddressWrongOwner() {
        // Arrange
        providedService.setLocationType(ServiceLocationType.CLIENT_LOCATION_ONLY);
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(providedService));
        when(addressRepository.findById("addr-prof")).thenReturn(Optional.of(professionalAddress));

        AppointmentCreateDTO invalidDTO = new AppointmentCreateDTO(
                "service-123",
                AppointmentType.CLIENT_LOCATION,
                testStartTime,
                "addr-prof", // Professional's address, not client's
                null
        );

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(client, invalidDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("errorCode", "ADDRESS_ACCESS_DENIED")
                .hasMessage("Access denied: The address must belong to the client.");

        verify(serviceRepository).findById("service-123");
        verify(addressRepository).findById("addr-prof");
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when provider location address belongs to client")
    void shouldThrowForbiddenWhenProviderLocationAddressWrongOwner() {
        // Arrange
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(providedService));
        when(addressRepository.findById("addr-client")).thenReturn(Optional.of(clientAddress));

        AppointmentCreateDTO invalidDTO = new AppointmentCreateDTO(
                "service-123",
                AppointmentType.PROVIDER_LOCATION,
                testStartTime,
                "addr-client", // Client's address, not professional's
                null
        );

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(client, invalidDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("errorCode", "ADDRESS_ACCESS_DENIED")
                .hasMessage("Access denied: The address must belong to the selected professional.");

        verify(serviceRepository).findById("service-123");
        verify(addressRepository).findById("addr-client");
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should throw CONFLICT when time slot is already taken")
    void shouldThrowConflictWhenTimeSlotTaken() {
        // Arrange
        when(serviceRepository.findById("service-123")).thenReturn(Optional.of(providedService));
        when(addressRepository.findById("addr-prof")).thenReturn(Optional.of(professionalAddress));
        when(appointmentRepository.hasOverlappingAppointments(
                "prof-123", testStartTime, testStartTime.plusMinutes(60), AppointmentStatus.CANCELLED))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(client, validCreateDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "TIME_SLOT_UNAVAILABLE")
                .hasMessage("The professional already has an appointment scheduled for this time slot.");

        verify(serviceRepository).findById("service-123");
        verify(addressRepository).findById("addr-prof");
        verify(appointmentRepository).hasOverlappingAppointments(eq("prof-123"), any(), any(), any());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should get appointments by client successfully")
    void shouldGetAppointmentsByClientSuccessfully() {
        // Arrange
        List<Appointment> appointments = List.of(testAppointment);
        when(appointmentRepository.findByClient_IdOrderByStartTimeAsc("client-123"))
                .thenReturn(appointments);

        // Act
        List<AppointmentResponseDTO> result = appointmentService.getAppointmentsByClient(client);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("apt-123");
        assertThat(result.get(0).status()).isEqualTo(AppointmentStatus.PENDING);

        verify(appointmentRepository).findByClient_IdOrderByStartTimeAsc("client-123");
    }

    @Test
    @DisplayName("Should get empty list when client has no appointments")
    void shouldReturnEmptyListWhenClientHasNoAppointments() {
        // Arrange
        when(appointmentRepository.findByClient_IdOrderByStartTimeAsc("client-123"))
                .thenReturn(List.of());

        // Act
        List<AppointmentResponseDTO> result = appointmentService.getAppointmentsByClient(client);

        // Assert
        assertThat(result).isEmpty();

        verify(appointmentRepository).findByClient_IdOrderByStartTimeAsc("client-123");
    }

    @Test
    @DisplayName("Should get appointments by professional successfully")
    void shouldGetAppointmentsByProfessionalSuccessfully() {
        // Arrange
        List<Appointment> appointments = List.of(testAppointment);
        when(appointmentRepository.findByProfessionalUser_IdOrderByStartTimeAsc("prof-123"))
                .thenReturn(appointments);

        // Act
        List<AppointmentResponseDTO> result = appointmentService.getAppointmentsByProfessional(professional);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("apt-123");

        verify(appointmentRepository).findByProfessionalUser_IdOrderByStartTimeAsc("prof-123");
    }

    @Test
    @DisplayName("Should get appointment by ID successfully for client")
    void shouldGetAppointmentByIdSuccessfullyForClient() {
        // Arrange
        when(appointmentRepository.findById("apt-123")).thenReturn(Optional.of(testAppointment));

        // Act
        AppointmentResponseDTO result = appointmentService.getAppointmentById(client, "apt-123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("apt-123");
        assertThat(result.status()).isEqualTo(AppointmentStatus.PENDING);

        verify(appointmentRepository).findById("apt-123");
    }

    @Test
    @DisplayName("Should get appointment by ID successfully for professional")
    void shouldGetAppointmentByIdSuccessfullyForProfessional() {
        // Arrange
        when(appointmentRepository.findById("apt-123")).thenReturn(Optional.of(testAppointment));

        // Act
        AppointmentResponseDTO result = appointmentService.getAppointmentById(professional, "apt-123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("apt-123");

        verify(appointmentRepository).findById("apt-123");
    }

    @Test
    @DisplayName("Should get appointment by ID successfully for admin")
    void shouldGetAppointmentByIdSuccessfullyForAdmin() {
        // Arrange
        User admin = User.builder()
                .id("admin-123")
                .role(UserRole.ADMIN)
                .build();
        when(appointmentRepository.findById("apt-123")).thenReturn(Optional.of(testAppointment));

        // Act
        AppointmentResponseDTO result = appointmentService.getAppointmentById(admin, "apt-123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("apt-123");

        verify(appointmentRepository).findById("apt-123");
    }

    @Test
    @DisplayName("Should throw NOT_FOUND when appointment does not exist")
    void shouldThrowNotFoundWhenAppointmentDoesNotExist() {
        // Arrange
        when(appointmentRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.getAppointmentById(client, "non-existent"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "APPOINTMENT_NOT_FOUND")
                .hasMessage("Appointment not found.");

        verify(appointmentRepository).findById("non-existent");
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when other user tries to access appointment")
    void shouldThrowForbiddenWhenOtherUserAccessesAppointment() {
        // Arrange
        User otherUser = User.builder()
                .id("other-user")
                .login("other@example.com")
                .role(UserRole.USER)
                .build();
        when(appointmentRepository.findById("apt-123")).thenReturn(Optional.of(testAppointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.getAppointmentById(otherUser, "apt-123"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("errorCode", "APPOINTMENT_ACCESS_DENIED")
                .hasMessage("Access denied: You are not part of this appointment.");

        verify(appointmentRepository).findById("apt-123");
    }


    @Test
    @DisplayName("Should throw NOT_FOUND when updating status of non-existent appointment")
    void shouldThrowNotFoundWhenUpdatingStatusOfNonExistentAppointment() {
        // Arrange
        when(appointmentRepository.findById("non-existent")).thenReturn(Optional.empty());

        AppointmentStatusUpdateDTO updateDTO = new AppointmentStatusUpdateDTO(AppointmentStatus.CONFIRMED);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.updateStatus(professional, "non-existent", updateDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "APPOINTMENT_NOT_FOUND")
                .hasMessage("Appointment not found.");

        verify(appointmentRepository).findById("non-existent");
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should allow Professional to update status to any value (e.g., CONFIRMED)")
    void shouldAllowProfessionalToUpdateStatus() {
        // Arrange
        when(appointmentRepository.findById("apt-123")).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(null);

        AppointmentStatusUpdateDTO updateDTO = new AppointmentStatusUpdateDTO(AppointmentStatus.CONFIRMED);

        // Act
        appointmentService.updateStatus(professional, "apt-123", updateDTO);

        // Assert
        verify(appointmentRepository).findById("apt-123");

        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(appointmentCaptor.capture());

        Appointment savedAppointment = appointmentCaptor.getValue();

        assertThat(savedAppointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should allow Client to update status ONLY to CANCELLED")
    void shouldAllowClientToCancelAppointment() {
        // Arrange
        when(appointmentRepository.findById("apt-123")).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(null);

        AppointmentStatusUpdateDTO updateDTO = new AppointmentStatusUpdateDTO(AppointmentStatus.CANCELLED);

        // Act
        appointmentService.updateStatus(client, "apt-123", updateDTO);

        // Assert
        verify(appointmentRepository).findById("apt-123");

        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(appointmentCaptor.capture());

        Appointment savedAppointment = appointmentCaptor.getValue();
        assertThat(savedAppointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when Client tries to CONFIRM or COMPLETE appointment")
    void shouldThrowForbiddenWhenClientTriesToConfirmOrComplete() {
        // Arrange
        when(appointmentRepository.findById("apt-123")).thenReturn(Optional.of(testAppointment));

        AppointmentStatusUpdateDTO updateDTO = new AppointmentStatusUpdateDTO(AppointmentStatus.CONFIRMED);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.updateStatus(client, "apt-123", updateDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("errorCode", "UPDATE_STATUS_FORBIDDEN")
                .hasMessage("Clients can only CANCEL appointments. Only professionals can update to other statuses.");

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when completely unrelated user tries to update status")
    void shouldThrowForbiddenWhenUnrelatedUserUpdatesStatus() {
        // Arrange
        User hacker = User.builder().id("hacker-123").role(UserRole.USER).build();
        when(appointmentRepository.findById("apt-123")).thenReturn(Optional.of(testAppointment));

        AppointmentStatusUpdateDTO updateDTO = new AppointmentStatusUpdateDTO(AppointmentStatus.CANCELLED);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.updateStatus(hacker, "apt-123", updateDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("errorCode", "APPOINTMENT_ACCESS_DENIED")
                .hasMessage("Access denied: You are not part of this appointment.");

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }
}