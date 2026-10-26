package ma.ac.emi.studentmanagement.controller;

import jakarta.validation.Valid;
import ma.ac.emi.studentmanagement.dto.AuthResponse;
import ma.ac.emi.studentmanagement.dto.LoginRequest;
import ma.ac.emi.studentmanagement.dto.RegisterRequest;
import ma.ac.emi.studentmanagement.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/health")
    public java.util.Map<String, String> health() {
        return java.util.Map.of("status", "UP");
    }
}
