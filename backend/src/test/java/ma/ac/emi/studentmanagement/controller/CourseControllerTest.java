package ma.ac.emi.studentmanagement.controller;

import ma.ac.emi.studentmanagement.dto.CourseDto;
import ma.ac.emi.studentmanagement.entity.Course;
import ma.ac.emi.studentmanagement.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseController Unit Tests")
class CourseControllerTest {

    @Mock
    private CourseRepository courseRepo;

    @InjectMocks
    private CourseController courseController;

    private Course sampleCourse;

    @BeforeEach
    void setUp() {
        sampleCourse = new Course();
        sampleCourse.setId(1L);
        sampleCourse.setTitle("DevOps & CI/CD");
        sampleCourse.setMaxCapacity(40);
        sampleCourse.setCurrentEnrollment(10);
    }

    @Nested
    @DisplayName("getAllCourses()")
    class GetAllCoursesTests {

        @Test
        @DisplayName("Should return list of CourseDto when courses exist")
        void shouldReturnCourseDtoList() {
            // Arrange
            when(courseRepo.findAll()).thenReturn(List.of(sampleCourse));

            // Act
            List<CourseDto> result = courseController.getAllCourses();

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            CourseDto dto = result.get(0);
            assertEquals(sampleCourse.getId(), dto.id());
            assertEquals(sampleCourse.getTitle(), dto.title());
            assertEquals(sampleCourse.getMaxCapacity(), dto.maxCapacity());
            assertEquals(sampleCourse.getCurrentEnrollment(), dto.currentEnrollment());
            assertFalse(dto.full());

            verify(courseRepo, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no courses exist")
        void shouldReturnEmptyListWhenNoCourses() {
            // Arrange
            when(courseRepo.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<CourseDto> result = courseController.getAllCourses();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(courseRepo, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("getCourse(Long id)")
    class GetCourseTests {

        @Test
        @DisplayName("Should return CourseDto when course is found")
        void shouldReturnCourseDtoWhenFound() {
            // Arrange
            when(courseRepo.findById(1L)).thenReturn(Optional.of(sampleCourse));

            // Act
            CourseDto result = courseController.getCourse(1L);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.id());
            assertEquals("DevOps & CI/CD", result.title());
            verify(courseRepo, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should return CourseDto with full=true when course has reached full capacity")
        void shouldReturnCourseDtoWithFullTrueWhenCourseIsFull() {
            // Arrange
            Course fullCourse = new Course();
            fullCourse.setId(2L);
            fullCourse.setTitle("Cloud Computing");
            fullCourse.setMaxCapacity(30);
            fullCourse.setCurrentEnrollment(30);

            when(courseRepo.findById(2L)).thenReturn(Optional.of(fullCourse));

            // Act
            CourseDto result = courseController.getCourse(2L);

            // Assert
            assertNotNull(result);
            assertEquals(2L, result.id());
            assertEquals("Cloud Computing", result.title());
            assertEquals(30, result.maxCapacity());
            assertEquals(30, result.currentEnrollment());
            assertTrue(result.full());
            verify(courseRepo, times(1)).findById(2L);
        }

        @Test
        @DisplayName("Should throw 404 NOT_FOUND when course is not found")
        void shouldThrowNotFoundWhenCourseMissing() {
            // Arrange
            when(courseRepo.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> courseController.getCourse(99L)
            );

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals("Course not found", ex.getReason());
            verify(courseRepo, times(1)).findById(99L);
        }
    }

    @Nested
    @DisplayName("createCourse(Course course)")
    class CreateCourseTests {

        @Test
        @DisplayName("Should initialize enrollment to 0, save, and return CourseDto")
        void shouldCreateCourseSuccessfully() {
            // Arrange
            Course newCourse = new Course();
            newCourse.setTitle("Machine Learning");
            newCourse.setMaxCapacity(50);
            newCourse.setCurrentEnrollment(15); // should be reset to 0

            when(courseRepo.save(any(Course.class))).thenAnswer(inv -> {
                Course c = inv.getArgument(0);
                c.setId(10L);
                return c;
            });

            // Act
            CourseDto result = courseController.createCourse(newCourse);

            // Assert
            assertNotNull(result);
            assertEquals(10L, result.id());
            assertEquals(0, result.currentEnrollment());
            assertEquals("Machine Learning", result.title());
            assertEquals(0, newCourse.getCurrentEnrollment());
            verify(courseRepo, times(1)).save(newCourse);
        }
    }

    @Nested
    @DisplayName("updateCourse(Long id, Course updated)")
    class UpdateCourseTests {

        @Test
        @DisplayName("Should update course fields and return updated CourseDto")
        void shouldUpdateCourseSuccessfully() {
            // Arrange
            Course updatedInput = new Course();
            updatedInput.setTitle("Advanced DevOps");
            updatedInput.setMaxCapacity(45);

            when(courseRepo.findById(1L)).thenReturn(Optional.of(sampleCourse));
            when(courseRepo.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            CourseDto result = courseController.updateCourse(1L, updatedInput);

            // Assert
            assertNotNull(result);
            assertEquals("Advanced DevOps", result.title());
            assertEquals(45, result.maxCapacity());
            verify(courseRepo, times(1)).findById(1L);
            verify(courseRepo, times(1)).save(sampleCourse);
        }

        @Test
        @DisplayName("Should throw 404 NOT_FOUND when updating non-existent course")
        void shouldThrowNotFoundWhenUpdatingMissingCourse() {
            // Arrange
            Course updatedInput = new Course();
            updatedInput.setTitle("Any Title");
            when(courseRepo.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> courseController.updateCourse(99L, updatedInput)
            );

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals("Course not found", ex.getReason());
            verify(courseRepo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteCourse(Long id)")
    class DeleteCourseTests {

        @Test
        @DisplayName("Should delete course when course exists")
        void shouldDeleteCourseSuccessfully() {
            // Arrange
            when(courseRepo.existsById(1L)).thenReturn(true);

            // Act
            courseController.deleteCourse(1L);

            // Assert
            verify(courseRepo, times(1)).existsById(1L);
            verify(courseRepo, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw 404 NOT_FOUND when course does not exist")
        void shouldThrowNotFoundWhenDeletingNonExistentCourse() {
            // Arrange
            when(courseRepo.existsById(99L)).thenReturn(false);

            // Act & Assert
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> courseController.deleteCourse(99L)
            );

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals("Course not found", ex.getReason());
            verify(courseRepo, times(1)).existsById(99L);
            verify(courseRepo, never()).deleteById(any());
        }
    }
}
