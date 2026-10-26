package ma.ac.emi.studentmanagement.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.mockito.Mockito.*;

@DisplayName("VersionHeaderFilter Unit Tests")
class VersionHeaderFilterTest {

    @Test
    @DisplayName("Should add X-Backend-Version header to response")
    void shouldAddVersionHeader() throws ServletException, IOException {
        VersionHeaderFilter filter = new VersionHeaderFilter();
        ReflectionTestUtils.setField(filter, "appVersion", "v1");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-Backend-Version", "v1");
        verify(filterChain).doFilter(request, response);
    }
}
