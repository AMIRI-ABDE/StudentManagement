package ma.ac.emi.studentmanagement.dto;

public record StudentDto(
        Long id,
        String name,
        String email,
        String role
) {}
