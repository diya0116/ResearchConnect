package com.researchconnect.entity;

import java.time.Instant;

public record ProjectNotification(
        String notificationId,
        String message,
        String projectId,
        String projectTitle,
        Instant createdAt,
        boolean read
) {
}
