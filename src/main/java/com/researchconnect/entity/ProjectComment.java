package com.researchconnect.entity;

import java.time.Instant;

public record ProjectComment(
        String commentId,
        String projectId,
        String authorId,
        String authorName,
        String authorRole,
        String text,
        Instant postedAt
) {
}
