package com.researchconnect.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.researchconnect.entity.Project;
import com.researchconnect.enums.ProjectStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class FirestoreProjectRepository {

    private static final Logger log = LoggerFactory.getLogger(FirestoreProjectRepository.class);
    private static final String COLLECTION = "projects";
    private final Firestore firestore;

    public FirestoreProjectRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Project save(Project project) throws ExecutionException, InterruptedException {
        Map<String, Object> values = new HashMap<>();
        values.put("projectId", project.projectId());
        values.put("title", project.title());
        values.put("shortDescription", project.shortDescription());
        values.put("description", project.description());
        values.put("category", project.category());
        values.put("department", project.department());
        values.put("requiredSkills", project.requiredSkills());
        values.put("teamSize", project.teamSize());
        values.put("availablePositions", project.availablePositions());
        values.put("duration", project.duration());
        values.put("weeklyCommitment", project.weeklyCommitment());
        values.put("expectedStartDate", project.expectedStartDate().toString());
        values.put("objectives", project.objectives());
        values.put("requirements", project.requirements());
        values.put("ownerId", project.ownerId());
        values.put("ownerName", project.ownerName());
        values.put("ownerRole", project.ownerRole());
        values.put("status", project.status().name());
        values.put("createdAt", project.createdAt().toString());
        values.put("updatedAt", project.updatedAt().toString());
        try {
            firestore.collection(COLLECTION).document(project.projectId()).set(values).get();
            log.info("Created Firestore project document: projectId={}, collection={}, projectIdConfig={}",
                    project.projectId(), COLLECTION, firestore.getOptions().getProjectId());
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Failed to write Firestore project: projectId={}, collection={}",
                    project.projectId(), COLLECTION, exception);
            throw exception;
        }
        return project;
    }

    public Project update(Project project) throws ExecutionException, InterruptedException {
        Map<String, Object> values = new HashMap<>();
        values.put("title", project.title());
        values.put("shortDescription", project.shortDescription());
        values.put("description", project.description());
        values.put("category", project.category());
        values.put("department", project.department());
        values.put("requiredSkills", project.requiredSkills());
        values.put("teamSize", project.teamSize());
        values.put("availablePositions", project.availablePositions());
        values.put("duration", project.duration());
        values.put("weeklyCommitment", project.weeklyCommitment());
        values.put("expectedStartDate", project.expectedStartDate() == null
                ? "" : project.expectedStartDate().toString());
        values.put("objectives", project.objectives());
        values.put("requirements", project.requirements());
        values.put("updatedAt", project.updatedAt().toString());
        firestore.collection(COLLECTION).document(project.projectId()).update(values).get();
        return project;
    }

    public void delete(String projectId) throws ExecutionException, InterruptedException {
        firestore.collection(COLLECTION).document(projectId).delete().get();
    }

    public List<Project> findByOwnerId(String ownerId) throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection(COLLECTION)
                .whereEqualTo("ownerId", ownerId)
                .get()
                .get();
        List<Project> owned = new ArrayList<>();
        for (QueryDocumentSnapshot document : snapshot.getDocuments()) {
            owned.add(fromDocument(document));
        }
        owned.sort(Comparator.comparing(Project::createdAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return owned;
    }

    public List<Project> findByIds(Collection<String> projectIds)
            throws ExecutionException, InterruptedException {
        List<String> ids = projectIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        List<Project> projects = new ArrayList<>();
        for (int start = 0; start < ids.size(); start += 30) {
            List<String> batch = ids.subList(start, Math.min(start + 30, ids.size()));
            QuerySnapshot snapshot = firestore.collection(COLLECTION)
                    .whereIn(FieldPath.documentId(), batch)
                    .get()
                    .get();
            for (QueryDocumentSnapshot document : snapshot.getDocuments()) {
                projects.add(fromDocument(document));
            }
        }
        return projects;
    }

    public int countAcceptedMembers(String projectId) throws ExecutionException, InterruptedException {
        return firestore.collection("project_members")
                .whereEqualTo("projectId", projectId)
                .get()
                .get()
                .getDocuments()
                .size();
    }

    public Project findById(String projectId) throws ExecutionException, InterruptedException {
        DocumentSnapshot document = firestore.collection(COLLECTION).document(projectId).get().get();
        return document.exists() ? fromDocument(document) : null;
    }

    public List<Project> findAll() throws ExecutionException, InterruptedException {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION).get();
            List<Project> projects = new ArrayList<>();
            for (QueryDocumentSnapshot document : future.get().getDocuments()) {
                try {
                    projects.add(fromDocument(document));
                } catch (RuntimeException exception) {
                    log.error("Skipping malformed Firestore project document: collection={}, documentId={}",
                            COLLECTION, document.getId(), exception);
                }
            }
            projects.sort(Comparator.comparing(Project::createdAt,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            log.info("Read {} documents from Firestore collection '{}' in project '{}'",
                    projects.size(), COLLECTION, firestore.getOptions().getProjectId());
            return projects;
        } catch (InterruptedException | ExecutionException exception) {
            log.error("Failed to read Firestore collection '{}'", COLLECTION, exception);
            throw exception;
        }
    }

    private Project fromDocument(DocumentSnapshot document) {
        Object rawSkills = document.get("requiredSkills");
        List<String> skills = rawSkills instanceof List<?> values
                ? values.stream().map(Object::toString).toList()
                : List.of();
        return new Project(
                document.getId(),
                stringValue(document, "title"),
                stringValue(document, "shortDescription"),
                stringValue(document, "description"),
                stringValue(document, "category"),
                stringValue(document, "department"),
                skills,
                intValue(document, "teamSize"),
                intValue(document, "availablePositions"),
                stringValue(document, "duration"),
                intValue(document, "weeklyCommitment"),
                dateValue(document, "expectedStartDate"),
                stringValue(document, "objectives"),
                stringValue(document, "requirements"),
                stringValue(document, "ownerId"),
                stringValue(document, "ownerName"),
                stringValue(document, "ownerRole"),
                statusValue(document),
                instantValue(document, "createdAt"),
                instantValue(document, "updatedAt")
        );
    }

    private String stringValue(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        return value == null ? "" : value;
    }

    private int intValue(DocumentSnapshot document, String field) {
        Long value = document.getLong(field);
        return value == null ? 0 : value.intValue();
    }

    private LocalDate dateValue(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            log.warn("Invalid date field '{}' in project document '{}'", field, document.getId());
            return null;
        }
    }

    private Instant instantValue(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        if (value == null || value.isBlank()) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            log.warn("Invalid timestamp field '{}' in project document '{}'", field, document.getId());
            return Instant.EPOCH;
        }
    }

    private ProjectStatus statusValue(DocumentSnapshot document) {
        String value = document.getString("status");
        if (value == null) {
            return ProjectStatus.PUBLISHED;
        }
        try {
            return ProjectStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            log.warn("Invalid project status '{}' in document '{}'", value, document.getId());
            return ProjectStatus.PUBLISHED;
        }
    }
}
