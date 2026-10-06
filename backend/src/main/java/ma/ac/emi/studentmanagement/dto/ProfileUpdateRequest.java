package ma.ac.emi.studentmanagement.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(
        @NotBlank String name
) {}
