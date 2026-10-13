package ma.ac.emi.studentmanagement.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Course Entity Unit Tests")
class CourseTest {

    @Test
    @DisplayName("Should return true when currentEnrollment is equal to maxCapacity")
    void shouldReturnTrueWhenCapacityExact() {
        Course course = new Course();
        course.setMaxCapacity(20);
        course.setCurrentEnrollment(20);
        assertTrue(course.isFull());
    }

    @Test
    @DisplayName("Should return true when currentEnrollment exceeds maxCapacity")
    void shouldReturnTrueWhenCapacityExceeded() {
        Course course = new Course();
        course.setMaxCapacity(20);
        course.setCurrentEnrollment(21);
        assertTrue(course.isFull());
    }

    @Test
    @DisplayName("Should return false when currentEnrollment is less than maxCapacity")
    void shouldReturnFalseWhenCapacityAvailable() {
        Course course = new Course();
        course.setMaxCapacity(20);
        course.setCurrentEnrollment(19);
        assertFalse(course.isFull());
    }

    @Test
    @DisplayName("Should verify getters and setters")
    void shouldVerifyGettersAndSetters() {
        Course course = new Course();
        course.setId(99L);
        course.setTitle("Software Architecture");
        course.setMaxCapacity(35);
        course.setCurrentEnrollment(12);

        assertEquals(99L, course.getId());
        assertEquals("Software Architecture", course.getTitle());
        assertEquals(35, course.getMaxCapacity());
        assertEquals(12, course.getCurrentEnrollment());
    }
}
