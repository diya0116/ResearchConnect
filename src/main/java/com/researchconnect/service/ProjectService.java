package com.researchconnect.service;

import com.google.firebase.auth.FirebaseToken;
import com.researchconnect.dto.ProjectRequest;
import com.researchconnect.entity.Project;
import com.researchconnect.entity.UserProfile;
import com.researchconnect.enums.ProjectStatus;
import com.researchconnect.repository.FirestoreProjectRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final FirestoreProjectRepository projectRepository;
    private final UserService userService;

    public ProjectService(
            FirestoreProjectRepository projectRepository,
            UserService userService
    ) {
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    public Project create(FirebaseToken token, ProjectRequest request) {
        validate(request);
        UserProfile owner = userService.findByUid(token.getUid());
        if (owner == null) {
            throw new IllegalStateException("Complete your profile before creating a project");
        }

        Instant now = Instant.now();
        Project project = new Project(
                UUID.randomUUID().toString(),
                request.title().trim(),
                request.shortDescription().trim(),
                request.description().trim(),
                request.category().trim(),
                request.department().trim(),
                request.requiredSkills().stream()
                        .map(String::trim)
                        .filter(skill -> !skill.isBlank())
                        .distinct()
                        .toList(),
                request.teamSize(),
                request.availablePositions(),
                request.duration().trim(),
                request.weeklyCommitment(),
                request.expectedStartDate(),
                request.objectives() == null ? "" : request.objectives().trim(),
                request.requirements() == null ? "" : request.requirements().trim(),
                owner.uid(),
                owner.fullName(),
                owner.role().name(),
                ProjectStatus.PUBLISHED,
                now,
                now
        );
        try {
            return projectRepository.save(project);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to save the project", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            log.error("Firestore project write failed: {}", cause.getMessage(), exception);
            throw new IllegalStateException("Firestore project write failed: " + cause.getMessage(), exception);
        }
    }

    public Project findById(String projectId) {
        try {
            return projectRepository.findById(projectId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to read the project", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new IllegalStateException("Firestore project read failed: " + cause.getMessage(), exception);
        }
    }

    public List<Project> findOwned(String ownerId) {
        try {
            return projectRepository.findByOwnerId(ownerId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to read your projects", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Unable to read your projects", exception);
        }
    }

    public Project update(String ownerId, String projectId, ProjectRequest request) {
        validate(request);
        Project existing = requireOwnedProject(ownerId, projectId);
        Project updated = new Project(
                    existing.projectId(),
                    request.title().trim(),
                    request.shortDescription().trim(),
                    request.description().trim(),
                    request.category().trim(),
                    request.department().trim(),
                    request.requiredSkills().stream().map(String::trim)
                            .filter(skill -> !skill.isBlank()).distinct().toList(),
                    request.teamSize(),
                    request.availablePositions(),
                    request.duration().trim(),
                    request.weeklyCommitment(),
                    request.expectedStartDate(),
                    request.objectives() == null ? "" : request.objectives().trim(),
                    request.requirements() == null ? "" : request.requirements().trim(),
                    existing.ownerId(),
                    existing.ownerName(),
                    existing.ownerRole(),
                    existing.status(),
                    existing.createdAt(),
                    Instant.now()
            );
        try {
            return projectRepository.update(updated);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to update the project", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Unable to update the project", exception);
        }
    }

    public void delete(String ownerId, String projectId) {
        requireOwnedProject(ownerId, projectId);
        try {
            projectRepository.delete(projectId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to delete the project", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Unable to delete the project", exception);
        }
    }

    public int memberCount(String projectId) {
        try {
            return projectRepository.countAcceptedMembers(projectId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to read project members", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Unable to read project members", exception);
        }
    }

    private Project requireOwnedProject(String ownerId, String projectId) {
        Project project = findById(projectId);
        if (project == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Project not found");
        }
        if (!ownerId.equals(project.ownerId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "You do not own this project");
        }
        return project;
    }

    public List<Project> findFiltered(String search, String department, String category, String projectType) {
        List<Project> projects;
        try {
            projects = projectRepository.findAll();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to read projects", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            log.error("Firestore project query failed: {}", cause.getMessage(), exception);
            throw new IllegalStateException("Firestore project query failed: " + cause.getMessage(), exception);
        }

        return projects.stream()
                .filter(project -> matchesText(project, search))
                .filter(project -> matchesFilter(project.department(), department))
                .filter(project -> matchesFilter(project.category(), category))
                .filter(project -> matchesProjectType(project.ownerRole(), projectType))
                .toList();
    }

    private boolean matchesText(Project project, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String query = search.toLowerCase(Locale.ROOT);
        String searchable = (project.title() + " " + project.shortDescription() + " "
                + project.description() + " " + String.join(" ", project.requiredSkills()))
                .toLowerCase(Locale.ROOT);
        return searchable.contains(query);
    }

    private boolean matchesFilter(String value, String filter) {
        return filter == null || filter.isBlank()
                || value != null && value.equalsIgnoreCase(filter);
    }

    private boolean matchesProjectType(String ownerRole, String projectType) {
        return projectType == null || projectType.isBlank()
                || ownerRole != null && ownerRole.equalsIgnoreCase(projectType);
    }

    private void validate(ProjectRequest request) {
        if (request.teamSize() < 1) {
            throw new IllegalArgumentException("Team size must be at least 1");
        }
        if (request.availablePositions() < 0
                || request.availablePositions() > request.teamSize()) {
            throw new IllegalArgumentException("Available positions must be between 0 and team size");
        }
        if (request.weeklyCommitment() < 1) {
            throw new IllegalArgumentException("Weekly commitment must be at least 1 hour");
        }
    }
}
