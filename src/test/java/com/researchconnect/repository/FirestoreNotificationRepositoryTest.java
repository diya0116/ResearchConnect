package com.researchconnect.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import com.researchconnect.entity.ProjectNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FirestoreNotificationRepositoryTest {

    @Mock
    private Firestore firestore;

    @Mock
    private Transaction transaction;

    @Mock
    private CollectionReference notifications;

    @Mock
    private DocumentReference notificationReference;

    @Mock
    private DocumentSnapshot notificationSnapshot;

    private FirestoreNotificationRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        repository = new FirestoreNotificationRepository(firestore);
        when(firestore.collection("notifications")).thenReturn(notifications);
        when(notifications.document("notification-id")).thenReturn(notificationReference);
        when(transaction.get(notificationReference))
                .thenReturn(ApiFutures.immediateFuture(notificationSnapshot));
        when(notificationSnapshot.exists()).thenReturn(true);
        when(notificationSnapshot.getString("recipientId")).thenReturn("recipient-a");
        when(firestore.<ProjectNotification>runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<ProjectNotification> function = invocation.getArgument(0);
            return ApiFutures.immediateFuture(function.updateCallback(transaction));
        });
    }

    @Test
    void recipientCanMarkNotificationRead() throws Exception {
        when(notificationSnapshot.getString("message")).thenReturn("Request accepted");
        when(notificationSnapshot.getString("projectId")).thenReturn("project-id");
        when(notificationSnapshot.getString("projectTitle")).thenReturn("Project");
        when(notificationSnapshot.getString("createdAt")).thenReturn("2026-01-01T00:00:00Z");
        when(notificationSnapshot.getBoolean("read")).thenReturn(false);
        when(notificationSnapshot.getId()).thenReturn("notification-id");

        ProjectNotification result = repository.markRead("notification-id", "recipient-a");

        assertTrue(result.read());
        assertEquals("notification-id", result.notificationId());
        verify(transaction).update(
                org.mockito.ArgumentMatchers.eq(notificationReference),
                org.mockito.ArgumentMatchers.argThat(values -> Boolean.TRUE.equals(values.get("read"))));
    }

    @Test
    void anotherUserCannotMarkNotificationRead() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> repository.markRead("notification-id", "recipient-b"));

        assertEquals(403, error.getStatusCode().value());
        verify(transaction, never()).update(
                org.mockito.ArgumentMatchers.eq(notificationReference), anyMap());
    }
}
