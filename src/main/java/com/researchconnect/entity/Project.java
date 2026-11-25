package com.researchconnect.entity;

import com.researchconnect.enums.ProjectStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record Project(
        String projectId,
        String title,
        String shortDescription,
        String description,
        String category,
        String department,
        List<String> requiredSkills,
        int teamSize,
        int availablePositions,
        String duration,
        int weeklyCommitment,
        LocalDate expectedStartDate,
        String objectives,
        String requirements,
        String ownerId,
        String ownerName,
        String ownerRole,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
