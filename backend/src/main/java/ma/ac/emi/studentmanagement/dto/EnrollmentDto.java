package ma.ac.emi.studentmanagement.dto;

import java.time.LocalDateTime;

public record EnrollmentDto(
        Long id,
        Long courseId,
        String courseTitle,
        LocalDateTime enrolledAt
) {}
