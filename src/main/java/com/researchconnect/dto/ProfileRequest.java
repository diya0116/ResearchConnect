package com.researchconnect.dto;

import com.researchconnect.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfileRequest(
        @NotNull UserRole role,
        @NotBlank String department,
        String description,
        String cohort
) {
}
