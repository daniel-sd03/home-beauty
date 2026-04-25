package sodresoftwares.homebeauty.dto;
import java.time.LocalDateTime;

public record ProfessionalBlockResponseDTO(
        String id,
        String title,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {}