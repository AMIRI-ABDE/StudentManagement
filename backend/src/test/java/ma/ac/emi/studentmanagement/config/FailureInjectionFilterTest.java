package ma.ac.emi.studentmanagement.config;

import io.getunleash.Unleash;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@DisplayName("FailureInjectionFilter Unit Tests")
class FailureInjectionFilterTest {

    @Test
    @DisplayName("Should bypass failure injection when app version is v1")
    void shouldBypassForV1() throws ServletException, IOException {
        Unleash unleash = mock(Unleash.class);
        when(unleash.isEnabled("simulate-failure")).thenReturn(true);
        FailureInjectionFilter filter = new FailureInjectionFilter(unleash, "v1");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(unleash, never()).isEnabled(anyString());
    }

    @Test
    @DisplayName("Should bypass failure injection for non-api endpoints in v2")
    void shouldBypassForNonApiEndpoints() throws ServletException, IOException {
        Unleash unleash = mock(Unleash.class);
        FailureInjectionFilter filter = new FailureInjectionFilter(unleash, "v2");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/actuator/health");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(unleash, never()).isEnabled(anyString());
    }

    @Test
    @DisplayName("Should return 500 on v2 when simulate-failure is enabled")
    void shouldReturn500WhenSimulateFailureEnabled() throws ServletException, IOException {
        Unleash unleash = mock(Unleash.class);
        when(unleash.isEnabled("simulate-failure")).thenReturn(true);
        FailureInjectionFilter filter = new FailureInjectionFilter(unleash, "v2");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/courses");
        when(request.getMethod()).thenReturn("GET");

        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(500);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("Simulated failure (canary test)"));
    }

    @Test
    @DisplayName("Should continue chain when no flags are active on v2")
    void shouldContinueChainWhenNoFlags() throws ServletException, IOException {
        Unleash unleash = mock(Unleash.class);
        when(unleash.isEnabled(anyString())).thenReturn(false);
        FailureInjectionFilter filter = new FailureInjectionFilter(unleash, "v2");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/courses");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
