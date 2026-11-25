package com.researchconnect.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;

import com.researchconnect.entity.Project;
import com.researchconnect.entity.ProjectComment;
import com.researchconnect.entity.UserProfile;
import com.researchconnect.enums.ProjectStatus;
import com.researchconnect.enums.UserRole;
import com.researchconnect.repository.FirestoreCommentRepository;
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
class CommentServiceTest {

    @Mock
    private FirestoreCommentRepository commentRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, projectService, userService);
    }

    @Test
    void commentAuthorIsLoadedFromAuthenticatedUserProfile() throws Exception {
        UserProfile profile = new UserProfile(
                "verified-user", "verified@thapar.edu", "Verified Name", "", UserRole.STUDENT,
                "Computer Science", "", "", Instant.EPOCH, Instant.EPOCH);
        ProjectComment saved = new ProjectComment(
                "comment-id", "project-id", "verified-user", "Verified Name", "STUDENT",
                "Helpful note", Instant.EPOCH);
        when(projectService.findById("project-id")).thenReturn(project());
        when(userService.findByUid("verified-user")).thenReturn(profile);
        when(commentRepository.save(
                "project-id", "verified-user", "Verified Name", "STUDENT", "Helpful note"))
                .thenReturn(saved);

        assertEquals(saved, commentService.addComment("verified-user", "project-id", " Helpful note "));
        verify(userService).findByUid("verified-user");
        verify(commentRepository).save(
                "project-id", "verified-user", "Verified Name", "STUDENT", "Helpful note");
    }

    @Test
    void rejectsBlankAndOversizedComments() throws Exception {
        assertEquals(400, assertThrows(
                ResponseStatusException.class,
                () -> commentService.addComment("verified-user", "project-id", "  "))
                .getStatusCode().value());
        assertEquals(400, assertThrows(
                ResponseStatusException.class,
                () -> commentService.addComment("verified-user", "project-id", "x".repeat(2001)))
                .getStatusCode().value());
        verify(commentRepository, never()).save(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void profileIsRequiredBeforeAddingComment() throws Exception {
        when(projectService.findById("project-id")).thenReturn(project());
        when(userService.findByUid("verified-user")).thenReturn(null);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> commentService.addComment("verified-user", "project-id", "Comment"));

        assertEquals(409, error.getStatusCode().value());
        verify(commentRepository, never()).save(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    private Project project() {
        return new Project(
                "project-id", "Project", "Short description", "Description", "AI", "Computer Science",
                List.of("Java"), 4, 2, "6 months", 5, LocalDate.parse("2026-12-01"),
                "", "", "owner-id", "Owner", "STUDENT", ProjectStatus.PUBLISHED,
                Instant.EPOCH, Instant.EPOCH);
    }
}
