package com.researchconnect.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FirestoreCommentRepositoryTest {

    @Mock
    private Firestore firestore;

    @Mock
    private Transaction transaction;

    @Mock
    private CollectionReference comments;

    @Mock
    private CollectionReference projects;

    @Mock
    private DocumentReference commentReference;

    @Mock
    private DocumentReference projectReference;

    @Mock
    private DocumentSnapshot commentSnapshot;

    @Mock
    private DocumentSnapshot projectSnapshot;

    private FirestoreCommentRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        repository = new FirestoreCommentRepository(firestore);
        when(firestore.collection("project_comments")).thenReturn(comments);
        when(firestore.collection("projects")).thenReturn(projects);
        when(comments.document("comment-id")).thenReturn(commentReference);
        when(projects.document("project-id")).thenReturn(projectReference);
        when(transaction.get(commentReference)).thenReturn(ApiFutures.immediateFuture(commentSnapshot));
        when(transaction.get(projectReference)).thenReturn(ApiFutures.immediateFuture(projectSnapshot));
        when(commentSnapshot.exists()).thenReturn(true);
        when(commentSnapshot.getString("projectId")).thenReturn("project-id");
        when(projectSnapshot.exists()).thenReturn(true);
        when(projectSnapshot.getString("ownerId")).thenReturn("project-owner");
        when(firestore.<Void>runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<Void> function = invocation.getArgument(0);
            return ApiFutures.immediateFuture(function.updateCallback(transaction));
        });
    }

    @Test
    void commentAuthorCanDeleteTheirOwnComment() throws Exception {
        when(commentSnapshot.getString("authorId")).thenReturn("comment-author");

        repository.delete("project-id", "comment-id", "comment-author");

        verify(transaction).delete(commentReference);
    }

    @Test
    void projectOwnerCanModerateComment() throws Exception {
        when(commentSnapshot.getString("authorId")).thenReturn("comment-author");

        repository.delete("project-id", "comment-id", "project-owner");
        verify(transaction).delete(commentReference);
    }

    @Test
    void unrelatedUserCannotDeleteComment() {
        when(commentSnapshot.getString("authorId")).thenReturn("comment-author");

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> repository.delete("project-id", "comment-id", "other-user"));
        assertEquals(403, error.getStatusCode().value());
        verify(transaction, never()).delete(commentReference);
    }
}
