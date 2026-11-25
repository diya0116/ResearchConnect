package com.researchconnect.dto;

import com.researchconnect.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ApplicationDecisionRequest(@NotNull ApplicationStatus status) {
}
