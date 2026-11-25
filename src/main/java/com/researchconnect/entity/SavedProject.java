package com.researchconnect.entity;

import java.time.Instant;

public record SavedProject(
        String savedProjectId,
        String projectId,
        String userId,
        Instant savedAt
) {
}
