package ma.ac.emi.studentmanagement.config;

import io.getunleash.Unleash;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class FailureInjectionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FailureInjectionFilter.class);

    private final Unleash unleash;
    private final String appVersion;

    public FailureInjectionFilter(Unleash unleash,
                                  @Value("${app.version}") String appVersion) {
        this.unleash = unleash;
        this.appVersion = appVersion;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Only inject failures in V2 (canary), never in V1
        if (!"v2".equals(appVersion)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Only apply to API endpoints
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Feature flag: simulate-failure (HTTP 500)
        if (unleash.isEnabled("simulate-failure")) {
            log.warn("FAILURE INJECTION: Returning 500 for {} {}", request.getMethod(), path);
            response.setStatus(500);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Simulated failure (canary test)\",\"version\":\"v2\"}");
            return;
        }

        // Feature flag: simulate-latency (add 3 seconds delay)
        if (unleash.isEnabled("simulate-latency")) {
            log.warn("LATENCY INJECTION: Adding 3s delay for {} {}", request.getMethod(), path);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        filterChain.doFilter(request, response);
    }
}
