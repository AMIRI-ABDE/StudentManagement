package ma.ac.emi.studentmanagement.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("handleResponseStatus(ResponseStatusException)")
    class HandleResponseStatusTests {

        @Test
        @DisplayName("Should return structured error response with status code and reason")
        void shouldHandleResponseStatusException() {
            // Arrange
            ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");

            // Act
            ResponseEntity<Map<String, Object>> response = exceptionHandler.handleResponseStatus(exception);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals(404, body.get("status"));
            assertEquals("Student not found", body.get("error"));
            assertNotNull(body.get("timestamp"));
        }
    }

    @Nested
    @DisplayName("handleValidation(MethodArgumentNotValidException)")
    class HandleValidationTests {

        @Test
        @DisplayName("Should return 400 Bad Request with comma-separated field error messages")
        void shouldHandleValidationException() {
            // Arrange
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);

            FieldError fieldError1 = new FieldError("course", "title", "must not be blank");
            FieldError fieldError2 = new FieldError("course", "maxCapacity", "must be greater than 0");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

            // Act
            ResponseEntity<Map<String, Object>> response = exceptionHandler.handleValidation(ex);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals(400, body.get("status"));
            assertNotNull(body.get("timestamp"));

            String errors = (String) body.get("errors");
            assertNotNull(errors);
            assertTrue(errors.contains("title: must not be blank"));
            assertTrue(errors.contains("maxCapacity: must be greater than 0"));
        }
    }
}
