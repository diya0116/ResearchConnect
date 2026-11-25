package com.researchconnect.service;

import com.google.firebase.auth.FirebaseToken;
import com.researchconnect.dto.ProfileRequest;
import com.researchconnect.entity.UserProfile;
import com.researchconnect.repository.FirestoreUserRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class UserService {

    private static final Pattern COHORT_PATTERN = Pattern.compile("(?i)(be|phd)(\\d{2})");
    private final FirestoreUserRepository userRepository;

    public UserService(FirestoreUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfile findByUid(String uid) {
        try {
            return userRepository.findByUid(uid);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to read the user profile", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Unable to read the user profile", exception);
        }
    }

    public UserProfile createProfile(FirebaseToken token, ProfileRequest request) {
        if (findByUid(token.getUid()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The profile has already been created");
        }
        String cohort = request.cohort();
        if (cohort == null || cohort.isBlank()) {
            cohort = extractCohort(token.getEmail());
        }
        Instant now = Instant.now();
        UserProfile profile = new UserProfile(
                token.getUid(),
                token.getEmail().toLowerCase(Locale.ROOT),
                token.getName(),
                token.getPicture(),
                request.role(),
                request.department().trim(),
                request.description() == null ? "" : request.description().trim(),
                cohort == null ? "" : cohort.toUpperCase(Locale.ROOT),
                now,
                now
        );
        try {
            userRepository.save(profile);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to save the user profile", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Unable to save the user profile", exception);
        }
        return profile;
    }

    private String extractCohort(String email) {
        if (email == null) {
            return "";
        }
        Matcher matcher = COHORT_PATTERN.matcher(email.substring(0, email.indexOf('@')));
        return matcher.find() ? (matcher.group(1) + matcher.group(2)).toUpperCase(Locale.ROOT) : "";
    }
}
