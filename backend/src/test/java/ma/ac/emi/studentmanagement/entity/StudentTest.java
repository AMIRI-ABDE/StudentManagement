package ma.ac.emi.studentmanagement.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Student Entity Unit Tests")
class StudentTest {

    @Test
    @DisplayName("Should initialize role to STUDENT by default")
    void shouldInitializeRoleToStudentByDefault() {
        Student student = new Student();
        assertEquals(Role.STUDENT, student.getRole());
    }

    @Test
    @DisplayName("Should set and get different role")
    void shouldSetAndGetRole() {
        Student student = new Student();
        student.setRole(Role.ADMIN);
        assertEquals(Role.ADMIN, student.getRole());
    }

    @Test
    @DisplayName("Should verify getters and setters for all student fields")
    void shouldVerifyGettersAndSetters() {
        Student student = new Student();
        student.setId(10L);
        student.setName("Alice Martin");
        student.setEmail("alice.martin@emi.ac.ma");
        student.setPassword("securePassword123");
        student.setRole(Role.STUDENT);

        assertEquals(10L, student.getId());
        assertEquals("Alice Martin", student.getName());
        assertEquals("alice.martin@emi.ac.ma", student.getEmail());
        assertEquals("securePassword123", student.getPassword());
        assertEquals(Role.STUDENT, student.getRole());
    }
}
