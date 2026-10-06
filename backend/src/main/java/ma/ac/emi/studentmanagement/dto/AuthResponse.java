package ma.ac.emi.studentmanagement.dto;

public record AuthResponse(
        String token,
        Long id,
        String name,
        String email,
        String role
) {}
