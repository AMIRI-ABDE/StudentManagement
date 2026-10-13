package ma.ac.emi.studentmanagement.config;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);
    }

    @Nested
    @DisplayName("generateToken & parseToken")
    class TokenGenerationAndParsingTests {

        @Test
        @DisplayName("Should generate a valid token and parse subject and claims properly")
        void shouldGenerateAndParseToken() {
            // Arrange
            Long userId = 77L;
            String email = "student@emi.ac.ma";
            String role = "STUDENT";

            // Act
            String token = jwtService.generateToken(userId, email, role);

            // Assert
            assertNotNull(token);
            assertFalse(token.isBlank());

            Claims claims = jwtService.parseToken(token);
            assertEquals(email, claims.getSubject());
            assertEquals(userId.intValue(), claims.get("userId", Integer.class));
            assertEquals(role, claims.get("role", String.class));
            assertNotNull(claims.getIssuedAt());
            assertNotNull(claims.getExpiration());
        }

        @Test
        @DisplayName("Should extract email directly using getEmailFromToken")
        void shouldExtractEmailFromToken() {
            // Arrange
            String email = "admin@emi.ac.ma";
            String token = jwtService.generateToken(1L, email, "ADMIN");

            // Act
            String extractedEmail = jwtService.getEmailFromToken(token);

            // Assert
            assertEquals(email, extractedEmail);
        }
    }

    @Nested
    @DisplayName("isTokenValid(String token)")
    class TokenValidationTests {

        @Test
        @DisplayName("Should return true for a freshly generated, untampered token")
        void shouldReturnTrueForValidToken() {
            // Arrange
            String token = jwtService.generateToken(1L, "user@emi.ac.ma", "STUDENT");

            // Act & Assert
            assertTrue(jwtService.isTokenValid(token));
        }

        @Test
        @DisplayName("Should return false when token string is malformed or invalid")
        void shouldReturnFalseForMalformedToken() {
            // Act & Assert
            assertFalse(jwtService.isTokenValid("malformed.jwt.token.string"));
            assertFalse(jwtService.isTokenValid(null));
            assertFalse(jwtService.isTokenValid(""));
        }
    }
}
