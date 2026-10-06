package ma.ac.emi.studentmanagement.dto;

public record CourseDto(
        Long id,
        String title,
        int maxCapacity,
        int currentEnrollment,
        boolean full
) {}
