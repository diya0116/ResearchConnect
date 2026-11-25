package com.researchconnect.controller;

import com.researchconnect.dto.CommentRequest;
import com.researchconnect.entity.ProjectComment;
import com.researchconnect.security.FirebaseUserPrincipal;
import com.researchconnect.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<ProjectComment> comments(@PathVariable String projectId) {
        return commentService.findByProjectId(projectId);
    }

    @PostMapping
    public ResponseEntity<ProjectComment> addComment(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @PathVariable String projectId,
            @Valid @RequestBody CommentRequest request
    ) {
        ProjectComment comment = commentService.addComment(
                principal.getUsername(), projectId, request.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @PathVariable String projectId,
            @PathVariable String commentId
    ) {
        commentService.deleteComment(principal.getUsername(), projectId, commentId);
        return ResponseEntity.noContent().build();
    }
}
