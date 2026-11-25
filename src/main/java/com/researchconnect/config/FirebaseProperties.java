package com.researchconnect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "researchconnect.firebase")
public record FirebaseProperties(
        String webApiKey,
        String webAuthDomain,
        String webProjectId,
        String webAppId,
        String serviceAccountPath
) {
}
