package ma.ac.emi.studentmanagement.controller;

import ma.ac.emi.studentmanagement.dto.EnrollmentDto;
import ma.ac.emi.studentmanagement.entity.Student;
import ma.ac.emi.studentmanagement.service.EnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentController Unit Tests")
class EnrollmentControllerTest {

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private EnrollmentController enrollmentController;

    private Student authStudent;

    @BeforeEach
    void setUp() {
        authStudent = new Student();
        authStudent.setId(5L);
        authStudent.setName("Sarah Connor");
        authStudent.setEmail("sarah@emi.ac.ma");
    }

    @Nested
    @DisplayName("getMyEnrollments(Student)")
    class GetMyEnrollmentsTests {

        @Test
        @DisplayName("Should return student enrollments from service")
        void shouldReturnStudentEnrollments() {
            // Arrange
            EnrollmentDto dto = new EnrollmentDto(10L, 100L, "Cloud Computing", LocalDateTime.now());
            when(enrollmentService.getStudentEnrollments(authStudent.getId())).thenReturn(List.of(dto));

            // Act
            List<EnrollmentDto> result = enrollmentController.getMyEnrollments(authStudent);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(dto, result.get(0));
            verify(enrollmentService, times(1)).getStudentEnrollments(authStudent.getId());
        }
    }

    @Nested
    @DisplayName("enroll(Student, Long)")
    class EnrollTests {

        @Test
        @DisplayName("Should delegate enrollment to service and return EnrollmentDto")
        void shouldEnrollStudent() {
            // Arrange
            Long courseId = 200L;
            EnrollmentDto dto = new EnrollmentDto(12L, courseId, "DevOps", LocalDateTime.now());
            when(enrollmentService.enroll(authStudent.getId(), courseId)).thenReturn(dto);

            // Act
            EnrollmentDto result = enrollmentController.enroll(authStudent, courseId);

            // Assert
            assertNotNull(result);
            assertEquals(dto, result);
            verify(enrollmentService, times(1)).enroll(authStudent.getId(), courseId);
        }
    }

    @Nested
    @DisplayName("cancel(Student, Long)")
    class CancelTests {

        @Test
        @DisplayName("Should delegate cancel to service")
        void shouldCancelEnrollment() {
            // Arrange
            Long courseId = 200L;
            doNothing().when(enrollmentService).cancel(authStudent.getId(), courseId);

            // Act
            enrollmentController.cancel(authStudent, courseId);

            // Assert
            verify(enrollmentService, times(1)).cancel(authStudent.getId(), courseId);
        }
    }
}
