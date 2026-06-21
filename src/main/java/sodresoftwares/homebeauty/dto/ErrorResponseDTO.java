package sodresoftwares.homebeauty.dto;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
        LocalDateTime timestamp,
        Integer status,
        String error,        String errorCode,
        String message,
        String path
) {}
