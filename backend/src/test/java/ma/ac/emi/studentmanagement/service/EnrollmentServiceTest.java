package ma.ac.emi.studentmanagement.service;

import ma.ac.emi.studentmanagement.dto.EnrollmentDto;
import ma.ac.emi.studentmanagement.entity.Course;
import ma.ac.emi.studentmanagement.entity.Enrollment;
import ma.ac.emi.studentmanagement.entity.Role;
import ma.ac.emi.studentmanagement.entity.Student;
import ma.ac.emi.studentmanagement.repository.CourseRepository;
import ma.ac.emi.studentmanagement.repository.EnrollmentRepository;
import ma.ac.emi.studentmanagement.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentService Unit Tests")
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepo;

    @Mock
    private StudentRepository studentRepo;

    @Mock
    private CourseRepository courseRepo;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Student testStudent;
    private Course testCourse;
    private Enrollment testEnrollment;
    private LocalDateTime fixedDateTime;

    @BeforeEach
    void setUp() {
        fixedDateTime = LocalDateTime.of(2026, 10, 6, 12, 0, 0);

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setName("Alice Dupont");
        testStudent.setEmail("alice@emi.ac.ma");
        testStudent.setPassword("encodedPwd123");
        testStudent.setRole(Role.STUDENT);

        testCourse = new Course();
        testCourse.setId(100L);
        testCourse.setTitle("Software Testing & QA");
        testCourse.setMaxCapacity(30);
        testCourse.setCurrentEnrollment(10);

        testEnrollment = new Enrollment();
        testEnrollment.setId(500L);
        testEnrollment.setStudent(testStudent);
        testEnrollment.setCourse(testCourse);
        testEnrollment.setEnrolledAt(fixedDateTime);
    }

    // =========================================================================
    // Nested Suite: getStudentEnrollments(Long)
    // =========================================================================
    @Nested
    @DisplayName("getStudentEnrollments(Long studentId)")
    class GetStudentEnrollmentsTests {

        @Test
        @DisplayName("Should return list of EnrollmentDto when student has existing enrollments")
        void shouldReturnEnrollmentDtoListWhenStudentHasEnrollments() {
            // Arrange
            Long studentId = 1L;
            Course secondCourse = new Course();
            secondCourse.setId(101L);
            secondCourse.setTitle("Distributed Systems");
            secondCourse.setMaxCapacity(25);
            secondCourse.setCurrentEnrollment(15);

            Enrollment secondEnrollment = new Enrollment();
            secondEnrollment.setId(501L);
            secondEnrollment.setStudent(testStudent);
            secondEnrollment.setCourse(secondCourse);
            secondEnrollment.setEnrolledAt(fixedDateTime.plusDays(1));

            List<Enrollment> enrollments = List.of(testEnrollment, secondEnrollment);
            when(enrollmentRepo.findByStudentId(studentId)).thenReturn(enrollments);

            // Act
            List<EnrollmentDto> result = enrollmentService.getStudentEnrollments(studentId);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());

            EnrollmentDto firstDto = result.get(0);
            assertEquals(testEnrollment.getId(), firstDto.id());
            assertEquals(testCourse.getId(), firstDto.courseId());
            assertEquals(testCourse.getTitle(), firstDto.courseTitle());
            assertEquals(fixedDateTime, firstDto.enrolledAt());

            EnrollmentDto secondDto = result.get(1);
            assertEquals(secondEnrollment.getId(), secondDto.id());
            assertEquals(secondCourse.getId(), secondDto.courseId());
            assertEquals(secondCourse.getTitle(), secondDto.courseTitle());
            assertEquals(secondEnrollment.getEnrolledAt(), secondDto.enrolledAt());

            verify(enrollmentRepo, times(1)).findByStudentId(studentId);
            verifyNoInteractions(studentRepo, courseRepo);
        }

        @Test
        @DisplayName("Should return empty list when student has no enrollments")
        void shouldReturnEmptyListWhenStudentHasNoEnrollments() {
            // Arrange
            Long studentId = 99L;
            when(enrollmentRepo.findByStudentId(studentId)).thenReturn(Collections.emptyList());

            // Act
            List<EnrollmentDto> result = enrollmentService.getStudentEnrollments(studentId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(enrollmentRepo, times(1)).findByStudentId(studentId);
            verifyNoInteractions(studentRepo, courseRepo);
        }
    }

    // =========================================================================
    // Nested Suite: enroll(Long, Long)
    // =========================================================================
    @Nested
    @DisplayName("enroll(Long studentId, Long courseId)")
    class EnrollTests {

        @Test
        @DisplayName("Should throw 404 NOT_FOUND when student does not exist")
        void shouldThrowNotFoundWhenStudentDoesNotExist() {
            // Arrange
            Long studentId = 999L;
            Long courseId = 100L;
            when(studentRepo.findById(studentId)).thenReturn(Optional.empty());

            // Act & Assert
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> enrollmentService.enroll(studentId, courseId)
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("Student not found", exception.getReason());

            verify(studentRepo, times(1)).findById(studentId);
            verifyNoInteractions(courseRepo);
            verifyNoInteractions(enrollmentRepo);
        }

        @Test
        @DisplayName("Should throw 404 NOT_FOUND when course does not exist")
        void shouldThrowNotFoundWhenCourseDoesNotExist() {
            // Arrange
            Long studentId = 1L;
            Long courseId = 999L;
            when(studentRepo.findById(studentId)).thenReturn(Optional.of(testStudent));
            when(courseRepo.findById(courseId)).thenReturn(Optional.empty());

            // Act & Assert
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> enrollmentService.enroll(studentId, courseId)
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("Course not found", exception.getReason());

            verify(studentRepo, times(1)).findById(studentId);
            verify(courseRepo, times(1)).findById(courseId);
            verifyNoInteractions(enrollmentRepo);
        }

        @Test
        @DisplayName("Should throw 409 CONFLICT when student is already enrolled in the course")
        void shouldThrowConflictWhenStudentAlreadyEnrolled() {
            // Arrange
            Long studentId = 1L;
            Long courseId = 100L;
            when(studentRepo.findById(studentId)).thenReturn(Optional.of(testStudent));
            when(courseRepo.findById(courseId)).thenReturn(Optional.of(testCourse));
            when(enrollmentRepo.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(true);

            // Act & Assert
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> enrollmentService.enroll(studentId, courseId)
            );

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            assertEquals("Already enrolled in this course", exception.getReason());

            verify(studentRepo, times(1)).findById(studentId);
            verify(courseRepo, times(1)).findById(courseId);
            verify(enrollmentRepo, times(1)).existsByStudentIdAndCourseId(studentId, courseId);
            verify(courseRepo, never()).save(any());
            verify(enrollmentRepo, never()).save(any());
        }

        @Test
        @DisplayName("Should throw 409 CONFLICT when course is at full capacity (current == max)")
        void shouldThrowConflictWhenCourseIsAtExactFullCapacity() {
            // Arrange
            Long studentId = 1L;
            Long courseId = 100L;
            testCourse.setMaxCapacity(20);
            testCourse.setCurrentEnrollment(20); // isFull() returns true (20 >= 20)

            when(studentRepo.findById(studentId)).thenReturn(Optional.of(testStudent));
            when(courseRepo.findById(courseId)).thenReturn(Optional.of(testCourse));
            when(enrollmentRepo.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(false);

            // Act & Assert
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> enrollmentService.enroll(studentId, courseId)
            );

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            assertEquals("Course is full", exception.getReason());

            verify(studentRepo, times(1)).findById(studentId);
            verify(courseRepo, times(1)).findById(courseId);
            verify(enrollmentRepo, times(1)).existsByStudentIdAndCourseId(studentId, courseId);
            verify(courseRepo, never()).save(any());
            verify(enrollmentRepo, never()).save(any());
        }

        @Test
        @DisplayName("Should throw 409 CONFLICT when course exceeds full capacity (current > max)")
        void shouldThrowConflictWhenCourseExceedsCapacity() {
            // Arrange
            Long studentId = 1L;
            Long courseId = 100L;
            testCourse.setMaxCapacity(20);
            testCourse.setCurrentEnrollment(21); // isFull() returns true (21 >= 20)

            when(studentRepo.findById(studentId)).thenReturn(Optional.of(testStudent));
            when(courseRepo.findById(courseId)).thenReturn(Optional.of(testCourse));
            when(enrollmentRepo.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(false);

            // Act & Assert
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> enrollmentService.enroll(studentId, courseId)
            );

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            assertEquals("Course is full", exception.getReason());

            verify(courseRepo, never()).save(any());
            verify(enrollmentRepo, never()).save(any());
        }

        @Test
        @DisplayName("Should enroll student successfully, increment course enrollment, and return EnrollmentDto")
        void shouldEnrollStudentSuccessfullyWhenCapacityAvailable() {
            // Arrange
            Long studentId = 1L;
            Long courseId = 100L;
            int initialEnrollment = 10;
            testCourse.setMaxCapacity(30);
            testCourse.setCurrentEnrollment(initialEnrollment);

            when(studentRepo.findById(studentId)).thenReturn(Optional.of(testStudent));
            when(courseRepo.findById(courseId)).thenReturn(Optional.of(testCourse));
            when(enrollmentRepo.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(false);

            // Mock saving the enrollment entity
            when(enrollmentRepo.save(any(Enrollment.class))).thenAnswer(invocation -> {
                Enrollment saved = invocation.getArgument(0);
                saved.setId(777L);
                saved.setEnrolledAt(fixedDateTime);
                return saved;
            });

            // Act
            EnrollmentDto result = enrollmentService.enroll(studentId, courseId);

            // Assert
            assertNotNull(result);
            assertEquals(777L, result.id());
            assertEquals(testCourse.getId(), result.courseId());
            assertEquals(testCourse.getTitle(), result.courseTitle());
            assertEquals(fixedDateTime, result.enrolledAt());

            // State verification: Course current enrollment must be incremented by 1
            assertEquals(initialEnrollment + 1, testCourse.getCurrentEnrollment());
            verify(courseRepo, times(1)).save(testCourse);

            // Capture and verify Enrollment entity passed to repository
            ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
            verify(enrollmentRepo, times(1)).save(enrollmentCaptor.capture());
            Enrollment capturedEnrollment = enrollmentCaptor.getValue();
            assertEquals(testStudent, capturedEnrollment.getStudent());
            assertEquals(testCourse, capturedEnrollment.getCourse());
        }

        @Test
        @DisplayName("Should enroll student successfully when course is one slot below capacity boundary")
        void shouldEnrollStudentSuccessfullyAtBoundarySlot() {
            // Arrange
            Long studentId = 1L;
            Long courseId = 100L;
            int initialEnrollment = 29;
            testCourse.setMaxCapacity(30);
            testCourse.setCurrentEnrollment(initialEnrollment); // 29 < 30 -> not full

            when(studentRepo.findById(studentId)).thenReturn(Optional.of(testStudent));
            when(courseRepo.findById(courseId)).thenReturn(Optional.of(testCourse));
            when(enrollmentRepo.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(false);

            when(enrollmentRepo.save(any(Enrollment.class))).thenAnswer(invocation -> {
                Enrollment saved = invocation.getArgument(0);
                saved.setId(888L);
                return saved;
            });

            // Act
            EnrollmentDto result = enrollmentService.enroll(studentId, courseId);

            // Assert
            assertNotNull(result);
            assertEquals(888L, result.id());
            assertEquals(30, testCourse.getCurrentEnrollment()); // Reached full capacity now
            verify(courseRepo, times(1)).save(testCourse);
            verify(enrollmentRepo, times(1)).save(any(Enrollment.class));
        }
    }

    // =========================================================================
    // Nested Suite: cancel(Long, Long)
    // =========================================================================
    @Nested
    @DisplayName("cancel(Long studentId, Long courseId)")
    class CancelTests {

        @Test
        @DisplayName("Should throw 404 NOT_FOUND when enrollment record does not exist")
        void shouldThrowNotFoundWhenEnrollmentDoesNotExist() {
            // Arrange
            Long studentId = 1L;
            Long courseId = 100L;
            when(enrollmentRepo.findByStudentIdAndCourseId(studentId, courseId)).thenReturn(Optional.empty());

            // Act & Assert
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> enrollmentService.cancel(studentId, courseId)
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("Enrollment not found", exception.getReason());

            verify(enrollmentRepo, times(1)).findByStudentIdAndCourseId(studentId, courseId);
            verify(courseRepo, never()).save(any());
            verify(enrollmentRepo, never()).delete(any());
        }

        @Test
        @DisplayName("Should cancel enrollment, decrement course enrollment, and delete enrollment record")
        void shouldCancelEnrollmentSuccessfullyWhenEnrollmentGreaterThanZero() {
            // Arrange
            Long studentId = 1L;
            Long courseId = 100L;
            int initialEnrollment = 10;
            testCourse.setCurrentEnrollment(initialEnrollment);
            testEnrollment.setCourse(testCourse);

            when(enrollmentRepo.findByStudentIdAndCourseId(studentId, courseId))
                    .thenReturn(Optional.of(testEnrollment));

            // Act
            enrollmentService.cancel(studentId, courseId);

            // Assert
            assertEquals(initialEnrollment - 1, testCourse.getCurrentEnrollment());
            verify(courseRepo, times(1)).save(testCourse);
            verify(enrollmentRepo, times(1)).delete(testEnrollment);
        }

        @Test
        @DisplayName("Should clamp currentEnrollment to zero (Math.max branch) when currentEnrollment is zero")
        void shouldClampEnrollmentToZeroWhenCurrentEnrollmentIsAlreadyZero() {
            // Arrange
            Long studentId = 1L;
            Long courseId = 100L;
            testCourse.setCurrentEnrollment(0); // Boundary: 0 - 1 = -1 -> clamped to 0
            testEnrollment.setCourse(testCourse);

            when(enrollmentRepo.findByStudentIdAndCourseId(studentId, courseId))
                    .thenReturn(Optional.of(testEnrollment));

            // Act
            enrollmentService.cancel(studentId, courseId);

            // Assert
            assertEquals(0, testCourse.getCurrentEnrollment());
            verify(courseRepo, times(1)).save(testCourse);
            verify(enrollmentRepo, times(1)).delete(testEnrollment);
        }

        @Test
        @DisplayName("Should decrement currentEnrollment from 1 to 0 on lower positive boundary")
        void shouldDecrementEnrollmentFromOneToZero() {
            // Arrange
            Long studentId = 1L;
            Long courseId = 100L;
            testCourse.setCurrentEnrollment(1);
            testEnrollment.setCourse(testCourse);

            when(enrollmentRepo.findByStudentIdAndCourseId(studentId, courseId))
                    .thenReturn(Optional.of(testEnrollment));

            // Act
            enrollmentService.cancel(studentId, courseId);

            // Assert
            assertEquals(0, testCourse.getCurrentEnrollment());
            verify(courseRepo, times(1)).save(testCourse);
            verify(enrollmentRepo, times(1)).delete(testEnrollment);
        }
    }
}
