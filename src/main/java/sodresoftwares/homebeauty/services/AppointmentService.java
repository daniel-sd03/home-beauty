package sodresoftwares.homebeauty.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.dto.AppointmentCreateDTO;
import sodresoftwares.homebeauty.dto.AppointmentResponseDTO;
import sodresoftwares.homebeauty.enums.AppointmentStatus;
import sodresoftwares.homebeauty.enums.AppointmentType;
import sodresoftwares.homebeauty.enums.ServiceLocationType;
import sodresoftwares.homebeauty.model.Address;
import sodresoftwares.homebeauty.model.Appointment;
import sodresoftwares.homebeauty.model.ProfessionalProfile;
import sodresoftwares.homebeauty.model.ProvidedService;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.repositories.AddressRepository;
import sodresoftwares.homebeauty.repositories.AppointmentRepository;
import sodresoftwares.homebeauty.repositories.ProvidedServiceRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ProvidedServiceRepository serviceRepository;
    private final AddressRepository addressRepository;

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional
    public AppointmentResponseDTO createAppointment(AppointmentCreateDTO dto) {
        // Get the current User
        User client = getCurrentUser();

        //get the provided service and validate if it exists
        ProvidedService providedService = serviceRepository.findById(dto.providedServicesId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provided Service not found."));

        // Get the current user professional profile
        ProfessionalProfile profile = providedService.getProfessional();
        User professionalUser = profile.getUser();

        // Validate if the chosen AppointmentType is allowed by the Service rule
        AppointmentType requestedType = validateAndGetAppointmentType(dto, providedService);

        // Resolve and Validate Address (Extracted Method)
        Address address = resolveAndValidateAddress(dto.addressId(), requestedType, client, professionalUser);

        // Calculate end time based on service duration
        var endTime = dto.startTime().plusMinutes(providedService.getDurationMinutes());

        //  Time Conflict Validation
        validateTimeSlotAvailability(professionalUser.getId(), dto.startTime(), endTime);

        // 7. Build the Appointment with the Snapshot strategy
        Appointment appointment = Appointment.builder()
                .client(client)
                .professionalUser(professionalUser)
                .service(providedService)
                .address(address)
                .appointmentType(requestedType)
                .startTime(dto.startTime())
                .endTime(endTime)
                .status(AppointmentStatus.PENDING)
                .notes(dto.notes())
                .serviceName(providedService.getName())
                .categoryName(providedService.getCategory().getName())
                .professionalName(professionalUser.getName())
                .price(providedService.getPrice())
                .build();

        // 8. Save and return wrapped in a DTO
        Appointment savedAppointment = appointmentRepository.save(appointment);
        return new AppointmentResponseDTO(savedAppointment);
    }

    /**
     * Validates if the requested appointment type is compatible with the location rules defined by the service.
     */
    private AppointmentType validateAndGetAppointmentType(AppointmentCreateDTO dto, ProvidedService providedService) {
        ServiceLocationType allowedLocation = providedService.getLocationType();
        AppointmentType requestedType = dto.appointmentType();

        if (allowedLocation == ServiceLocationType.CLIENT_LOCATION_ONLY && requestedType != AppointmentType.CLIENT_LOCATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This service is only available at the client's location.");
        }
        if (allowedLocation == ServiceLocationType.PROVIDER_LOCATION_ONLY && requestedType != AppointmentType.PROVIDER_LOCATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This service is only available at the professional's location.");
        }
        return requestedType;
    }

    /**
     * Fetches the address from the database and validates if the correct user owns it
     * based on the requested appointment type.
     */
    private Address resolveAndValidateAddress(String addressId, AppointmentType requestedType, User client, User professionalUser) {
        if (addressId == null || addressId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address ID is required.");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found."));

        if (requestedType == AppointmentType.CLIENT_LOCATION) {
            // The address owner MUST be the logged-in client
            if (!address.getUser().getId().equals(client.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: The address must belong to the client.");
            }
        } else if (requestedType == AppointmentType.PROVIDER_LOCATION) {
            // The address owner MUST be the professional providing the service
            if (!address.getUser().getId().equals(professionalUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: The address must belong to the selected professional.");
            }
        }
        return address;
    }

    /**
     * Validates if the professional has any overlapping appointments during the requested time slot.
     */
    private void validateTimeSlotAvailability(String professionalId, LocalDateTime startTime, LocalDateTime endTime) {
        boolean isTimeSlotTaken = appointmentRepository.hasOverlappingAppointments(
                professionalId,
                startTime,
                endTime,
                AppointmentStatus.CANCELLED
        );

        if (isTimeSlotTaken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The professional already has an appointment scheduled for this time slot.");
        }
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getAppointmentsByClient() {

        //  Get the current User
        User client = getCurrentUser();

        List<Appointment> appointments = appointmentRepository.findByClient_IdOrderByStartTimeAsc(client.getId());

        return appointments.stream()
                .map(AppointmentResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getAppointmentsByProfessional() {

        // Get the current User (acting as the professional)
        User professional = getCurrentUser();

        List<Appointment> appointments = appointmentRepository.findByProfessionalUser_IdOrderByStartTimeAsc(professional.getId());

        return appointments.stream()
                .map(AppointmentResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponseDTO getAppointmentById(String id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found."));

        User currentUser = getCurrentUser();

        // Security check: only the involved client or professional can view it
        boolean isClient = appointment.getClient().getId().equals(currentUser.getId());
        boolean isProfessional = appointment.getProfessionalUser().getId().equals(currentUser.getId());

        // Check if the user is an Administrator
        boolean isAdmin = currentUser.getRole() == sodresoftwares.homebeauty.model.user.UserRole.ADMIN;

        if (!(isClient || isProfessional || isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not part of this appointment.");
        }

        return new AppointmentResponseDTO(appointment);
    }
}