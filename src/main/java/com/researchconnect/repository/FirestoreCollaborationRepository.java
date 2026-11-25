package com.researchconnect.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Transaction;
import com.google.cloud.firestore.WriteResult;
import com.researchconnect.entity.ProjectApplication;
import com.researchconnect.entity.ProjectMembership;
import com.researchconnect.entity.SavedProject;
import com.researchconnect.entity.UserProfile;
import com.researchconnect.enums.ApplicationStatus;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class FirestoreCollaborationRepository {

    private static final String APPLICATIONS = "project_applications";
    private static final String MEMBERS = "project_members";
    private static final String SAVED_PROJECTS = "saved_projects";
    private static final String PROJECTS = "projects";
    private static final String NOTIFICATIONS = "notifications";
    private final Firestore firestore;

    public FirestoreCollaborationRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public ProjectApplication findApplication(String applicationId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot document = applications().document(applicationId).get().get();
        return document.exists() ? applicationFrom(document) : null;
    }

    public List<ProjectApplication> findApplicationsByApplicant(String applicantId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = applications()
                .whereEqualTo("applicantId", applicantId)
                .get()
                .get();
        return applicationsFrom(snapshot.getDocuments());
    }

    public List<ProjectApplication> findApplicationsForProjects(Collection<String> projectIds)
            throws ExecutionException, InterruptedException {
        List<String> ids = projectIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        List<ProjectApplication> result = new ArrayList<>();
        for (int start = 0; start < ids.size(); start += 30) {
            List<String> batch = ids.subList(start, Math.min(start + 30, ids.size()));
            QuerySnapshot snapshot = applications().whereIn("projectId", batch).get().get();
            result.addAll(applicationsFrom(snapshot.getDocuments()));
        }
        return result;
    }

    public List<ProjectMembership> findMembershipsByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = memberships().whereEqualTo("userId", userId).get().get();
        return membershipsFrom(snapshot.getDocuments());
    }

    public List<ProjectMembership> findMembershipsForProjects(Collection<String> projectIds)
            throws ExecutionException, InterruptedException {
        List<String> ids = projectIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        List<ProjectMembership> result = new ArrayList<>();
        for (int start = 0; start < ids.size(); start += 30) {
            List<String> batch = ids.subList(start, Math.min(start + 30, ids.size()));
            QuerySnapshot snapshot = memberships().whereIn("projectId", batch).get().get();
            result.addAll(membershipsFrom(snapshot.getDocuments()));
        }
        return result;
    }

    public List<SavedProject> findSavedProjects(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = savedProjects().whereEqualTo("userId", userId).get().get();
        List<SavedProject> result = new ArrayList<>();
        for (QueryDocumentSnapshot document : snapshot.getDocuments()) {
            result.add(savedProjectFrom(document));
        }
        return result;
    }

    public boolean isSaved(String userId, String projectId) throws ExecutionException, InterruptedException {
        return savedProjects().document(compositeId(userId, projectId)).get().get().exists();
    }

    public void saveProject(String userId, String projectId) throws ExecutionException, InterruptedException {
        Instant now = Instant.now();
        Map<String, Object> values = Map.of(
                "userId", userId,
                "projectId", projectId,
                "savedAt", now.toString()
        );
        ApiFuture<WriteResult> write = savedProjects()
                .document(compositeId(userId, projectId))
                .set(values);
        write.get();
    }

    public void removeSavedProject(String userId, String projectId)
            throws ExecutionException, InterruptedException {
        savedProjects().document(compositeId(userId, projectId)).delete().get();
    }

    public ProjectApplication submitApplication(String projectId, UserProfile applicant)
            throws ExecutionException, InterruptedException {
        String applicantId = applicant.uid();
        String applicationId = compositeId(projectId, applicantId);
        DocumentReference projectRef = firestore.collection(PROJECTS).document(projectId);
        DocumentReference applicationRef = applications().document(applicationId);
        DocumentReference memberRef = memberships().document(compositeId(projectId, applicantId));
        DocumentReference notificationRef = notifications().document();

        ApiFuture<ProjectApplication> transactionFuture = firestore.runTransaction(transaction -> {
            DocumentSnapshot project = transaction.get(projectRef).get();
            DocumentSnapshot existingApplication = transaction.get(applicationRef).get();
            DocumentSnapshot existingMembership = transaction.get(memberRef).get();
            if (!project.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
            }
            if (applicantId.equals(project.getString("ownerId"))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Project owners cannot apply to their own project");
            }
            if (!"PUBLISHED".equals(project.getString("status"))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This project is not accepting applications");
            }
            Long availablePositions = project.getLong("availablePositions");
            if (availablePositions == null || availablePositions < 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This project has no available positions");
            }
            if (existingMembership.exists()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already a member of this project");
            }
            if (existingApplication.exists()
                    && !"REJECTED".equals(existingApplication.getString("status"))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an application for this project");
            }

            Instant now = Instant.now();
            Map<String, Object> values = applicationValues(
                    projectId, applicant, ApplicationStatus.PENDING, now, now);
            transaction.set(applicationRef, values);
            transaction.set(notificationRef, notificationValues(
                    project.getString("ownerId"),
                    displayName(applicant) + " requested to join "
                            + valueOrEmpty(project.getString("title")) + ".",
                    projectId,
                    valueOrEmpty(project.getString("title")),
                    now
            ));
            return applicationFrom(applicationId, values);
        });
        return await(transactionFuture);
    }

    public ProjectApplication decideApplication(
            String applicationId,
            String ownerId,
            ApplicationStatus decision
    ) throws ExecutionException, InterruptedException {
        if (decision == ApplicationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be ACCEPTED or REJECTED");
        }

        DocumentReference applicationRef = applications().document(applicationId);
        DocumentReference notificationRef = notifications().document();
        ApiFuture<ProjectApplication> transactionFuture = firestore.runTransaction(transaction -> {
            DocumentSnapshot application = transaction.get(applicationRef).get();
            if (!application.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found");
            }
            if (!"PENDING".equals(application.getString("status"))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This application has already been decided");
            }

            String projectId = application.getString("projectId");
            String applicantId = application.getString("applicantId");
            DocumentReference projectRef = firestore.collection(PROJECTS).document(projectId);
            DocumentSnapshot project = transaction.get(projectRef).get();
            if (!project.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
            }
            if (!ownerId.equals(project.getString("ownerId"))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the project owner can decide applications");
            }

            DocumentReference memberRef = memberships().document(compositeId(projectId, applicantId));
            DocumentSnapshot existingMembership = decision == ApplicationStatus.ACCEPTED
                    ? transaction.get(memberRef).get()
                    : null;
            Instant now = Instant.now();
            Map<String, Object> applicationUpdates = Map.of(
                    "status", decision.name(),
                    "updatedAt", now.toString()
            );
            if (decision == ApplicationStatus.ACCEPTED) {
                Long availablePositions = project.getLong("availablePositions");
                if (availablePositions == null || availablePositions < 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "This project has no available positions");
                }
                if (existingMembership.exists()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Applicant is already a project member");
                }
                Map<String, Object> memberValues = Map.of(
                        "projectId", projectId,
                        "userId", applicantId,
                        "fullName", valueOrEmpty(application.getString("applicantName")),
                        "email", valueOrEmpty(application.getString("applicantEmail")),
                        "memberRole", valueOrEmpty(application.getString("applicantRole")),
                        "department", valueOrEmpty(application.getString("applicantDepartment")),
                        "status", ApplicationStatus.ACCEPTED.name(),
                        "joinedAt", now.toString()
                );
                transaction.set(memberRef, memberValues);
                transaction.update(projectRef, Map.of(
                        "availablePositions", availablePositions - 1,
                        "updatedAt", now.toString()
                ));
            }
            transaction.update(applicationRef, applicationUpdates);
            transaction.set(notificationRef, notificationValues(
                    applicantId,
                    "Your join request to " + valueOrEmpty(project.getString("title"))
                            + (decision == ApplicationStatus.ACCEPTED ? " was accepted." : " was rejected."),
                    projectId,
                    valueOrEmpty(project.getString("title")),
                    now
            ));
            Map<String, Object> updatedApplication = new HashMap<>(application.getData());
            updatedApplication.putAll(applicationUpdates);
            return applicationFrom(applicationId, updatedApplication);
        });
        return await(transactionFuture);
    }

    private List<ProjectApplication> applicationsFrom(List<QueryDocumentSnapshot> documents) {
        List<ProjectApplication> result = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            result.add(applicationFrom(document));
        }
        return result;
    }

    private List<ProjectMembership> membershipsFrom(List<QueryDocumentSnapshot> documents) {
        List<ProjectMembership> result = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            String status = document.getString("status");
            if (status == null || "ACCEPTED".equals(status)) {
                result.add(new ProjectMembership(
                        document.getId(),
                        document.getString("projectId"),
                        document.getString("userId"),
                        valueOrEmpty(document.getString("fullName")),
                        valueOrEmpty(document.getString("email")),
                        valueOrEmpty(document.getString("memberRole")),
                        valueOrEmpty(document.getString("department")),
                        instantOrEpoch(document.getString("joinedAt"))
                ));
            }
        }
        return result;
    }

    private SavedProject savedProjectFrom(DocumentSnapshot document) {
        return new SavedProject(
                document.getId(),
                document.getString("projectId"),
                document.getString("userId"),
                instantOrEpoch(document.getString("savedAt"))
        );
    }

    private ProjectApplication applicationFrom(DocumentSnapshot document) {
        return new ProjectApplication(
                document.getId(),
                document.getString("projectId"),
                document.getString("applicantId"),
                valueOrEmpty(document.getString("applicantName")),
                valueOrEmpty(document.getString("applicantEmail")),
                valueOrEmpty(document.getString("applicantRole")),
                valueOrEmpty(document.getString("applicantDepartment")),
                ApplicationStatus.valueOf(document.getString("status")),
                instantOrEpoch(document.getString("appliedAt")),
                instantOrEpoch(document.getString("updatedAt"))
        );
    }

    private ProjectApplication applicationFrom(String applicationId, Map<String, Object> values) {
        return new ProjectApplication(
                applicationId,
                (String) values.get("projectId"),
                (String) values.get("applicantId"),
                (String) values.get("applicantName"),
                (String) values.get("applicantEmail"),
                (String) values.get("applicantRole"),
                (String) values.get("applicantDepartment"),
                ApplicationStatus.valueOf((String) values.get("status")),
                Instant.parse((String) values.get("appliedAt")),
                Instant.parse((String) values.get("updatedAt"))
        );
    }

    private Map<String, Object> applicationValues(
            String projectId,
            UserProfile applicant,
            ApplicationStatus status,
            Instant appliedAt,
            Instant updatedAt
    ) {
        return Map.of(
                "projectId", projectId,
                "applicantId", applicant.uid(),
                "applicantName", valueOrEmpty(applicant.fullName()),
                "applicantEmail", valueOrEmpty(applicant.email()),
                "applicantRole", applicant.role().name(),
                "applicantDepartment", valueOrEmpty(applicant.department()),
                "status", status.name(),
                "appliedAt", appliedAt.toString(),
                "updatedAt", updatedAt.toString()
        );
    }

    private Map<String, Object> notificationValues(
            String recipientId,
            String message,
            String projectId,
            String projectTitle,
            Instant createdAt
    ) {
        return Map.of(
                "recipientId", recipientId,
                "message", message,
                "projectId", projectId,
                "projectTitle", projectTitle,
                "createdAt", createdAt.toString(),
                "read", false
        );
    }

    private String displayName(UserProfile profile) {
        String name = valueOrEmpty(profile.fullName());
        return name.isBlank() ? valueOrEmpty(profile.email()) : name;
    }

    private Instant instantOrEpoch(String value) {
        return value == null || value.isBlank() ? Instant.EPOCH : Instant.parse(value);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String compositeId(String first, String second) {
        byte[] value = (first + "\u0000" + second).getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private CollectionReference applications() {
        return firestore.collection(APPLICATIONS);
    }

    private CollectionReference memberships() {
        return firestore.collection(MEMBERS);
    }

    private CollectionReference savedProjects() {
        return firestore.collection(SAVED_PROJECTS);
    }

    private CollectionReference notifications() {
        return firestore.collection(NOTIFICATIONS);
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
