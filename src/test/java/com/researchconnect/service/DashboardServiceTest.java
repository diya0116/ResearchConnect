package com.researchconnect.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.researchconnect.entity.Project;
import com.researchconnect.entity.ProjectApplication;
import com.researchconnect.entity.ProjectMembership;
import com.researchconnect.entity.SavedProject;
import com.researchconnect.entity.UserProfile;
import com.researchconnect.enums.ApplicationStatus;
import com.researchconnect.enums.ProjectStatus;
import com.researchconnect.enums.UserRole;
import com.researchconnect.repository.FirestoreCollaborationRepository;
import com.researchconnect.repository.FirestoreNotificationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private ProjectService projectService;

    @Mock
    private FirestoreCollaborationRepository collaborationRepository;

    @Mock
    private FirestoreNotificationRepository notificationRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void dashboardAggregatesOnlyTheAuthenticatedUsersProjectRelationships() throws Exception {
        UserProfile profile = profile("user-a");
        Project owned = project("project-owned", "user-a");
        Project joined = project("project-joined", "owner-b");
        Project saved = project("project-saved", "owner-c");
        ProjectApplication outgoing = application("outgoing", joined.projectId(), "user-a", ApplicationStatus.PENDING);
        ProjectApplication incoming = application("incoming", owned.projectId(), "applicant-b", ApplicationStatus.PENDING);

        when(userService.findByUid("user-a")).thenReturn(profile);
        when(projectService.findOwned("user-a")).thenReturn(List.of(owned));
        when(collaborationRepository.findApplicationsByApplicant("user-a")).thenReturn(List.of(outgoing));
        when(collaborationRepository.findMembershipsByUser("user-a"))
                .thenReturn(List.of(membership("user-a", joined.projectId())));
        when(collaborationRepository.findSavedProjects("user-a"))
                .thenReturn(List.of(new SavedProject("saved", saved.projectId(), "user-a", Instant.EPOCH)));
        when(collaborationRepository.findApplicationsForProjects(List.of(owned.projectId())))
                .thenReturn(List.of(incoming));
        when(projectService.findByIds(anyCollection()))
                .thenReturn(Map.of(
                        owned.projectId(), owned,
                        joined.projectId(), joined,
                        saved.projectId(), saved
                ));
        when(collaborationRepository.findMembershipsForProjects(anyCollection()))
                .thenReturn(List.of(
                        membership("member-b", owned.projectId()),
                        membership("user-a", joined.projectId()),
                        membership("member-c", joined.projectId())
                ));

        var result = dashboardService.dashboard("user-a");

        assertEquals("user-a@thapar.edu", result.profile().email());
        assertEquals(1, result.totalProjects());
        assertEquals(1, result.activeMemberProjects());
        assertEquals(1, result.pendingIncomingRequests());
        assertEquals(1, result.pendingOutgoingRequests());
        assertEquals(1, result.myProjects().get(0).currentMembers());
        assertEquals(2, result.teamMemberships().get(0).members().size());
        assertEquals(saved.projectId(), result.savedProjects().get(0).project().projectId());
        verify(userService).findByUid("user-a");
        verify(projectService).findOwned("user-a");
        verify(collaborationRepository).findApplicationsByApplicant("user-a");
    }

    @Test
    void nonOwnerCannotDecideAnotherOwnersApplication() throws Exception {
        Project ownedBySomeoneElse = project("project-owned", "owner-a");
        ProjectApplication pending = application(
                "application", ownedBySomeoneElse.projectId(), "applicant", ApplicationStatus.PENDING);
        when(collaborationRepository.findApplication("application")).thenReturn(pending);
        when(projectService.findById(ownedBySomeoneElse.projectId())).thenReturn(ownedBySomeoneElse);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> dashboardService.decideApplication("owner-b", "application", ApplicationStatus.ACCEPTED));

        assertEquals(403, error.getStatusCode().value());
        verify(collaborationRepository, never()).decideApplication(
                anyString(), anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void userWithoutProfileCannotLoadDashboard() throws Exception {
        when(userService.findByUid("new-user")).thenReturn(null);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class, () -> dashboardService.dashboard("new-user"));

        assertEquals(409, error.getStatusCode().value());
        verify(collaborationRepository, never()).findApplicationsByApplicant(anyString());
    }

    @Test
    void savedProjectOperationsRemainScopedToTheAuthenticatedUser() throws Exception {
        when(projectService.findById("project-id")).thenReturn(project("project-id", "owner"));
        when(collaborationRepository.isSaved("user-a", "project-id")).thenReturn(true);

        assertTrue(dashboardService.isSaved("user-a", "project-id"));
        dashboardService.saveProject("user-a", "project-id");
        dashboardService.removeSavedProject("user-a", "project-id");

        verify(collaborationRepository).isSaved("user-a", "project-id");
        verify(collaborationRepository).saveProject("user-a", "project-id");
        verify(collaborationRepository).removeSavedProject("user-a", "project-id");
    }

    private UserProfile profile(String uid) {
        return new UserProfile(
                uid, uid + "@thapar.edu", "User A", "", UserRole.STUDENT,
                "Computer Science", "", "", Instant.EPOCH, Instant.EPOCH);
    }

    private Project project(String id, String ownerId) {
        return new Project(
                id, "Title " + id, "Short description", "Description", "AI", "Computer Science",
                List.of("Java"), 4, 2, "6 months", 5, LocalDate.parse("2026-12-01"),
                "", "", ownerId, "Owner " + ownerId, "STUDENT",
                ProjectStatus.PUBLISHED, Instant.EPOCH, Instant.EPOCH);
    }

    private ProjectApplication application(
            String id,
            String projectId,
            String applicantId,
            ApplicationStatus status
    ) {
        return new ProjectApplication(
                id, projectId, applicantId, "Applicant", "applicant@thapar.edu",
                "STUDENT", "Computer Science", status, Instant.EPOCH, Instant.EPOCH);
    }

    private ProjectMembership membership(String userId, String projectId) {
        return new ProjectMembership(
                userId + "-" + projectId, projectId, userId, userId, userId + "@thapar.edu",
                "STUDENT", "Computer Science", Instant.EPOCH);
    }
}
