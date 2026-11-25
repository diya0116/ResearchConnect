package com.researchconnect.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.firebase.auth.FirebaseToken;
import com.researchconnect.dto.ApplicationDecisionRequest;
import com.researchconnect.dto.DashboardResponse;
import com.researchconnect.entity.ProjectApplication;
import com.researchconnect.entity.ProjectNotification;
import com.researchconnect.enums.ApplicationStatus;
import com.researchconnect.security.FirebaseUserPrincipal;
import com.researchconnect.service.DashboardService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    private DashboardController controller;
    private FirebaseUserPrincipal principal;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new DashboardController(dashboardService);
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("authenticated-user");
        principal = new FirebaseUserPrincipal(token);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dashboardUsesAuthenticatedUid() {
        DashboardResponse expected = new DashboardResponse(
                new DashboardResponse.ProfileSummary("Name", "user@thapar.edu", "STUDENT", "CS"),
                0, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of());
        when(dashboardService.dashboard("authenticated-user")).thenReturn(expected);

        assertEquals(expected, controller.dashboard(principal));
    }

    @Test
    void collaborationEndpointsAlwaysUseAuthenticatedUid() {
        ProjectApplication application = new ProjectApplication(
                "application", "project", "authenticated-user", "Name", "user@thapar.edu",
                "STUDENT", "CS", ApplicationStatus.PENDING, Instant.EPOCH, Instant.EPOCH);
        ProjectApplication acceptedApplication = new ProjectApplication(
                "application", "project", "authenticated-user", "Name", "user@thapar.edu",
                "STUDENT", "CS", ApplicationStatus.ACCEPTED, Instant.EPOCH, Instant.EPOCH);
        when(dashboardService.submitApplication("authenticated-user", "project")).thenReturn(application);
        when(dashboardService.applicationForUser("authenticated-user", "project")).thenReturn(application);
        when(dashboardService.decideApplication("authenticated-user", "application", ApplicationStatus.ACCEPTED))
                .thenReturn(acceptedApplication);
        when(dashboardService.isSaved("authenticated-user", "project")).thenReturn(true);

        assertEquals(HttpStatus.CREATED, controller.submitApplication(principal, "project").getStatusCode());
        ResponseEntity<ProjectApplication> ownApplication = controller.applicationForUser(principal, "project");
        assertEquals(HttpStatus.OK, ownApplication.getStatusCode());
        assertEquals(application, ownApplication.getBody());
        assertEquals(acceptedApplication, controller.decideApplication(
                principal, "application", new ApplicationDecisionRequest(ApplicationStatus.ACCEPTED)));
        assertTrue(controller.savedStatus(principal, "project").get("saved"));
        assertEquals(HttpStatus.NO_CONTENT, controller.saveProject(principal, "project").getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.removeSavedProject(principal, "project").getStatusCode());

        verify(dashboardService).submitApplication("authenticated-user", "project");
        verify(dashboardService).applicationForUser("authenticated-user", "project");
        verify(dashboardService).decideApplication(
                "authenticated-user", "application", ApplicationStatus.ACCEPTED);
        verify(dashboardService).isSaved("authenticated-user", "project");
        verify(dashboardService).saveProject("authenticated-user", "project");
        verify(dashboardService).removeSavedProject("authenticated-user", "project");
    }

    @Test
    void dashboardApiRoutesReturnExpectedHttpResponses() throws Exception {
        DashboardResponse expected = new DashboardResponse(
                new DashboardResponse.ProfileSummary("Name", "user@thapar.edu", "STUDENT", "CS"),
                0, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of());
        ProjectApplication application = new ProjectApplication(
                "application", "project", "authenticated-user", "Name", "user@thapar.edu",
                "STUDENT", "CS", ApplicationStatus.PENDING, Instant.EPOCH, Instant.EPOCH);
        ProjectApplication acceptedApplication = new ProjectApplication(
                "application", "project", "authenticated-user", "Name", "user@thapar.edu",
                "STUDENT", "CS", ApplicationStatus.ACCEPTED, Instant.EPOCH, Instant.EPOCH);
        when(dashboardService.dashboard("authenticated-user")).thenReturn(expected);
        when(dashboardService.submitApplication("authenticated-user", "project")).thenReturn(application);
        when(dashboardService.applicationForUser("authenticated-user", "project")).thenReturn(null);
        when(dashboardService.decideApplication("authenticated-user", "application", ApplicationStatus.ACCEPTED))
                .thenReturn(acceptedApplication);
        when(dashboardService.isSaved("authenticated-user", "project")).thenReturn(true);
        when(dashboardService.notifications("authenticated-user")).thenReturn(List.of(
                new ProjectNotification(
                        "notification-id", "Request accepted", "project", "Project", Instant.EPOCH, false)));
        when(dashboardService.markNotificationRead("authenticated-user", "notification-id")).thenReturn(
                new ProjectNotification(
                        "notification-id", "Request accepted", "project", "Project", Instant.EPOCH, true));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.name").value("Name"));
        mockMvc.perform(post("/api/projects/project/applications"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicantId").value("authenticated-user"));
        mockMvc.perform(get("/api/projects/project/applications/mine"))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/applications/application/decision")
                        .contentType("application/json")
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mockMvc.perform(get("/api/projects/project/saved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));
        mockMvc.perform(put("/api/projects/project/saved"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/projects/project/saved"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationId").value("notification-id"));
        mockMvc.perform(patch("/api/notifications/notification-id/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
        verify(dashboardService).notifications("authenticated-user");
        verify(dashboardService).markNotificationRead("authenticated-user", "notification-id");
    }
}
