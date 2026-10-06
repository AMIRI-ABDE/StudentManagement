package ma.ac.emi.studentmanagement.service;

import ma.ac.emi.studentmanagement.config.JwtService;
import ma.ac.emi.studentmanagement.dto.AuthResponse;
import ma.ac.emi.studentmanagement.dto.LoginRequest;
import ma.ac.emi.studentmanagement.dto.RegisterRequest;
import ma.ac.emi.studentmanagement.entity.Role;
import ma.ac.emi.studentmanagement.entity.Student;
import ma.ac.emi.studentmanagement.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final StudentRepository studentRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(StudentRepository studentRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.studentRepo = studentRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (studentRepo.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        Student student = new Student();
        student.setName(request.name());
        student.setEmail(request.email());
        student.setPassword(passwordEncoder.encode(request.password()));
        student.setRole(Role.STUDENT);

        student = studentRepo.save(student);

        String token = jwtService.generateToken(student.getId(), student.getEmail(), student.getRole().name());
        return new AuthResponse(token, student.getId(), student.getName(), student.getEmail(), student.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        Student student = studentRepo.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), student.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtService.generateToken(student.getId(), student.getEmail(), student.getRole().name());
        return new AuthResponse(token, student.getId(), student.getName(), student.getEmail(), student.getRole().name());
    }
}
