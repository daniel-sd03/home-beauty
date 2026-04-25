package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sodresoftwares.homebeauty.model.ProfessionalBlock;

import java.time.LocalDateTime;
import java.util.List;

public interface ProfessionalBlockRepository  extends JpaRepository<ProfessionalBlock, String> {
    List<ProfessionalBlock> findByProfessionalIdOrderByStartDateTimeAsc(String professionalId);

    @Query("SELECT COUNT(b) > 0 FROM ProfessionalBlock b " +
            "WHERE b.professional.id = :profileId " +
            "AND b.startDateTime < :endTime " +
            "AND b.endDateTime > :startTime")
    boolean hasOverlappingBlocks(
            @Param("profileId") String profileId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
