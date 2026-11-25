package com.researchconnect.service;

import com.researchconnect.entity.Project;
import com.researchconnect.entity.ProjectComment;
import com.researchconnect.entity.UserProfile;
import com.researchconnect.repository.FirestoreCommentRepository;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentService {

    private static final int MAX_COMMENT_LENGTH = 2000;
    private final FirestoreCommentRepository commentRepository;
    private final ProjectService projectService;
    private final UserService userService;

    public CommentService(
            FirestoreCommentRepository commentRepository,
            ProjectService projectService,
            UserService userService
    ) {
        this.commentRepository = commentRepository;
        this.projectService = projectService;
        this.userService = userService;
    }

    public List<ProjectComment> findByProjectId(String projectId) {
        requireProject(projectId);
        return execute(
                () -> commentRepository.findByProjectId(projectId),
                "Unable to load project comments");
    }

    public ProjectComment addComment(String authorId, String projectId, String text) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment text cannot be empty");
        }
        String normalizedText = text.trim();
        if (normalizedText.length() > MAX_COMMENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must be 2000 characters or fewer");
        }
        requireProject(projectId);
        UserProfile author = userService.findByUid(authorId);
        if (author == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Complete your profile before commenting");
        }
        return execute(
                () -> commentRepository.save(
                        projectId,
                        author.uid(),
                        valueOrEmpty(author.fullName()),
                        author.role().name(),
                        normalizedText),
                "Unable to save the comment");
    }

    public void deleteComment(String requesterId, String projectId, String commentId) {
        execute(
                () -> {
                    commentRepository.delete(projectId, commentId, requesterId);
                    return null;
                },
                "Unable to delete the comment");
    }

    private void requireProject(String projectId) {
        Project project = projectService.findById(projectId);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
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
