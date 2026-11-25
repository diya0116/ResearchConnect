package com.researchconnect.service;

import com.researchconnect.dto.DashboardResponse;
import com.researchconnect.entity.Project;
import com.researchconnect.entity.ProjectApplication;
import com.researchconnect.entity.ProjectMembership;
import com.researchconnect.entity.ProjectNotification;
import com.researchconnect.entity.SavedProject;
import com.researchconnect.entity.UserProfile;
import com.researchconnect.enums.ApplicationStatus;
import com.researchconnect.repository.FirestoreCollaborationRepository;
import com.researchconnect.repository.FirestoreNotificationRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DashboardService {

    private final UserService userService;
    private final ProjectService projectService;
    private final FirestoreCollaborationRepository collaborationRepository;
    private final FirestoreNotificationRepository notificationRepository;

    public DashboardService(
            UserService userService,
            ProjectService projectService,
            FirestoreCollaborationRepository collaborationRepository,
            FirestoreNotificationRepository notificationRepository
    ) {
        this.userService = userService;
        this.projectService = projectService;
        this.collaborationRepository = collaborationRepository;
        this.notificationRepository = notificationRepository;
    }

    public DashboardResponse dashboard(String userId) {
        UserProfile profile = userService.findByUid(userId);
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Complete your profile to view the dashboard");
        }

        List<Project> ownedProjects = projectService.findOwned(userId);
        List<ProjectApplication> outgoingApplications = execute(
                () -> collaborationRepository.findApplicationsByApplicant(userId),
                "Unable to read your applications");
        List<ProjectMembership> userMemberships = execute(
                () -> collaborationRepository.findMembershipsByUser(userId),
                "Unable to read your team memberships");
        List<SavedProject> savedProjects = execute(
                () -> collaborationRepository.findSavedProjects(userId),
                "Unable to read your saved projects");
        List<String> ownedProjectIds = ownedProjects.stream().map(Project::projectId).toList();
        List<ProjectApplication> incomingApplications = execute(
                () -> collaborationRepository.findApplicationsForProjects(ownedProjectIds),
                "Unable to read incoming applications");

        LinkedHashSet<String> relatedProjectIds = new LinkedHashSet<>(ownedProjectIds);
        outgoingApplications.forEach(application -> relatedProjectIds.add(application.projectId()));
        userMemberships.forEach(membership -> relatedProjectIds.add(membership.projectId()));
        savedProjects.forEach(saved -> relatedProjectIds.add(saved.projectId()));
        incomingApplications.forEach(application -> relatedProjectIds.add(application.projectId()));
        relatedProjectIds.removeAll(ownedProjectIds);
        Map<String, Project> projectsById = new HashMap<>();
        ownedProjects.forEach(project -> projectsById.put(project.projectId(), project));
        projectsById.putAll(projectService.findByIds(relatedProjectIds));

        List<ProjectMembership> acceptedTeamMemberships = userMemberships.stream()
                .filter(membership -> projectsById.containsKey(membership.projectId()))
                .filter(membership -> !userId.equals(projectsById.get(membership.projectId()).ownerId()))
                .toList();
        LinkedHashSet<String> teamProjectIds = new LinkedHashSet<>();
        acceptedTeamMemberships.forEach(membership -> teamProjectIds.add(membership.projectId()));

        LinkedHashSet<String> projectsWithTeams = new LinkedHashSet<>(ownedProjectIds);
        projectsWithTeams.addAll(teamProjectIds);
        savedProjects.forEach(saved -> projectsWithTeams.add(saved.projectId()));
        List<ProjectMembership> projectMemberships = execute(
                () -> collaborationRepository.findMembershipsForProjects(projectsWithTeams),
                "Unable to read project team details");
        Map<String, List<ProjectMembership>> membersByProject = new HashMap<>();
        for (ProjectMembership membership : projectMemberships) {
            membersByProject.computeIfAbsent(membership.projectId(), ignored -> new ArrayList<>()).add(membership);
        }

        List<DashboardResponse.ProjectItem> myProjects = ownedProjects.stream()
                .map(project -> projectItem(project, membersByProject))
                .toList();
        List<DashboardResponse.ApplicationItem> myApplications = outgoingApplications.stream()
                .filter(application -> projectsById.containsKey(application.projectId()))
                .sorted(Comparator.comparing(ProjectApplication::appliedAt).reversed())
                .map(application -> {
                    Project project = projectsById.get(application.projectId());
                    return new DashboardResponse.ApplicationItem(
                            application.applicationId(),
                            project.projectId(),
                            project.title(),
                            project.ownerName(),
                            application.appliedAt(),
                            application.status()
                    );
                })
                .toList();
        List<DashboardResponse.IncomingRequestItem> incomingRequests = incomingApplications.stream()
                .filter(application -> projectsById.containsKey(application.projectId()))
                .sorted(Comparator.comparing(ProjectApplication::appliedAt).reversed())
                .map(application -> {
                    Project project = projectsById.get(application.projectId());
                    return new DashboardResponse.IncomingRequestItem(
                            application.applicationId(),
                            project.projectId(),
                            project.title(),
                            application.applicantId(),
                            application.applicantName(),
                            application.applicantEmail(),
                            application.applicantRole(),
                            application.applicantDepartment(),
                            application.appliedAt(),
                            application.status()
                    );
                })
                .toList();
        List<DashboardResponse.TeamProjectItem> teamMemberships = acceptedTeamMemberships.stream()
                .map(ProjectMembership::projectId)
                .distinct()
                .filter(projectsById::containsKey)
                .map(projectId -> {
                    List<ProjectMembership> members = membersByProject.getOrDefault(projectId, List.of());
                    return new DashboardResponse.TeamProjectItem(
                            projectsById.get(projectId),
                            members.size(),
                            members.stream().map(this::teamMember).toList()
                    );
                })
                .toList();
        List<DashboardResponse.ProjectItem> saved = savedProjects.stream()
                .filter(project -> projectsById.containsKey(project.projectId()))
                .map(project -> projectItem(projectsById.get(project.projectId()), membersByProject))
                .toList();

        return new DashboardResponse(
                new DashboardResponse.ProfileSummary(
                        profile.fullName(), profile.email(), profile.role().name(), profile.department()),
                myProjects.size(),
                (int) teamMemberships.stream()
                        .filter(team -> team.project().status() == com.researchconnect.enums.ProjectStatus.PUBLISHED)
                        .count(),
                (int) incomingRequests.stream()
                        .filter(request -> request.status() == ApplicationStatus.PENDING)
                        .count(),
                (int) myApplications.stream()
                        .filter(application -> application.status() == ApplicationStatus.PENDING)
                        .count(),
                myProjects,
                myApplications,
                incomingRequests,
                teamMemberships,
                saved
        );
    }

    public ProjectApplication submitApplication(String userId, String projectId) {
        UserProfile applicant = userService.findByUid(userId);
        if (applicant == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Complete your profile before applying");
        }
        Project project = projectService.findById(projectId);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        if (userId.equals(project.ownerId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project owners cannot apply to their own project");
        }
        return execute(
                () -> collaborationRepository.submitApplication(projectId, applicant),
                "Unable to submit your application");
    }

    public ProjectApplication applicationForUser(String userId, String projectId) {
        return execute(
                        () -> collaborationRepository.findApplicationsByApplicant(userId),
                        "Unable to read your application")
                .stream()
                .filter(application -> projectId.equals(application.projectId()))
                .findFirst()
                .orElse(null);
    }

    public ProjectApplication decideApplication(
            String ownerId,
            String applicationId,
            ApplicationStatus decision
    ) {
        if (decision == ApplicationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be ACCEPTED or REJECTED");
        }
        ProjectApplication application = execute(
                () -> collaborationRepository.findApplication(applicationId),
                "Unable to find the application");
        if (application == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found");
        }
        Project project = projectService.findById(application.projectId());
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        if (!ownerId.equals(project.ownerId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the project owner can decide applications");
        }
        if (application.status() != ApplicationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This application has already been decided");
        }
        return execute(
                () -> collaborationRepository.decideApplication(applicationId, ownerId, decision),
                "Unable to update the application");
    }

    public boolean isSaved(String userId, String projectId) {
        requireProject(projectId);
        return execute(
                () -> collaborationRepository.isSaved(userId, projectId),
                "Unable to read saved project status");
    }

    public void saveProject(String userId, String projectId) {
        requireProject(projectId);
        execute(
                () -> {
                    collaborationRepository.saveProject(userId, projectId);
                    return null;
                },
                "Unable to save the project");
    }

    public void removeSavedProject(String userId, String projectId) {
        execute(
                () -> {
                    collaborationRepository.removeSavedProject(userId, projectId);
                    return null;
                },
                "Unable to remove the saved project");
    }

    public List<ProjectNotification> notifications(String userId) {
        return execute(
                () -> notificationRepository.findByRecipientId(userId),
                "Unable to load notifications");
    }

    public ProjectNotification markNotificationRead(String userId, String notificationId) {
        return execute(
                () -> notificationRepository.markRead(notificationId, userId),
                "Unable to update the notification");
    }

    private Project requireProject(String projectId) {
        Project project = projectService.findById(projectId);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        return project;
    }

    private DashboardResponse.ProjectItem projectItem(
            Project project,
            Map<String, List<ProjectMembership>> membersByProject
    ) {
        return new DashboardResponse.ProjectItem(
                project,
                membersByProject.getOrDefault(project.projectId(), List.of()).size()
        );
    }

    private DashboardResponse.TeamMember teamMember(ProjectMembership membership) {
        return new DashboardResponse.TeamMember(
                membership.fullName(),
                membership.email(),
                membership.role(),
                membership.department(),
                membership.joinedAt()
        );
    }

    private <T> T execute(FirestoreOperation<T> operation, String message) {
        try {
            return operation.run();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(message, exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(message, cause);
        }
    }

    @FunctionalInterface
    private interface FirestoreOperation<T> {
        T run() throws ExecutionException, InterruptedException;
    }
}
