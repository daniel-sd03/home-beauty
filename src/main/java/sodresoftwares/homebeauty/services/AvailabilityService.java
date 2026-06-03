package sodresoftwares.homebeauty.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.enums.AppointmentStatus;
import sodresoftwares.homebeauty.model.*;
import sodresoftwares.homebeauty.repositories.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    private final ProfessionalProfileRepository profileRepository;
    private final ProvidedServiceRepository serviceRepository;
    private final WorkingHourRepository workingHoursRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProfessionalBlockRepository blockRepository;

    public AvailabilityService(
            ProfessionalProfileRepository profileRepository,
            ProvidedServiceRepository serviceRepository,
            WorkingHourRepository workingHourRepository,
            AppointmentRepository appointmentRepository,
            ProfessionalBlockRepository blockRepository) {

        this.profileRepository = profileRepository;
        this.serviceRepository = serviceRepository;
        this.workingHoursRepository = workingHourRepository;
        this.appointmentRepository = appointmentRepository;
        this.blockRepository = blockRepository;
    }


    private static final int SLOT_INTERVAL_MINUTES = 30;

    public List<LocalDateTime> getAvailableSlots(String professionalId, String serviceId, LocalDate date) {
        log.info("Calculating available slots for professionalId: {}, serviceId: {}, date: {}", professionalId, serviceId, date);

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        if (date.isBefore(todayUtc)) {
            log.warn("Attempted to fetch availability for a past date: {}", date);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot fetch availability for past dates."
            );
        }

        // 1. Fetch base data
        ProfessionalProfile profile = profileRepository.findById(professionalId)
                .orElseThrow(() -> {
                    log.warn("Professional profile not found with ID: {}", professionalId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Professional profile not found");
                });

        ProvidedService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> {
                    log.warn("Service not found with ID: {}", serviceId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found");
                });

        WorkingHour workingHour = workingHoursRepository
                .findByProfessionalIdAndDayOfWeek(professionalId, date.getDayOfWeek())
                .orElse(null);

        // If the professional doesn't work on this day of the week, return an empty list
        if (workingHour == null) {
            log.info("Professional {} does not work on {}. Returning empty slots.", professionalId, date.getDayOfWeek());
            return new ArrayList<>();
        }

        // 2. Fetch the day's conflicts from the database
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Appointment> dayAppointments = appointmentRepository.findActiveAppointmentsByDay(
                profile.getUser().getId(),
                startOfDay,
                endOfDay,
                AppointmentStatus.CANCELLED
        );

        List<ProfessionalBlock> dayBlocks = blockRepository.findBlocksByDay(
                professionalId,
                startOfDay,
                endOfDay);

        log.info("Found {} active appointments and {} blocks for professional {} on {}",
                dayAppointments.size(), dayBlocks.size(), professionalId, date);

        // 3. Set the boundaries for the working day
        LocalDateTime startOfWorkDay = date.atTime(workingHour.getStartTime());
        LocalDateTime endOfWorkDay = date.atTime(workingHour.getEndTime());

        // 4. Generate the list of available slots
        List<LocalDateTime> availableSlots = calculateFreeSlots(startOfWorkDay, endOfWorkDay, service.getDurationMinutes(), dayAppointments, dayBlocks);

        log.info("Successfully calculated {} available slots for professional {} on {}", availableSlots.size(), professionalId, date);
        return availableSlots;
    }

    private List<LocalDateTime> calculateFreeSlots(
            LocalDateTime startOfWorkDay,
            LocalDateTime endOfWorkDay,
            int serviceDuration,
            List<Appointment> appointments,
            List<ProfessionalBlock> blocks) {

        List<LocalDateTime> availableSlots = new ArrayList<>();
        LocalDateTime currentSlot = startOfWorkDay;
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        // Jumps in 30-minute intervals until the end of the shift
        while (currentSlot.isBefore(endOfWorkDay)) {
            LocalDateTime slotEndTime = currentSlot.plusMinutes(serviceDuration);

            boolean fitsInWorkingHours = !slotEndTime.isAfter(endOfWorkDay);
            boolean isFuture = currentSlot.isAfter(nowUtc);
            boolean hasNoConflicts = !isOverlapping(currentSlot, slotEndTime, appointments, blocks);

            if (fitsInWorkingHours && isFuture && hasNoConflicts) {
                availableSlots.add(currentSlot);
            }

            currentSlot = currentSlot.plusMinutes(SLOT_INTERVAL_MINUTES);
        }

        return availableSlots;
    }

    private boolean isOverlapping(
            LocalDateTime slotStart,
            LocalDateTime slotEnd,
            List<Appointment> appointments,
            List<ProfessionalBlock> blocks) {

        boolean overlapAppt = appointments.stream().anyMatch(appt ->
                slotStart.isBefore(appt.getEndTime()) && slotEnd.isAfter(appt.getStartTime())
        );

        if (overlapAppt) return true;

        return blocks.stream().anyMatch(block ->
                slotStart.isBefore(block.getEndDateTime()) && slotEnd.isAfter(block.getStartDateTime())
        );
    }
}