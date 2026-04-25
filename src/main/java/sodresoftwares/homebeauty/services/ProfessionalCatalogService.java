package sodresoftwares.homebeauty.services;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.dto.ProfessionalBlockDTO;
import sodresoftwares.homebeauty.dto.ProfessionalBlockResponseDTO;
import sodresoftwares.homebeauty.dto.ProvidedServiceDTO;
import sodresoftwares.homebeauty.dto.WorkingHourDTO;
import sodresoftwares.homebeauty.enums.AppointmentStatus;
import sodresoftwares.homebeauty.model.*;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.repositories.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProfessionalCatalogService {

    private final ProfessionalProfileRepository profileRepository;
    private final ProvidedServiceRepository providedServiceRepository;
    private final WorkingHourRepository workingHourRepository;
    private final CategoryRepository categoryRepository;
    private final ProfessionalBlockRepository blockRepository;
    private final AppointmentRepository appointmentRepository;

    public ProfessionalCatalogService(ProfessionalProfileRepository profileRepository,
                                      ProvidedServiceRepository providedServiceRepository,
                                      WorkingHourRepository workingHourRepository,
                                      CategoryRepository categoryRepository,
                                      ProfessionalBlockRepository blockRepository,
                                      AppointmentRepository appointmentRepository) {
        this.profileRepository = profileRepository;
        this.providedServiceRepository = providedServiceRepository;
        this.workingHourRepository = workingHourRepository;
        this.categoryRepository = categoryRepository;
        this.blockRepository = blockRepository;
        this.appointmentRepository = appointmentRepository;
    }

    // Helper method to always get the logged-in professional's profile
    private ProfessionalProfile getCurrentUserProfile() {
        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        return profileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: User does not have a professional profile"));
    }

    private Category getCategoryById(String categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with the provided ID"));
    }

    @Transactional
    public void addProvidedService(ProvidedServiceDTO data) {
        // 1. Get the current professional profile
        ProfessionalProfile professional = getCurrentUserProfile();

        // 2. Get the current category profile
        Category currentCategory = getCategoryById(data.categoryId());

        // 3. Build the new service linked to the profile
        ProvidedService newService = ProvidedService.builder()
                .name(data.name())
                .description(data.description())
                .price(data.price())
                .durationMinutes(data.durationMinutes())
                .professional(professional)
                .category(currentCategory)
                .build();

        // 4. Save it
        providedServiceRepository.save(newService);
    }

    public List<ProvidedServiceDTO> getMyProvidedServices() {
        ProfessionalProfile professional = getCurrentUserProfile();

        // Maps the list of Entities to a list of DTOs to return to the front-end
        return professional.getServices().stream()
                .map(service -> new ProvidedServiceDTO(
                        service.getId(),
                        service.getName(),
                        service.getDescription(),
                        service.getPrice(),
                        service.getDurationMinutes(),
                        service.getCategory().getId()
                ))
                .toList();
    }

    @Transactional
    public void updateService(String serviceId, ProvidedServiceDTO data) {
        ProfessionalProfile professional = getCurrentUserProfile();

        // 1. Find the service by ID
        ProvidedService existingService = providedServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));

        // 2. SECURITY: Check if the service belongs to the logged-in professional
        if (!existingService.getProfessional().getId().equals(professional.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to edit this service");
        }

        // 3. Fetch the new category (in case it was changed)
        Category currentCategory = getCategoryById(data.categoryId());

        // 4. Update the data
        existingService.setName(data.name());
        existingService.setDescription(data.description());
        existingService.setPrice(data.price());
        existingService.setDurationMinutes(data.durationMinutes());
        existingService.setCategory(currentCategory);

        // 5. Save to the database
        providedServiceRepository.save(existingService);
    }

    @Transactional
    public void addWorkingHour(WorkingHourDTO data) {
        // 1. Get the current professional profile
        ProfessionalProfile professional = getCurrentUserProfile();

        // 2. Build the working hour linked to the profile
        WorkingHour newWorkingHour = WorkingHour.builder()
                .dayOfWeek(data.dayOfWeek())
                .startTime(data.startTime())
                .endTime(data.endTime())
                .professional(professional)
                .build();

        // 3. Save it
        workingHourRepository.save(newWorkingHour);
    }

    public List<WorkingHourDTO> getMyWorkingHours() {
        ProfessionalProfile professional = getCurrentUserProfile();

        // Maps the list of Entities to a list of DTOs to return to the front-end
        return professional.getWorkingHours().stream()
                .map(workingHour -> new WorkingHourDTO(
                        workingHour.getId(),
                        workingHour.getDayOfWeek(),
                        workingHour.getStartTime(),
                        workingHour.getEndTime()
                ))
                .toList();
    }

    @Transactional
    public void updateWorkingHour(String workingHourId, WorkingHourDTO data) {
        ProfessionalProfile professional = getCurrentUserProfile();

        // 1. Find the working hour by ID
        WorkingHour existingWorkingHour = workingHourRepository.findById(workingHourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Working hour not found"));

        // 2. SECURITY: Check if the working hour belongs to the logged-in professional
        if (!existingWorkingHour.getProfessional().getId().equals(professional.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to edit this working hour");
        }

        // 3. Update the data
        existingWorkingHour.setDayOfWeek(data.dayOfWeek());
        existingWorkingHour.setStartTime(data.startTime());
        existingWorkingHour.setEndTime(data.endTime());

        // 4. Save to the database
        workingHourRepository.save(existingWorkingHour);
    }

    @Transactional
    public void deleteService(String serviceId) {
        ProfessionalProfile professional = getCurrentUserProfile();

        // 1. Find the service by ID
        ProvidedService existingService = providedServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));

        // 2. SECURITY: Check if the service belongs to the logged-in professional
        if (!existingService.getProfessional().getId().equals(professional.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this service");
        }

        // 3. Delete the service
        providedServiceRepository.delete(existingService);
    }

    @Transactional
    public void deleteWorkingHour(String workingHourId) {
        ProfessionalProfile professional = getCurrentUserProfile();

        // 1. Find the working hour by ID
        WorkingHour existingWorkingHour = workingHourRepository.findById(workingHourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Working hour not found"));

        // 2. SECURITY: Check if the working hour belongs to the logged-in professional
        if (!existingWorkingHour.getProfessional().getId().equals(professional.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this working hour");
        }

        // 3. Delete the working hour
        workingHourRepository.delete(existingWorkingHour);
    }

    @Transactional
    public void createBlock(ProfessionalBlockDTO data) {
        ProfessionalProfile profile = getCurrentUserProfile();

        // Calculate time
        LocalDateTime checkStart = data.startDateTime().withSecond(0).withNano(0);;
        LocalDateTime checkEnd = data.endDateTime().withSecond(0).withNano(0);;

        // valide the block timeline (start must be before end, and cannot be in the past,
        // and a single block cannot exceed 30 days)
        validateBlockTimeline(checkStart, checkEnd);

        // Check conflict existing appointments
        validateNoSchedulingConflicts(profile.getUser().getId(), checkStart, checkEnd);

        // Check conflict with existing blocks
        validateNoOverlappingBlocks(profile.getId(), checkStart, checkEnd);

        ProfessionalBlock newBlock = ProfessionalBlock.builder()
                .title(data.title())
                .startDateTime(checkStart)
                .endDateTime(checkEnd)
                .professional(profile)
                .build();

        blockRepository.save(newBlock);
    }

    private void validateBlockTimeline(LocalDateTime start, LocalDateTime end) {
        // Set UTC 0
        LocalDateTime nowUtc = LocalDateTime.now(java.time.ZoneOffset.UTC);

        if (start.isBefore(nowUtc)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot create blocks in the past (UTC 0 reference)."
            );
        }

        if (start.isAfter(end) || start.isEqual(end)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The start date and time must be before the end date and time."
            );
        }

        if (java.time.temporal.ChronoUnit.DAYS.between(start, end) > 30) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A single block cannot exceed 30 days."
            );
        }
    }

    private void validateNoSchedulingConflicts(String professionalUserId, LocalDateTime start, LocalDateTime end) {
        boolean hasConflict = appointmentRepository.hasOverlappingAppointments(
                professionalUserId,
                start,
                end,
                AppointmentStatus.CANCELLED);

        if (hasConflict) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "It is not possible to block this period because there are already scheduled."
            );
        }
    }

    private void validateNoOverlappingBlocks(String profileId, LocalDateTime start, LocalDateTime end) {
        boolean hasConflict = blockRepository.hasOverlappingBlocks(
                profileId,
                start,
                end
        );

        if (hasConflict) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot create block: this period overlaps with an already existing block."
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ProfessionalBlockResponseDTO> getMyBlocks() {
        ProfessionalProfile profile = getCurrentUserProfile();

        return blockRepository.findByProfessionalIdOrderByStartDateTimeAsc(profile.getId())
                .stream()
                .map(block -> new ProfessionalBlockResponseDTO(
                        block.getId(),
                        block.getTitle(),
                        block.getStartDateTime(),
                        block.getEndDateTime()
                ))
                .toList();
    }
}