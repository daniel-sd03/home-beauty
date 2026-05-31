package sodresoftwares.homebeauty.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import sodresoftwares.homebeauty.model.ProfessionalBlock;
import sodresoftwares.homebeauty.model.ProfessionalProfile;
import sodresoftwares.homebeauty.model.user.User;
import sodresoftwares.homebeauty.model.user.UserRole;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("ProfessionalBlockRepository - Query tests")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProfessionalBlockRepositoryTest {

    @Autowired
    private ProfessionalBlockRepository blockRepository;

    @Autowired
    private ProfessionalProfileRepository professionalProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private ProfessionalProfile profile;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .firstName("Prof")
                .lastName("User")
                .login("prof@test.com")
                .password("pass")
                .role(UserRole.PROFESSIONAL)
                .build();
        user = userRepository.save(user);

        profile = ProfessionalProfile.builder()
                .description("Test profile")
                .user(user)
                .build();
        profile = professionalProfileRepository.save(profile);

        baseTime = LocalDateTime.of(2026, 5, 17, 8, 0, 0);
    }

    // ==================== hasOverlappingBlocks ====================

    @Test
    @DisplayName("hasOverlappingBlocks should detect overlaps for the same professional")
    void testHasOverlappingBlocksDetectsOverlap() {
        // existing block from 08:00 to 09:00
        LocalDateTime existingStart = baseTime;
        LocalDateTime existingEnd = baseTime.plusHours(1);
        createAndSaveBlock(profile, existingStart, existingEnd);

        // completely overlapping
        assertThat(blockRepository.hasOverlappingBlocks(
                profile.getId(), existingStart, existingEnd
                )).isTrue();

        // starts inside existing
        assertThat(blockRepository.hasOverlappingBlocks(
                profile.getId(), existingStart.plusMinutes(15), existingEnd.plusHours(1)
        )).isTrue();

        // ends inside existing
        assertThat(blockRepository.hasOverlappingBlocks(
                profile.getId(), existingStart.minusMinutes(30), existingStart.plusMinutes(30)
        )).isTrue();

        // contained within existing
        assertThat(blockRepository.hasOverlappingBlocks(
                profile.getId(), existingStart.plusMinutes(10), existingEnd.minusMinutes(10)
        )).isTrue();
    }

    @Test
    @DisplayName("hasOverlappingBlocks should return false when there is no overlap or for different professional")
    void testHasOverlappingBlocksReturnsFalseWhenNoOverlapOrDifferentProfessional() {
        // existing block from 08:00 to 09:00
        LocalDateTime existingStart = baseTime;
        LocalDateTime existingEnd = baseTime.plusHours(1);
        createAndSaveBlock(profile, existingStart, existingEnd);

        // completely before
        assertThat(blockRepository.hasOverlappingBlocks(
                profile.getId(), existingStart.minusHours(2), existingStart.minusMinutes(1)
        )).isFalse();

        // completely after
        assertThat(blockRepository.hasOverlappingBlocks(
                profile.getId(), existingEnd.plusMinutes(1), existingEnd.plusHours(2)
        )).isFalse();

        // adjacent (touching but not overlapping)
        assertThat(blockRepository.hasOverlappingBlocks(
                profile.getId(), existingEnd, existingEnd.plusHours(1)
        )).isFalse();

        // different professional
        User otherUser = User.builder()
                .firstName("Other")
                .lastName("Prof")
                .login("otherprof@test.com")
                .password("pass")
                .role(UserRole.PROFESSIONAL)
                .build();
        otherUser = userRepository.save(otherUser);

        ProfessionalProfile otherProfile = ProfessionalProfile.builder()
                .description("Other profile")
                .user(otherUser)
                .build();
        otherProfile = professionalProfileRepository.save(otherProfile);

        assertThat(blockRepository.hasOverlappingBlocks(
                otherProfile.getId(), existingStart, existingEnd
        )).isFalse();
    }

    // ==================== findBlocksByDay ====================

    @Test
    @DisplayName("findBlocksByDay should return blocks that intersect the day, including multi-day blocks")
    void testFindBlocksByDay() {
        LocalDateTime dayStart = LocalDateTime.of(2026, 5, 18, 0, 0);
        LocalDateTime dayEnd = LocalDateTime.of(2026, 5, 18, 23, 59, 59);

        LocalDateTime b1 = LocalDateTime.of(2026, 5, 18, 9, 0);
        LocalDateTime b2 = LocalDateTime.of(2026, 5, 18, 12, 0);
        LocalDateTime b3 = LocalDateTime.of(2026, 5, 18, 18, 0);

        createAndSaveBlock(profile, b1, b1.plusHours(1));
        createAndSaveBlock(profile, b2, b2.plusHours(1));
        createAndSaveBlock(profile, b3, b3.plusHours(1));

        // vacation 10 days
        LocalDateTime vacationStart = LocalDateTime.of(2026, 5, 10, 0, 0);
        LocalDateTime vacationEnd = LocalDateTime.of(2026, 5, 20, 23, 59);
        createAndSaveBlock(profile, vacationStart, vacationEnd);

        // should not appear because it overlaps the entire day, not fully contained
        createAndSaveBlock(profile, LocalDateTime.of(2026, 5, 19, 10, 0), LocalDateTime.of(2026, 5, 19, 11, 0));

        List<ProfessionalBlock> results = blockRepository.findBlocksByDay(profile.getId(), dayStart, dayEnd);

        assertThat(results).hasSize(4);
        assertThat(results).extracting(ProfessionalBlock::getStartDateTime)
                .containsExactlyInAnyOrder(b1, b2, b3, vacationStart);
    }

    // helper
    private ProfessionalBlock createAndSaveBlock(ProfessionalProfile profile, LocalDateTime start, LocalDateTime end) {
        ProfessionalBlock block = ProfessionalBlock.builder()
                .title("Block")
                .startDateTime(start)
                .endDateTime(end)
                .professional(profile)
                .build();
        return blockRepository.save(block);
    }
}