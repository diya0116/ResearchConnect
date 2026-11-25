package com.researchconnect.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.researchconnect.entity.ProjectNotification;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class FirestoreNotificationRepository {

    private static final String NOTIFICATIONS = "notifications";
    private final Firestore firestore;

    public FirestoreNotificationRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<ProjectNotification> findByRecipientId(String recipientId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection(NOTIFICATIONS)
                .whereEqualTo("recipientId", recipientId)
                .get()
                .get();
        List<ProjectNotification> result = new ArrayList<>();
        for (QueryDocumentSnapshot document : snapshot.getDocuments()) {
            result.add(notificationFrom(document));
        }
        result.sort(Comparator.comparing(ProjectNotification::createdAt).reversed());
        return result;
    }

    public ProjectNotification markRead(String notificationId, String recipientId)
            throws ExecutionException, InterruptedException {
        DocumentReference reference = firestore.collection(NOTIFICATIONS).document(notificationId);
        ApiFuture<ProjectNotification> transactionFuture = firestore.runTransaction(transaction -> {
            DocumentSnapshot document = transaction.get(reference).get();
            if (!document.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
            }
            if (!recipientId.equals(document.getString("recipientId"))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot update this notification");
            }
            boolean read = Boolean.TRUE.equals(document.getBoolean("read"));
            if (!read) {
                transaction.update(reference, Map.of("read", true));
            }
            return new ProjectNotification(
                    document.getId(),
                    valueOrEmpty(document.getString("message")),
                    valueOrEmpty(document.getString("projectId")),
                    valueOrEmpty(document.getString("projectTitle")),
                    instantOrEpoch(document.getString("createdAt")),
                    true
            );
        });
        return await(transactionFuture);
    }

    private ProjectNotification notificationFrom(DocumentSnapshot document) {
        return new ProjectNotification(
                document.getId(),
                valueOrEmpty(document.getString("message")),
                valueOrEmpty(document.getString("projectId")),
                valueOrEmpty(document.getString("projectTitle")),
                instantOrEpoch(document.getString("createdAt")),
                Boolean.TRUE.equals(document.getBoolean("read"))
        );
    }

    private Instant instantOrEpoch(String value) {
        return value == null || value.isBlank() ? Instant.EPOCH : Instant.parse(value);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private <T> T await(ApiFuture<T> future) throws ExecutionException, InterruptedException {
        try {
            return future.get();
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }
}
