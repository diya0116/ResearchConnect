package com.researchconnect.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record ProjectRequest(
        @NotBlank String title,
        @NotBlank String shortDescription,
        @NotBlank String description,
        @NotBlank String category,
        @NotBlank String department,
        @NotEmpty List<@NotBlank String> requiredSkills,
        @Min(1) int teamSize,
        @Min(0) int availablePositions,
        @NotBlank String duration,
        @Min(1) int weeklyCommitment,
        @NotNull LocalDate expectedStartDate,
        String objectives,
        String requirements
) {
}
