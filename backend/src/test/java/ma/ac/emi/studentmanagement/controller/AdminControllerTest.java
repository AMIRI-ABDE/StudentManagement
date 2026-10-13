package ma.ac.emi.studentmanagement.controller;

import ma.ac.emi.studentmanagement.dto.StudentDto;
import ma.ac.emi.studentmanagement.entity.Role;
import ma.ac.emi.studentmanagement.entity.Student;
import ma.ac.emi.studentmanagement.repository.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController Unit Tests")
class AdminControllerTest {

    @Mock
    private StudentRepository studentRepo;

    @InjectMocks
    private AdminController adminController;

    @Nested
    @DisplayName("getAllStudents()")
    class GetAllStudentsTests {

        @Test
        @DisplayName("Should return list of StudentDto when students exist")
        void shouldReturnAllStudentsDtoList() {
            // Arrange
            Student student = new Student();
            student.setId(10L);
            student.setName("Charlie");
            student.setEmail("charlie@emi.ac.ma");
            student.setRole(Role.STUDENT);

            when(studentRepo.findAll()).thenReturn(List.of(student));

            // Act
            List<StudentDto> result = adminController.getAllStudents();

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).id());
            assertEquals("Charlie", result.get(0).name());
            assertEquals("charlie@emi.ac.ma", result.get(0).email());
            assertEquals("STUDENT", result.get(0).role());

            verify(studentRepo, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no students exist")
        void shouldReturnEmptyListWhenNoStudents() {
            // Arrange
            when(studentRepo.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<StudentDto> result = adminController.getAllStudents();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(studentRepo, times(1)).findAll();
        }
    }
}
