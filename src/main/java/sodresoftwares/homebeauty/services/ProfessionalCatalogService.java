package sodresoftwares.homebeauty.services;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.dto.ProvidedServiceDTO;
import sodresoftwares.homebeauty.dto.WorkingHourDTO;
import sodresoftwares.homebeauty.model.Category;
import sodresoftwares.homebeauty.model.ProfessionalProfile;
import sodresoftwares.homebeauty.model.ProvidedService;
import sodresoftwares.homebeauty.model.WorkingHour;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.repositories.CategoryRepository;
import sodresoftwares.homebeauty.repositories.ProfessionalProfileRepository;
import sodresoftwares.homebeauty.repositories.ProvidedServiceRepository;
import sodresoftwares.homebeauty.repositories.WorkingHourRepository;

import java.util.List;

@Service
public class ProfessionalCatalogService {

    private final ProfessionalProfileRepository profileRepository;
    private final ProvidedServiceRepository providedServiceRepository;
    private final WorkingHourRepository workingHourRepository;
    private final CategoryRepository categoryRepository;

    public ProfessionalCatalogService(ProfessionalProfileRepository profileRepository,
                                      ProvidedServiceRepository providedServiceRepository,
                                      WorkingHourRepository workingHourRepository,
                                      CategoryRepository categoryRepository) {
        this.profileRepository = profileRepository;
        this.providedServiceRepository = providedServiceRepository;
        this.workingHourRepository = workingHourRepository;
        this.categoryRepository = categoryRepository;
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

        // 5. Save to the database (Optional due to @Transactional, but it is a good practice to leave it explicit)
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
}