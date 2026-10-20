package ma.ac.emi.studentmanagement.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Enrollment Entity Unit Tests")
class EnrollmentTest {

    @Test
    @DisplayName("Should initialize enrolledAt timestamp upon instantiation")
    void shouldInitializeEnrolledAtByDefault() {
        Enrollment enrollment = new Enrollment();
        assertNotNull(enrollment.getEnrolledAt());
    }

    @Test
    @DisplayName("Should verify getters and setters for all enrollment fields")
    void shouldVerifyGettersAndSetters() {
        Enrollment enrollment = new Enrollment();
        Student student = new Student();
        student.setId(1L);
        Course course = new Course();
        course.setId(2L);
        LocalDateTime customTime = LocalDateTime.of(2026, 10, 13, 10, 0);

        enrollment.setId(100L);
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrolledAt(customTime);

        assertEquals(100L, enrollment.getId());
        assertEquals(student, enrollment.getStudent());
        assertEquals(course, enrollment.getCourse());
        assertEquals(customTime, enrollment.getEnrolledAt());
    }
}
