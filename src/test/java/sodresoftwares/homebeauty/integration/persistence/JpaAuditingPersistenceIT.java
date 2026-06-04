package sodresoftwares.homebeauty.integration.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import sodresoftwares.homebeauty.config.AuditConfig;
import sodresoftwares.homebeauty.model.*;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;
import sodresoftwares.homebeauty.repositories.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(AuditConfig.class)
@DisplayName("Auditing - Granular Persistence Integration Tests")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class JpaAuditingPersistenceIT {

    @Autowired private UserRepository userRepository;
    @Autowired private ProfessionalProfileRepository profileRepository;
    @Autowired private StateRepository stateRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private WorkingHourRepository workingHourRepository;
    @Autowired private ProfessionalBlockRepository blockRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProvidedServiceRepository providedServiceRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private ReviewRepository reviewRepository;

    private final String auditor = "test-auditor";

    private User testClient;
    private User testProUser;
    private ProfessionalProfile testProfile;
    private City testCity;
    private Category testCategory;
    private Address testAddress;
    private ProvidedService testProvidedService;
    private Appointment testAppointment;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(auditor, "na", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        State state = stateRepository.saveAndFlush(createDummyStateSpec());
        testCity = cityRepository.saveAndFlush(createDummyCitySpec(state));
        testCategory = categoryRepository.saveAndFlush(createDummyCategorySpec());

        testClient = userRepository.saveAndFlush(createDummyUser("client@test.com"));
        testProUser = userRepository.saveAndFlush(createDummyUser("professional@test.com"));

        testProfile = profileRepository.saveAndFlush(createDummyProfileSpec(testProUser));
        testAddress = addressRepository.saveAndFlush(createDummyAddressSpec());
        testProvidedService = providedServiceRepository.saveAndFlush(createDummyServiceSpec());
        testAppointment = appointmentRepository.saveAndFlush(createDummyAppointmentSpec(testProvidedService, testAddress));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- User Tests ---

    @Test
    @DisplayName("User - Create: Should populate structural audit fields")
    void user_create_shouldPopulateAuditFields() {
        User saved = userRepository.saveAndFlush(createDummyUser("fresh-user@test.com"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo(auditor);
    }

    @Test
    @DisplayName("User - Update: Should refresh update fields")
    void user_update_shouldPopulateAuditFields() {
        LocalDateTime originalCreated = testClient.getCreatedAt();

        testClient.setPhone("11922222222");
        User updated = userRepository.saveAndFlush(testClient);

        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedBy()).isEqualTo(auditor);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalCreated);
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreated);
    }

    // --- Professional Profile Tests ---

    @Test
    @DisplayName("ProfessionalProfile - Create: Should populate structural audit fields")
    void profile_create_shouldPopulateAuditFields() {
        User freshPro = userRepository.saveAndFlush(createDummyUser("fresh-pro@test.com"));
        ProfessionalProfile saved = profileRepository.saveAndFlush(createDummyProfileSpec(freshPro));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo(auditor);
    }

    @Test
    @DisplayName("ProfessionalProfile - Update: Should refresh update fields")
    void profile_update_shouldPopulateAuditFields() {
        LocalDateTime originalCreated = testProfile.getCreatedAt();

        testProfile.setDescription("Updated description");
        ProfessionalProfile updated = profileRepository.saveAndFlush(testProfile);

        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedBy()).isEqualTo(auditor);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalCreated);
    }

    // --- Address Tests ---

    @Test
    @DisplayName("Address - Create: Should populate structural audit fields")
    void address_create_shouldPopulateAuditFields() {
        Address saved = addressRepository.saveAndFlush(createDummyAddressSpec());

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo(auditor);
    }

    @Test
    @DisplayName("Address - Update: Should refresh update fields")
    void address_update_shouldPopulateAuditFields() {
        LocalDateTime originalCreated = testAddress.getCreatedAt();

        testAddress.setStreet("Rua Alterada");
        Address updated = addressRepository.saveAndFlush(testAddress);

        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedBy()).isEqualTo(auditor);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalCreated);
    }

    // --- Working Hour Tests ---

    @Test
    @DisplayName("WorkingHour - Create: Should populate structural audit fields")
    void workingHour_create_shouldPopulateAuditFields() {
        WorkingHour wh = WorkingHour.builder()
                .professional(testProfile).dayOfWeek(java.time.DayOfWeek.MONDAY)
                .startTime(java.time.LocalTime.of(9,0)).endTime(java.time.LocalTime.of(18,0)).build();

        WorkingHour saved = workingHourRepository.saveAndFlush(wh);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo(auditor);
    }

    @Test
    @DisplayName("WorkingHour - Update: Should refresh update fields")
    void workingHour_update_shouldPopulateAuditFields() {
        WorkingHour saved = workingHourRepository.saveAndFlush(WorkingHour.builder()
                .professional(testProfile).dayOfWeek(java.time.DayOfWeek.MONDAY)
                .startTime(java.time.LocalTime.of(9,0)).endTime(java.time.LocalTime.of(18,0)).build());
        LocalDateTime originalCreated = saved.getCreatedAt();

        saved.setStartTime(java.time.LocalTime.of(10, 0));
        WorkingHour updated = workingHourRepository.saveAndFlush(saved);

        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedBy()).isEqualTo(auditor);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalCreated);
    }

    // --- Professional Block Tests ---

    @Test
    @DisplayName("ProfessionalBlock - Create: Should populate structural audit fields")
    void block_create_shouldPopulateAuditFields() {
        ProfessionalBlock block = ProfessionalBlock.builder()
                .professional(testProfile).title("Block Out")
                .startDateTime(LocalDateTime.now().plusDays(1)).endDateTime(LocalDateTime.now().plusDays(1).plusHours(1)).build();

        ProfessionalBlock saved = blockRepository.saveAndFlush(block);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo(auditor);
    }

    @Test
    @DisplayName("ProfessionalBlock - Update: Should refresh update fields")
    void block_update_shouldPopulateAuditFields() {
        ProfessionalBlock saved = blockRepository.saveAndFlush(ProfessionalBlock.builder()
                .professional(testProfile).title("Block Out")
                .startDateTime(LocalDateTime.now().plusDays(1)).endDateTime(LocalDateTime.now().plusDays(1).plusHours(1)).build());
        LocalDateTime originalCreated = saved.getCreatedAt();

        saved.setTitle("Block Out - Updated");
        ProfessionalBlock updated = blockRepository.saveAndFlush(saved);

        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedBy()).isEqualTo(auditor);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalCreated);
    }

    // --- Provided Service Tests ---

    @Test
    @DisplayName("ProvidedService - Create: Should populate structural audit fields")
    void service_create_shouldPopulateAuditFields() {
        ProvidedService saved = providedServiceRepository.saveAndFlush(createDummyServiceSpec());

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo(auditor);
    }

    @Test
    @DisplayName("ProvidedService - Update: Should refresh update fields")
    void service_update_shouldPopulateAuditFields() {
        LocalDateTime originalCreated = testProvidedService.getCreatedAt();

        testProvidedService.setPrice(BigDecimal.valueOf(120.00));
        ProvidedService updated = providedServiceRepository.saveAndFlush(testProvidedService);

        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedBy()).isEqualTo(auditor);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalCreated);
    }

    // --- Appointment Tests ---

    @Test
    @DisplayName("Appointment - Create: Should populate structural audit fields")
    void appointment_create_shouldPopulateAuditFields() {
        Appointment saved = appointmentRepository.saveAndFlush(createDummyAppointmentSpec(testProvidedService, testAddress));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo(auditor);
    }

    @Test
    @DisplayName("Appointment - Update: Should refresh update fields")
    void appointment_update_shouldPopulateAuditFields() {
        LocalDateTime originalCreated = testAppointment.getCreatedAt();

        testAppointment.setNotes("Client called changing details");
        Appointment updated = appointmentRepository.saveAndFlush(testAppointment);

        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedBy()).isEqualTo(auditor);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalCreated);
    }

    // --- Review Tests ---

    @Test
    @DisplayName("Review - Create: Should populate structural audit fields")
    void review_create_shouldPopulateAuditFields() {
        Review review = Review.builder()
                .appointment(testAppointment).client(testClient)
                .professionalUser(testProUser).rating(5).comment("Perfeito").build();

        Review saved = reviewRepository.saveAndFlush(review);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo(auditor);
    }

    @Test
    @DisplayName("Review - Update: Should refresh update fields")
    void review_update_shouldPopulateAuditFields() {
        Review saved = reviewRepository.saveAndFlush(Review.builder()
                .appointment(testAppointment).client(testClient)
                .professionalUser(testProUser).rating(5).comment("Bom").build());
        LocalDateTime originalCreated = saved.getCreatedAt();

        saved.setComment("Excelente!");
        Review updated = reviewRepository.saveAndFlush(saved);

        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedBy()).isEqualTo(auditor);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalCreated);
    }

    // --- Helper Spec Methods ---

    private User createDummyUser(String email) {
        return User.builder()
                .login(email)
                .password("pwd")
                .firstName("Audit")
                .lastName("User")
                .role(UserRole.USER)
                .build();
    }

    private State createDummyStateSpec() {
        return State.builder().uf("SP").name("Sao Paulo").build();
    }

    private City createDummyCitySpec(State state) {
        return City.builder().name("Sao Paulo").state(state).build();
    }

    private Category createDummyCategorySpec() {
        return Category.builder().name("Cat").iconName("ic").build();
    }

    private ProfessionalProfile createDummyProfileSpec(User user) {
        return ProfessionalProfile.builder()
                .user(user).description("Initial Description").build();
    }

    private Address createDummyAddressSpec() {
        return Address.builder()
                .user(testClient).city(testCity)
                .street("Rua Teste").neighborhood("Centro").zipCode("01234567").build();
    }

    private ProvidedService createDummyServiceSpec() {
        return ProvidedService.builder()
                .professional(testProfile).category(testCategory)
                .name("Corte Simples").price(BigDecimal.valueOf(50.00)).durationMinutes(30)
                .locationType(sodresoftwares.homebeauty.enums.ServiceLocationType.PROVIDER_LOCATION_ONLY).build();
    }

    private Appointment createDummyAppointmentSpec(ProvidedService svc, Address addr) {
        return Appointment.builder()
                .client(testClient).professionalUser(testProUser).service(svc).address(addr)
                .serviceName(svc.getName()).categoryName(testCategory.getName()).professionalName(testProUser.getFullName()).price(svc.getPrice())
                .appointmentType(sodresoftwares.homebeauty.enums.AppointmentType.PROVIDER_LOCATION)
                .startTime(LocalDateTime.now().plusDays(2)).endTime(LocalDateTime.now().plusDays(2).plusHours(1))
                .status(sodresoftwares.homebeauty.enums.AppointmentStatus.PENDING).build();
    }
}