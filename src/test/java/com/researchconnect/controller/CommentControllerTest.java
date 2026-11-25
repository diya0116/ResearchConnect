package com.researchconnect.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.firebase.auth.FirebaseToken;
import com.researchconnect.entity.ProjectComment;
import com.researchconnect.security.FirebaseUserPrincipal;
import com.researchconnect.service.CommentService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FirebaseToken token = org.mockito.Mockito.mock(FirebaseToken.class);
        org.mockito.Mockito.lenient().when(token.getUid()).thenReturn("authenticated-user");
        FirebaseUserPrincipal principal = new FirebaseUserPrincipal(token);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new CommentController(commentService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void commentsApiUsesAuthenticatedIdentityAndSupportsReadCreateDelete() throws Exception {
        ProjectComment comment = new ProjectComment(
                "comment-id", "project-id", "authenticated-user", "Member", "STUDENT",
                "Comment text", Instant.EPOCH);
        when(commentService.findByProjectId("project-id")).thenReturn(List.of(comment));
        when(commentService.addComment("authenticated-user", "project-id", "Comment text"))
                .thenReturn(comment);

        mockMvc.perform(get("/api/projects/project-id/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorId").value("authenticated-user"));
        mockMvc.perform(post("/api/projects/project-id/comments")
                        .contentType("application/json")
                        .content("{\"text\":\"Comment text\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Member"));
        mockMvc.perform(delete("/api/projects/project-id/comments/comment-id"))
                .andExpect(status().isNoContent());

        verify(commentService).addComment("authenticated-user", "project-id", "Comment text");
        verify(commentService).deleteComment("authenticated-user", "project-id", "comment-id");
    }

    @Test
    void blankCommentIsRejectedByRequestValidation() throws Exception {
        mockMvc.perform(post("/api/projects/project-id/comments")
                        .contentType("application/json")
                        .content("{\"text\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }
}
