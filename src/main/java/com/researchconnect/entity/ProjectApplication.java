package com.researchconnect.entity;

import com.researchconnect.enums.ApplicationStatus;
import java.time.Instant;

public record ProjectApplication(
        String applicationId,
        String projectId,
        String applicantId,
        String applicantName,
        String applicantEmail,
        String applicantRole,
        String applicantDepartment,
        ApplicationStatus status,
        Instant appliedAt,
        Instant updatedAt
) {
}
