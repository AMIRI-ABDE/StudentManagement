package ma.ac.emi.studentmanagement.service;

import ma.ac.emi.studentmanagement.config.JwtService;
import ma.ac.emi.studentmanagement.dto.AuthResponse;
import ma.ac.emi.studentmanagement.dto.LoginRequest;
import ma.ac.emi.studentmanagement.dto.RegisterRequest;
import ma.ac.emi.studentmanagement.entity.Role;
import ma.ac.emi.studentmanagement.entity.Student;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private StudentRepository studentRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Student existingStudent;

    @BeforeEach
    void setUp() {
        existingStudent = new Student();
        existingStudent.setId(42L);
        existingStudent.setName("John Doe");
        existingStudent.setEmail("john.doe@emi.ac.ma");
        existingStudent.setPassword("$2a$10$hashedPasswordHere");
        existingStudent.setRole(Role.STUDENT);
    }

    // =========================================================================
    // Nested Suite: register(RegisterRequest)
    // =========================================================================
    @Nested
    @DisplayName("register(RegisterRequest request)")
    class RegisterTests {

        @Test
        @DisplayName("Should throw 409 CONFLICT when email is already registered")
        void shouldThrowConflictWhenEmailAlreadyRegistered() {
            // Arrange
            RegisterRequest request = new RegisterRequest("Bob Smith", "bob@emi.ac.ma", "secret123");
            when(studentRepo.existsByEmail(request.email())).thenReturn(true);

            // Act & Assert
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.register(request));

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            assertEquals("Email already registered", exception.getReason());

            verify(studentRepo, times(1)).existsByEmail(request.email());
            verify(studentRepo, never()).save(any());
            verifyNoInteractions(passwordEncoder, jwtService);
        }

        @Test
        @DisplayName("Should register new student successfully, encode password, generate JWT token, and return AuthResponse")
        void shouldRegisterStudentSuccessfully() {
            // Arrange
            RegisterRequest request = new RegisterRequest("Bob Smith", "bob@emi.ac.ma", "plainPassword123");
            String encodedPassword = "$2a$10$encodedBobPassword";
            String generatedToken = "jwt.token.bob.registration";

            when(studentRepo.existsByEmail(request.email())).thenReturn(false);
            when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);

            when(studentRepo.save(any(Student.class))).thenAnswer(invocation -> {
                Student saved = invocation.getArgument(0);
                saved.setId(101L);
                return saved;
            });

            when(jwtService.generateToken(101L, request.email(), "STUDENT")).thenReturn(generatedToken);

            // Act
            AuthResponse response = authService.register(request);

            // Assert
            assertNotNull(response);
            assertEquals(generatedToken, response.token());
            assertEquals(101L, response.id());
            assertEquals(request.name(), response.name());
            assertEquals(request.email(), response.email());
            assertEquals("STUDENT", response.role());

            // State & Entity Verification via ArgumentCaptor
            ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
            verify(studentRepo, times(1)).save(studentCaptor.capture());
            Student capturedStudent = studentCaptor.getValue();

            assertEquals(request.name(), capturedStudent.getName());
            assertEquals(request.email(), capturedStudent.getEmail());
            assertEquals(encodedPassword, capturedStudent.getPassword());
            assertEquals(Role.STUDENT, capturedStudent.getRole());

            verify(studentRepo, times(1)).existsByEmail(request.email());
            verify(passwordEncoder, times(1)).encode(request.password());
            verify(jwtService, times(1)).generateToken(101L, request.email(), "STUDENT");
        }
    }

    // =========================================================================
    // Nested Suite: login(LoginRequest)
    // =========================================================================
    @Nested
    @DisplayName("login(LoginRequest request)")
    class LoginTests {

        @Test
        @DisplayName("Should throw 401 UNAUTHORIZED when student email does not exist")
        void shouldThrowUnauthorizedWhenStudentEmailNotFound() {
            // Arrange
            LoginRequest request = new LoginRequest("unknown@emi.ac.ma", "somePassword");
            when(studentRepo.findByEmail(request.email())).thenReturn(Optional.empty());

            // Act & Assert
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.login(request));

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
            assertEquals("Invalid credentials", exception.getReason());

            verify(studentRepo, times(1)).findByEmail(request.email());
            verifyNoInteractions(passwordEncoder, jwtService);
        }

        @Test
        @DisplayName("Should throw 401 UNAUTHORIZED when password does not match encoded password")
        void shouldThrowUnauthorizedWhenPasswordDoesNotMatch() {
            // Arrange
            LoginRequest request = new LoginRequest("john.doe@emi.ac.ma", "wrongPassword");
            when(studentRepo.findByEmail(request.email())).thenReturn(Optional.of(existingStudent));
            when(passwordEncoder.matches(request.password(), existingStudent.getPassword())).thenReturn(false);

            // Act & Assert
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.login(request));

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
            assertEquals("Invalid credentials", exception.getReason());

            verify(studentRepo, times(1)).findByEmail(request.email());
            verify(passwordEncoder, times(1)).matches(request.password(), existingStudent.getPassword());
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Should login successfully, generate JWT token, and return AuthResponse when credentials are valid")
        void shouldLoginSuccessfullyWhenCredentialsAreValid() {
            // Arrange
            LoginRequest request = new LoginRequest("john.doe@emi.ac.ma", "correctPassword");
            String generatedToken = "jwt.token.john.login";

            when(studentRepo.findByEmail(request.email())).thenReturn(Optional.of(existingStudent));
            when(passwordEncoder.matches(request.password(), existingStudent.getPassword())).thenReturn(true);
            when(jwtService.generateToken(existingStudent.getId(), existingStudent.getEmail(), "STUDENT"))
                    .thenReturn(generatedToken);

            // Act
            AuthResponse response = authService.login(request);

            // Assert
            assertNotNull(response);
            assertEquals(generatedToken, response.token());
            assertEquals(existingStudent.getId(), response.id());
            assertEquals(existingStudent.getName(), response.name());
            assertEquals(existingStudent.getEmail(), response.email());
            assertEquals("STUDENT", response.role());

            verify(studentRepo, times(1)).findByEmail(request.email());
            verify(passwordEncoder, times(1)).matches(request.password(), existingStudent.getPassword());
            verify(jwtService, times(1)).generateToken(existingStudent.getId(), existingStudent.getEmail(), "STUDENT");
        }
    }
}
