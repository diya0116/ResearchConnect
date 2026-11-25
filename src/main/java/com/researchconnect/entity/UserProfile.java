package com.researchconnect.entity;

import com.researchconnect.enums.UserRole;
import java.time.Instant;

public record UserProfile(
        String uid,
        String email,
        String fullName,
        String profileImageUrl,
        UserRole role,
        String department,
        String description,
        String cohort,
        Instant registrationDate,
        Instant updatedAt
) {
}
