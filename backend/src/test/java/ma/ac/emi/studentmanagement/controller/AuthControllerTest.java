package ma.ac.emi.studentmanagement.controller;

import ma.ac.emi.studentmanagement.dto.AuthResponse;
import ma.ac.emi.studentmanagement.dto.LoginRequest;
import ma.ac.emi.studentmanagement.dto.RegisterRequest;
import ma.ac.emi.studentmanagement.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Nested
    @DisplayName("register(RegisterRequest)")
    class RegisterTests {

        @Test
        @DisplayName("Should delegate registration to AuthService and return AuthResponse")
        void shouldRegisterSuccessfully() {
            // Arrange
            RegisterRequest request = new RegisterRequest("Sam", "sam@emi.ac.ma", "password123");
            AuthResponse expectedResponse = new AuthResponse("token-123", 1L, "Sam", "sam@emi.ac.ma", "STUDENT");
            when(authService.register(request)).thenReturn(expectedResponse);

            // Act
            AuthResponse actualResponse = authController.register(request);

            // Assert
            assertNotNull(actualResponse);
            assertEquals(expectedResponse, actualResponse);
            verify(authService, times(1)).register(request);
        }
    }

    @Nested
    @DisplayName("login(LoginRequest)")
    class LoginTests {

        @Test
        @DisplayName("Should delegate login to AuthService and return AuthResponse")
        void shouldLoginSuccessfully() {
            // Arrange
            LoginRequest request = new LoginRequest("sam@emi.ac.ma", "password123");
            AuthResponse expectedResponse = new AuthResponse("token-456", 1L, "Sam", "sam@emi.ac.ma", "STUDENT");
            when(authService.login(request)).thenReturn(expectedResponse);

            // Act
            AuthResponse actualResponse = authController.login(request);

            // Assert
            assertNotNull(actualResponse);
            assertEquals(expectedResponse, actualResponse);
            verify(authService, times(1)).login(request);
        }
    }
}
