package com.researchconnect.controller;

import com.researchconnect.dto.ProjectRequest;
import com.researchconnect.entity.Project;
import com.researchconnect.security.FirebaseUserPrincipal;
import com.researchconnect.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<Project> feed(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String projectType
    ) {
        return projectService.findFiltered(search, department, category, projectType);
    }

    @GetMapping("/{projectId}")
    public Project details(@PathVariable String projectId) {
        Project project = projectService.findById(projectId);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        return project;
    }

    @GetMapping("/mine")
    public List<Project> mine(@AuthenticationPrincipal FirebaseUserPrincipal principal) {
        return projectService.findOwned(principal.getUsername());
    }

    @GetMapping("/{projectId}/member-count")
    public Map<String, Integer> memberCount(@PathVariable String projectId) {
        return Map.of("count", projectService.memberCount(projectId));
    }

    @PostMapping
    public ResponseEntity<Project> create(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @Valid @RequestBody ProjectRequest request
    ) {
        Project project = projectService.create(principal.token(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    @PutMapping("/{projectId}")
    public Project update(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @PathVariable String projectId,
            @Valid @RequestBody ProjectRequest request
    ) {
        return projectService.update(principal.getUsername(), projectId, request);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @PathVariable String projectId
    ) {
        projectService.delete(principal.getUsername(), projectId);
        return ResponseEntity.noContent().build();
    }
}
