package sodresoftwares.homebeauty.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import sodresoftwares.homebeauty.infra.exception.AppException;
import sodresoftwares.homebeauty.dto.SpecialtyRequestDTO;
import sodresoftwares.homebeauty.dto.SpecialtyResponseDTO;
import sodresoftwares.homebeauty.model.Specialty;
import sodresoftwares.homebeauty.repositories.SpecialtyRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialtyServiceTest {

    @Mock
    private SpecialtyRepository repository;

    @InjectMocks
    private SpecialtyService service;

    private final String MOCK_ID = "123e4567-e89b-12d3-a456-426614174000";

    private Specialty validSpecialty;
    private Specialty updatedSpecialty;
    private SpecialtyRequestDTO validRequestDTO;
    private SpecialtyRequestDTO updateRequestDTO;

    @BeforeEach
    void setUp() {
        validSpecialty = new Specialty(MOCK_ID, "Manicure");
        validRequestDTO = new SpecialtyRequestDTO("Manicure");

        updatedSpecialty = new Specialty(MOCK_ID, "Esteticista");
        updateRequestDTO = new SpecialtyRequestDTO("Esteticista");
    }

    // ==================== CREATE SPECIALTY TESTS ====================

    @Test
    @DisplayName("Should create a specialty successfully")
    void create_Success() {
        // Arrange
        when(repository.existsByNameIgnoreCase(validRequestDTO.name())).thenReturn(false);
        when(repository.save(any(Specialty.class))).thenReturn(validSpecialty);

        // Act
        SpecialtyResponseDTO response = service.create(validRequestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(MOCK_ID, response.id());
        assertEquals("Manicure", response.name());
        verify(repository, times(1)).save(any(Specialty.class));
    }

    @Test
    @DisplayName("Should throw 409 Conflict when creating an existing specialty")
    void create_Conflict() {
        // Arrange
        when(repository.existsByNameIgnoreCase(validRequestDTO.name())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.create(validRequestDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "SPECIALTY_ALREADY_EXISTS")
                .hasMessage("Specialty name already exists.");

        verify(repository, never()).save(any());
    }

    // ==================== FIND ALL SPECIALTY TESTS ====================

    @Test
    @DisplayName("Should return a list of all specialties")
    void findAll_Success() {
        // Arrange
        Specialty spec2 = new Specialty("id-2", "Cabeleireira");
        when(repository.findAll()).thenReturn(List.of(validSpecialty, spec2));

        // Act
        List<SpecialtyResponseDTO> response = service.findAll();

        // Assert
        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("Manicure", response.get(0).name());
        assertEquals("Cabeleireira", response.get(1).name());
        verify(repository, times(1)).findAll();
    }

    // ==================== FIND BY ID SPECIALTY TESTS ====================

    @Test
    @DisplayName("Should return a specialty by ID")
    void findById_Success() {
        // Arrange
        when(repository.findById(MOCK_ID)).thenReturn(Optional.of(validSpecialty));

        // Act
        SpecialtyResponseDTO response = service.findById(MOCK_ID);

        // Assert
        assertNotNull(response);
        assertEquals("Manicure", response.name());
    }

    @Test
    @DisplayName("Should throw 404 Not Found when ID does not exist")
    void findById_NotFound() {
        // Arrange
        when(repository.findById(MOCK_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.findById(MOCK_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "SPECIALTY_NOT_FOUND")
                .hasMessage("Specialty not found.");
    }

    // ==================== UPDATE SPECIALTY TESTS ====================

    @Test
    @DisplayName("Should update an existing specialty successfully")
    void update_Success() {
        // Arrange
        when(repository.findById(MOCK_ID)).thenReturn(Optional.of(validSpecialty));
        when(repository.existsByNameIgnoreCase(updateRequestDTO.name())).thenReturn(false);
        when(repository.save(any(Specialty.class))).thenReturn(updatedSpecialty);

        // Act
        SpecialtyResponseDTO response = service.update(MOCK_ID, updateRequestDTO);

        // Assert
        assertNotNull(response);
        assertEquals("Esteticista", response.name());
        verify(repository, times(1)).save(any(Specialty.class));
    }

    @Test
    @DisplayName("Should throw 404 Not Found when updating non-existent specialty")
    void update_NotFound() {
        // Arrange
        when(repository.findById(MOCK_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.update(MOCK_ID, updateRequestDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "SPECIALTY_NOT_FOUND")
                .hasMessage("Specialty not found.");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw 409 Conflict when updating to an already existing name")
    void update_Conflict() {
        // Arrange
        when(repository.findById(MOCK_ID)).thenReturn(Optional.of(validSpecialty));
        when(repository.existsByNameIgnoreCase(updateRequestDTO.name())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.update(MOCK_ID, updateRequestDTO))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "SPECIALTY_ALREADY_EXISTS")
                .hasMessage("Specialty name already exists.");
        verify(repository, never()).save(any());
    }

    // ==================== DELETE SPECIALTY TESTS ====================

    @Test
    @DisplayName("Should delete a specialty successfully")
    void delete_Success() {
        // Arrange
        when(repository.existsById(MOCK_ID)).thenReturn(true);

        // Act
        service.delete(MOCK_ID);

        // Assert
        verify(repository, times(1)).existsById(MOCK_ID);
        verify(repository, times(1)).deleteById(MOCK_ID);
    }

    @Test
    @DisplayName("Should throw 404 Not Found when deleting non-existent specialty")
    void delete_NotFound() {
        // Arrange
        when(repository.existsById(MOCK_ID)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> service.delete(MOCK_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "SPECIALTY_NOT_FOUND")
                .hasMessage("Specialty not found.");
        verify(repository, never()).deleteById(any());
    }
}