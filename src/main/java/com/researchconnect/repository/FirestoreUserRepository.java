package com.researchconnect.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.researchconnect.entity.UserProfile;
import com.researchconnect.enums.UserRole;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.springframework.stereotype.Repository;

@Repository
public class FirestoreUserRepository {

    private static final String COLLECTION = "users";
    private final Firestore firestore;

    public FirestoreUserRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public UserProfile findByUid(String uid) throws ExecutionException, InterruptedException {
        ApiFuture<DocumentSnapshot> future = firestore.collection(COLLECTION).document(uid).get();
        DocumentSnapshot document = future.get();
        if (!document.exists()) {
            return null;
        }
        return new UserProfile(
                document.getString("uid"),
                document.getString("email"),
                document.getString("fullName"),
                document.getString("profileImageUrl"),
                UserRole.valueOf(document.getString("role")),
                document.getString("department"),
                document.getString("description"),
                document.getString("cohort"),
                Instant.parse(document.getString("registrationDate")),
                Instant.parse(document.getString("updatedAt"))
        );
    }

    public void save(UserProfile profile) throws ExecutionException, InterruptedException {
        Map<String, Object> values = new java.util.HashMap<>();
        values.put("uid", profile.uid());
        values.put("email", profile.email());
        values.put("fullName", profile.fullName());
        values.put("profileImageUrl", profile.profileImageUrl());
        values.put("role", profile.role().name());
        values.put("department", profile.department());
        values.put("description", profile.description());
        values.put("cohort", profile.cohort());
        values.put("registrationDate", profile.registrationDate().toString());
        values.put("updatedAt", profile.updatedAt().toString());
        Future<?> write = firestore.collection(COLLECTION)
                .document(profile.uid())
                .set(values, SetOptions.merge());
        write.get();
    }
}
