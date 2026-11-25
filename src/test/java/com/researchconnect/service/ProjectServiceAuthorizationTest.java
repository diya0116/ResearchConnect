package com.researchconnect.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.researchconnect.dto.ProjectRequest;
import com.researchconnect.entity.Project;
import com.researchconnect.enums.ProjectStatus;
import com.researchconnect.repository.FirestoreProjectRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ProjectServiceAuthorizationTest {

    @Mock
    private FirestoreProjectRepository projectRepository;

    @Mock
    private UserService userService;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, userService);
    }

    @Test
    void nonOwnerCannotUpdateProject() throws Exception {
        when(projectRepository.findById("project-id")).thenReturn(project("owner-a"));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> projectService.update("owner-b", "project-id", projectRequest()));

        assertEquals(403, error.getStatusCode().value());
        verify(projectRepository, never()).update(any(Project.class));
    }

    @Test
    void nonOwnerCannotDeleteProject() throws Exception {
        when(projectRepository.findById("project-id")).thenReturn(project("owner-a"));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> projectService.delete("owner-b", "project-id"));

        assertEquals(403, error.getStatusCode().value());
        verify(projectRepository, never()).delete("project-id");
    }

    @Test
    void projectSearchAndFiltersReturnOnlyMatchingProjects() throws Exception {
        Project matching = project("owner-a");
        Project other = new Project(
                "other-project", "Marine biology", "Field research", "Marine ecology",
                "Biotechnology", "Biotechnology", List.of("Lab work"), 3, 1, "3 months", 4,
                LocalDate.parse("2026-12-01"), "", "", "owner-b", "Owner B", "PROFESSOR",
                ProjectStatus.PUBLISHED, Instant.EPOCH, Instant.EPOCH);
        when(projectRepository.findAll()).thenReturn(List.of(matching, other));

        List<Project> result = projectService.findFiltered(
                "java", "Computer Science", "AI", "STUDENT");

        assertEquals(List.of(matching), result);
    }

    private Project project(String ownerId) {
        return new Project(
                "project-id", "Project", "Short description", "Description", "AI", "Computer Science",
                List.of("Java"), 4, 2, "6 months", 5, LocalDate.parse("2026-12-01"),
                "", "", ownerId, "Owner", "STUDENT", ProjectStatus.PUBLISHED,
                Instant.EPOCH, Instant.EPOCH);
    }

    private ProjectRequest projectRequest() {
        return new ProjectRequest(
                "Project", "Short description", "Description", "AI", "Computer Science",
                List.of("Java"), 4, 2, "6 months", 5, LocalDate.parse("2026-12-01"), "", "");
    }
}
