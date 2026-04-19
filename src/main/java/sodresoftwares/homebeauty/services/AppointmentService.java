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
        // 1. Get the current User
        User client = getCurrentUser();

        ProvidedService providedService = serviceRepository.findById(dto.providedServicesId())
                .orElseThrow(() -> new RuntimeException("Service not found."));

        // Get the current user professional profile
        ProfessionalProfile profile = providedService.getProfessional();
        User professionalUser = profile.getUser();

        // 2. Validate if the chosen AppointmentType is allowed by the Service rule
        ServiceLocationType allowedLocation = providedService.getLocationType();
        AppointmentType requestedType = dto.appointmentType();

        if (allowedLocation == ServiceLocationType.CLIENT_LOCATION_ONLY && requestedType != AppointmentType.CLIENT_LOCATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This service is only available at the client's location.");
        }
        if (allowedLocation == ServiceLocationType.PROVIDER_LOCATION_ONLY && requestedType != AppointmentType.PROVIDER_LOCATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This service is only available at the professional's location.");
        }

        // 3. Search the address
        if (dto.addressId() == null || dto.addressId().isBlank()) {
            throw new IllegalArgumentException("Address ID is required.");
        }

        Address address = addressRepository.findById(dto.addressId())
                .orElseThrow(() -> new RuntimeException("Address not found."));

        // 4. Security Validation (Who owns this address?)
        if (requestedType == AppointmentType.CLIENT_LOCATION) {
            // If Client Location, the address owner MUST be the logged-in client
            if (!address.getUser().getId().equals(client.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: The address must belong to the client.");
            }
        } else if (requestedType == AppointmentType.PROVIDER_LOCATION) {
            // If Provider Location, the address owner MUST be the professional providing the service
            if (!address.getUser().getId().equals(professionalUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: The address must belong to the selected professional.");
            }
        }

        // 5. Calculate end time based on service duration
        var endTime = dto.startTime().plusMinutes(providedService.getDurationMinutes());

        // 6. Time Conflict Validation (Double-booking prevention)
        boolean isTimeSlotTaken = appointmentRepository.hasOverlappingAppointments(
                professionalUser.getId(),
                dto.startTime(),
                endTime,
                AppointmentStatus.CANCELLED
        );

        if (isTimeSlotTaken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The professional already has an appointment scheduled for this time slot.");
        }

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
                .orElseThrow(() -> new RuntimeException("Appointment not found."));

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