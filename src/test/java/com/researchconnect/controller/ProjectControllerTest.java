package com.researchconnect.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.firebase.auth.FirebaseToken;
import com.researchconnect.dto.ProjectRequest;
import com.researchconnect.entity.Project;
import com.researchconnect.enums.ProjectStatus;
import com.researchconnect.security.FirebaseUserPrincipal;
import com.researchconnect.service.ProjectService;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    private FirebaseUserPrincipal principal;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FirebaseToken token = org.mockito.Mockito.mock(FirebaseToken.class);
        org.mockito.Mockito.lenient().when(token.getUid()).thenReturn("authenticated-user");
        principal = new FirebaseUserPrincipal(token);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectController(projectService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void projectApiRoutesReachExpectedServiceOperations() throws Exception {
        Project project = project();
        ProjectRequest request = request();
        when(projectService.findFiltered("AI", "Computer Science", "Robotics", "STUDENT"))
                .thenReturn(List.of(project));
        when(projectService.findById("project-id")).thenReturn(project);
        when(projectService.findOwned("authenticated-user")).thenReturn(List.of(project));
        when(projectService.memberCount("project-id")).thenReturn(2);
        when(projectService.create(any(FirebaseToken.class), any(ProjectRequest.class))).thenReturn(project);
        when(projectService.update("authenticated-user", "project-id", request)).thenReturn(project);

        mockMvc.perform(get("/api/projects")
                        .param("search", "AI")
                        .param("department", "Computer Science")
                        .param("category", "Robotics")
                        .param("projectType", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value("project-id"));
        mockMvc.perform(get("/api/projects/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ownerId").value("owner-id"));
        mockMvc.perform(get("/api/projects/project-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Robotics project"));
        mockMvc.perform(get("/api/projects/project-id/member-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
        mockMvc.perform(post("/api/projects")
                        .contentType("application/json")
                        .content(projectJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value("project-id"));
        mockMvc.perform(put("/api/projects/project-id")
                        .contentType("application/json")
                        .content(projectJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value("project-id"));
        mockMvc.perform(delete("/api/projects/project-id"))
                .andExpect(status().isNoContent());

        verify(projectService).findFiltered("AI", "Computer Science", "Robotics", "STUDENT");
        verify(projectService).findOwned("authenticated-user");
        verify(projectService).create(any(FirebaseToken.class), any(ProjectRequest.class));
        verify(projectService).update("authenticated-user", "project-id", request);
        verify(projectService).delete("authenticated-user", "project-id");
    }

    private ProjectRequest request() {
        return new ProjectRequest(
                "Robotics project", "Short description", "Detailed description", "Robotics",
                "Computer Science", List.of("Python", "Robotics"), 4, 2, "6 months", 6,
                LocalDate.parse("2026-12-01"), "", "");
    }

    private String projectJson() {
        return """
                {
                  "title":"Robotics project",
                  "shortDescription":"Short description",
                  "description":"Detailed description",
                  "category":"Robotics",
                  "department":"Computer Science",
                  "requiredSkills":["Python","Robotics"],
                  "teamSize":4,
                  "availablePositions":2,
                  "duration":"6 months",
                  "weeklyCommitment":6,
                  "expectedStartDate":"2026-12-01",
                  "objectives":"",
                  "requirements":""
                }
                """;
    }

    private Project project() {
        return new Project(
                "project-id", "Robotics project", "Short description", "Detailed description",
                "Robotics", "Computer Science", List.of("Python", "Robotics"), 4, 2,
                "6 months", 6, LocalDate.parse("2026-12-01"), "", "", "owner-id",
                "Project owner", "PROFESSOR", ProjectStatus.PUBLISHED, Instant.EPOCH, Instant.EPOCH);
    }
}
