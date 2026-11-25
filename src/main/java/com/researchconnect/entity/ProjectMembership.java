package com.researchconnect.entity;

import java.time.Instant;

public record ProjectMembership(
        String memberId,
        String projectId,
        String userId,
        String fullName,
        String email,
        String role,
        String department,
        Instant joinedAt
) {
}
