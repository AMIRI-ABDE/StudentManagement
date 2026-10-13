package ma.ac.emi.studentmanagement.controller;

import ma.ac.emi.studentmanagement.dto.ProfileUpdateRequest;
import ma.ac.emi.studentmanagement.dto.StudentDto;
import ma.ac.emi.studentmanagement.entity.Role;
import ma.ac.emi.studentmanagement.entity.Student;
import ma.ac.emi.studentmanagement.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileController Unit Tests")
class ProfileControllerTest {

    @Mock
    private StudentRepository studentRepo;

    @InjectMocks
    private ProfileController profileController;

    private Student currentStudent;

    @BeforeEach
    void setUp() {
        currentStudent = new Student();
        currentStudent.setId(15L);
        currentStudent.setName("David");
        currentStudent.setEmail("david@emi.ac.ma");
        currentStudent.setRole(Role.STUDENT);
    }

    @Nested
    @DisplayName("getProfile(Student)")
    class GetProfileTests {

        @Test
        @DisplayName("Should return current student profile DTO")
        void shouldReturnProfileDto() {
            // Act
            StudentDto dto = profileController.getProfile(currentStudent);

            // Assert
            assertNotNull(dto);
            assertEquals(15L, dto.id());
            assertEquals("David", dto.name());
            assertEquals("david@emi.ac.ma", dto.email());
            assertEquals("STUDENT", dto.role());
            verifyNoInteractions(studentRepo);
        }
    }

    @Nested
    @DisplayName("updateProfile(Student, ProfileUpdateRequest)")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update student name and return updated DTO")
        void shouldUpdateProfileSuccessfully() {
            // Arrange
            ProfileUpdateRequest request = new ProfileUpdateRequest("David Updated");
            when(studentRepo.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            StudentDto result = profileController.updateProfile(currentStudent, request);

            // Assert
            assertNotNull(result);
            assertEquals("David Updated", result.name());
            assertEquals(currentStudent.getId(), result.id());
            assertEquals(currentStudent.getEmail(), result.email());
            verify(studentRepo, times(1)).save(currentStudent);
        }
    }
}
