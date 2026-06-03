package sodresoftwares.homebeauty.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.dto.AppointmentCreateDTO;
import sodresoftwares.homebeauty.dto.AppointmentResponseDTO;
import sodresoftwares.homebeauty.dto.AppointmentStatusUpdateDTO;
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
@Transactional(readOnly = true)
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final ProvidedServiceRepository serviceRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public AppointmentResponseDTO createAppointment(User LoggedInClient, AppointmentCreateDTO dto) {

        //get the provided service and validate if it exists
        ProvidedService providedService = serviceRepository.findById(dto.providedServicesId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provided Service not found."));

        // Get the current user professional profile
        ProfessionalProfile profile = providedService.getProfessional();
        User professionalUser = profile.getUser();

        // Validate if the chosen AppointmentType is allowed by the Service rule
        AppointmentType requestedType = validateAndGetAppointmentType(dto, providedService);

        // Resolve and Validate Address (Extracted Method)
        Address address = resolveAndValidateAddress(dto.addressId(), requestedType, LoggedInClient, professionalUser);

        // Calculate end time based on service duration
        var endTime = dto.startTime().plusMinutes(providedService.getDurationMinutes());

        //  Time Conflict Validation
        validateTimeSlotAvailability(professionalUser.getId(), dto.startTime(), endTime);

        // 7. Build the Appointment with the Snapshot strategy
        Appointment appointment = Appointment.builder()
                .client(LoggedInClient)
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
                .professionalName(professionalUser.getFullName())
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
    private void validateTimeSlotAvailability(String professionalUserId, LocalDateTime startTime, LocalDateTime endTime) {
        boolean isTimeSlotTaken = appointmentRepository.hasOverlappingAppointments(
                professionalUserId,
                startTime,
                endTime,
                AppointmentStatus.CANCELLED
        );

        if (isTimeSlotTaken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The professional already has an appointment scheduled for this time slot.");
        }
    }

    public List<AppointmentResponseDTO> getAppointmentsByClient(User LoggedInClient) {

        List<Appointment> appointments = appointmentRepository.findByClient_IdOrderByStartTimeAsc(LoggedInClient.getId());

        return appointments.stream()
                .map(AppointmentResponseDTO::new)
                .toList();
    }

    public List<AppointmentResponseDTO> getAppointmentsByProfessional(User LoggedInProfessional) {

        List<Appointment> appointments = appointmentRepository.findByProfessionalUser_IdOrderByStartTimeAsc(LoggedInProfessional.getId());

        return appointments.stream()
                .map(AppointmentResponseDTO::new)
                .toList();
    }

    public AppointmentResponseDTO getAppointmentById(User loggedInUser, String id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found."));

        // Security check: only the involved client or professional can view it
        boolean isClient = appointment.getClient().getId().equals(loggedInUser.getId());
        boolean isProfessional = appointment.getProfessionalUser().getId().equals(loggedInUser.getId());

        // Check if the user is an Administrator
        boolean isAdmin = loggedInUser.getRole() == sodresoftwares.homebeauty.model.user.UserRole.ADMIN;

        if (!(isClient || isProfessional || isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not part of this appointment.");
        }

        return new AppointmentResponseDTO(appointment);
    }

    @Transactional
    public void updateStatus(User loggedInUser, String id, AppointmentStatusUpdateDTO dto) {
        log.info("Attempting to update status of appointment ID: {} to {}", id, dto.status());

        // Fetch the appointment
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found."));

        // Identify who is trying to update
        boolean isProfessional = appointment.getProfessionalUser().getId().equals(loggedInUser.getId());
        boolean isClient = appointment.getClient().getId().equals(loggedInUser.getId());

        // 1. SECURITY: Check if user is part of the appointment
        if (!isProfessional && !isClient) {
            log.warn("Security breach attempt: User ID {} tried to modify appointment ID {}", loggedInUser.getId(), id);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not part of this appointment.");
        }

        // 2. BUSINESS RULE: Clients can only cancel
        if (isClient && !isProfessional) {
            if (dto.status() != AppointmentStatus.CANCELLED) {
                log.warn("Rule violation: Client ID {} attempted to set status to {} for appointment ID {}",
                        loggedInUser.getId(), dto.status(), id);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clients can only CANCEL appointments. Only professionals can update to other statuses.");
            }
        }

        // Update the status
        appointment.setStatus(dto.status());

        // Save to database
        appointmentRepository.save(appointment);

        log.info("Appointment ID: {} status successfully updated to {}", id, dto.status());
    }
}