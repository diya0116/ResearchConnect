package com.researchconnect.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.researchconnect.entity.ProjectComment;
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
public class FirestoreCommentRepository {

    private static final String COMMENTS = "project_comments";
    private static final String PROJECTS = "projects";
    private final Firestore firestore;

    public FirestoreCommentRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<ProjectComment> findByProjectId(String projectId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = comments().whereEqualTo("projectId", projectId).get().get();
        List<ProjectComment> result = new ArrayList<>();
        for (QueryDocumentSnapshot document : snapshot.getDocuments()) {
            result.add(commentFrom(document));
        }
        result.sort(Comparator.comparing(ProjectComment::postedAt));
        return result;
    }

    public ProjectComment save(
            String projectId,
            String authorId,
            String authorName,
            String authorRole,
            String text
    ) throws ExecutionException, InterruptedException {
        DocumentReference reference = comments().document();
        Instant now = Instant.now();
        ProjectComment comment = new ProjectComment(
                reference.getId(), projectId, authorId, authorName, authorRole, text, now);
        Map<String, Object> values = Map.of(
                "projectId", projectId,
                "authorId", authorId,
                "authorName", authorName,
                "authorRole", authorRole,
                "text", text,
                "postedAt", now.toString()
        );
        ApiFuture<WriteResult> write = reference.set(values);
        write.get();
        return comment;
    }

    public void delete(String projectId, String commentId, String requesterId)
            throws ExecutionException, InterruptedException {
        DocumentReference commentReference = comments().document(commentId);
        DocumentReference projectReference = firestore.collection(PROJECTS).document(projectId);
        ApiFuture<Void> deletion = firestore.runTransaction(transaction -> {
            DocumentSnapshot comment = transaction.get(commentReference).get();
            DocumentSnapshot project = transaction.get(projectReference).get();
            if (!comment.exists() || !project.exists()
                    || !projectId.equals(comment.getString("projectId"))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
            }
            String authorId = comment.getString("authorId");
            String ownerId = project.getString("ownerId");
            if (!requesterId.equals(authorId) && !requesterId.equals(ownerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete this comment");
            }
            transaction.delete(commentReference);
            return null;
        });
        await(deletion);
    }

    private ProjectComment commentFrom(DocumentSnapshot document) {
        String postedAt = document.getString("postedAt");
        return new ProjectComment(
                document.getId(),
                document.getString("projectId"),
                document.getString("authorId"),
                valueOrEmpty(document.getString("authorName")),
                valueOrEmpty(document.getString("authorRole")),
                valueOrEmpty(document.getString("text")),
                postedAt == null || postedAt.isBlank() ? Instant.EPOCH : Instant.parse(postedAt)
        );
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private CollectionReference comments() {
        return firestore.collection(COMMENTS);
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
