package com.researchconnect.controller;

import com.researchconnect.dto.ApplicationDecisionRequest;
import com.researchconnect.dto.DashboardResponse;
import com.researchconnect.entity.ProjectApplication;
import com.researchconnect.entity.ProjectNotification;
import com.researchconnect.security.FirebaseUserPrincipal;
import com.researchconnect.service.DashboardService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal FirebaseUserPrincipal principal) {
        return dashboardService.dashboard(principal.getUsername());
    }

    @PostMapping("/projects/{projectId}/applications")
    public ResponseEntity<ProjectApplication> submitApplication(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @PathVariable String projectId
    ) {
        ProjectApplication application = dashboardService.submitApplication(principal.getUsername(), projectId);
        return ResponseEntity.status(HttpStatus.CREATED).body(application);
    }

    @GetMapping("/projects/{projectId}/applications/mine")
    public ResponseEntity<ProjectApplication> applicationForUser(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @PathVariable String projectId
    ) {
        ProjectApplication application = dashboardService.applicationForUser(
                principal.getUsername(), projectId);
        return application == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(application);
    }

    @PatchMapping("/applications/{applicationId}/decision")
    public ProjectApplication decideApplication(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @PathVariable String applicationId,
            @Valid @RequestBody ApplicationDecisionRequest request
    ) {
        return dashboardService.decideApplication(
                principal.getUsername(), applicationId, request.status());
    }

    @GetMapping("/projects/{projectId}/saved")
    public Map<String, Boolean> savedStatus(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @PathVariable String projectId
    ) {
        return Map.of("saved", dashboardService.isSaved(principal.getUsername(), projectId));
    }

    @PutMapping("/projects/{projectId}/saved")
    public ResponseEntity<Void> saveProject(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @PathVariable String projectId
    ) {
        dashboardService.saveProject(principal.getUsername(), projectId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/projects/{projectId}/saved")
    public ResponseEntity<Void> removeSavedProject(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @PathVariable String projectId
    ) {
        dashboardService.removeSavedProject(principal.getUsername(), projectId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notifications")
    public List<ProjectNotification> notifications(
            @AuthenticationPrincipal FirebaseUserPrincipal principal
    ) {
        return dashboardService.notifications(principal.getUsername());
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ProjectNotification markNotificationRead(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @PathVariable String notificationId
    ) {
        return dashboardService.markNotificationRead(principal.getUsername(), notificationId);
    }
}
