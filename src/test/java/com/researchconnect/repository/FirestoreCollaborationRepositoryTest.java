package com.researchconnect.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import com.researchconnect.entity.ProjectApplication;
import com.researchconnect.entity.UserProfile;
import com.researchconnect.enums.ApplicationStatus;
import com.researchconnect.enums.UserRole;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FirestoreCollaborationRepositoryTest {

    @Mock
    private Firestore firestore;

    @Mock
    private Transaction transaction;

    @Mock
    private CollectionReference applications;

    @Mock
    private CollectionReference projects;

    @Mock
    private CollectionReference memberships;

    @Mock
    private CollectionReference notifications;

    @Mock
    private DocumentReference applicationReference;

    @Mock
    private DocumentReference projectReference;

    @Mock
    private DocumentReference memberReference;

    @Mock
    private DocumentReference notificationReference;

    @Mock
    private DocumentSnapshot applicationSnapshot;

    @Mock
    private DocumentSnapshot projectSnapshot;

    @Mock
    private DocumentSnapshot memberSnapshot;

    private FirestoreCollaborationRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        repository = new FirestoreCollaborationRepository(firestore);
        when(firestore.collection("project_applications")).thenReturn(applications);
        when(firestore.collection("projects")).thenReturn(projects);
        when(applications.document(anyString())).thenReturn(applicationReference);
        when(projects.document("project-id")).thenReturn(projectReference);
        when(firestore.collection("notifications")).thenReturn(notifications);
        when(notifications.document()).thenReturn(notificationReference);
        when(transaction.get(applicationReference))
                .thenReturn(ApiFutures.immediateFuture(applicationSnapshot));
        when(transaction.get(projectReference))
                .thenReturn(ApiFutures.immediateFuture(projectSnapshot));
        when(projectSnapshot.exists()).thenReturn(true);
        when(projectSnapshot.getString("ownerId")).thenReturn("owner-a");
        when(firestore.<ProjectApplication>runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<ProjectApplication> function = invocation.getArgument(0);
            return ApiFutures.immediateFuture(function.updateCallback(transaction));
        });
    }

    @Test
    void anotherUserCannotDecideAnOwnersApplicationInsideTransaction() {
        stubPendingApplication();

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> repository.decideApplication(
                        "application-id", "owner-b", ApplicationStatus.ACCEPTED));

        assertEquals(403, error.getStatusCode().value());
        verify(transaction, never()).set(any(DocumentReference.class), any());
        verify(transaction, never()).update(any(DocumentReference.class), anyMap());
    }

    @Test
    void acceptingApplicationAddsMembershipAndDecrementsAvailablePositions() throws Exception {
        stubPendingApplication();
        stubMembershipReferences();
        stubMembershipSnapshot();
        when(projectSnapshot.getString("title")).thenReturn("Project title");
        when(projectSnapshot.getLong("availablePositions")).thenReturn(1L);
        when(applicationSnapshot.getData()).thenReturn(java.util.Map.of(
                "projectId", "project-id",
                "applicantId", "applicant-id",
                "applicantName", "Applicant",
                "applicantEmail", "applicant@thapar.edu",
                "applicantRole", "STUDENT",
                "applicantDepartment", "Computer Science",
                "status", "PENDING",
                "appliedAt", "2026-01-01T00:00:00Z",
                "updatedAt", "2026-01-01T00:00:00Z"
        ));
        ProjectApplication result = repository.decideApplication(
                "application-id", "owner-a", ApplicationStatus.ACCEPTED);

        assertEquals(ApplicationStatus.ACCEPTED, result.status());
        verify(transaction).set(eq(memberReference), anyMap());
        verify(transaction).set(eq(notificationReference), argThat(values ->
                "applicant-id".equals(values.get("recipientId"))
                        && Boolean.FALSE.equals(values.get("read"))
                        && "Your join request to Project title was accepted.".equals(values.get("message"))));
        verify(transaction).update(eq(projectReference), argThat(values ->
                Long.valueOf(0).equals(values.get("availablePositions"))));
    }

    @Test
    void rejectingApplicationWritesApplicantNotificationInSameTransaction() throws Exception {
        stubPendingApplication();
        stubMembershipReferences();
        when(projectSnapshot.getString("title")).thenReturn("Project title");
        when(applicationSnapshot.getData()).thenReturn(java.util.Map.of(
                "projectId", "project-id",
                "applicantId", "applicant-id",
                "applicantName", "Applicant",
                "applicantEmail", "applicant@thapar.edu",
                "applicantRole", "STUDENT",
                "applicantDepartment", "Computer Science",
                "status", "PENDING",
                "appliedAt", "2026-01-01T00:00:00Z",
                "updatedAt", "2026-01-01T00:00:00Z"
        ));

        ProjectApplication result = repository.decideApplication(
                "application-id", "owner-a", ApplicationStatus.REJECTED);

        assertEquals(ApplicationStatus.REJECTED, result.status());
        verify(transaction).set(eq(notificationReference), argThat(values ->
                "applicant-id".equals(values.get("recipientId"))
                        && "Your join request to Project title was rejected.".equals(values.get("message"))));
    }

    @Test
    void submittingApplicationWritesOwnerNotificationInSameTransaction() throws Exception {
        when(applicationSnapshot.exists()).thenReturn(false);
        when(projectSnapshot.getString("status")).thenReturn("PUBLISHED");
        when(projectSnapshot.getString("title")).thenReturn("Project title");
        when(projectSnapshot.getLong("availablePositions")).thenReturn(2L);
        stubMembershipReferences();
        stubMembershipSnapshot();
        when(memberSnapshot.exists()).thenReturn(false);
        UserProfile applicant = new UserProfile(
                "applicant-id", "applicant@thapar.edu", "Applicant", "", UserRole.STUDENT,
                "Computer Science", "", "", Instant.EPOCH, Instant.EPOCH);

        ProjectApplication result = repository.submitApplication("project-id", applicant);

        assertEquals(ApplicationStatus.PENDING, result.status());
        verify(transaction).set(eq(notificationReference), argThat(values ->
                "owner-a".equals(values.get("recipientId"))
                        && "Applicant requested to join Project title.".equals(values.get("message"))
                        && Boolean.FALSE.equals(values.get("read"))));
    }

    private void stubPendingApplication() {
        when(applicationSnapshot.exists()).thenReturn(true);
        when(applicationSnapshot.getString("status")).thenReturn("PENDING");
        when(applicationSnapshot.getString("projectId")).thenReturn("project-id");
        when(applicationSnapshot.getString("applicantId")).thenReturn("applicant-id");
    }

    private void stubMembershipReferences() {
        when(firestore.collection("project_members")).thenReturn(memberships);
        when(memberships.document(anyString())).thenReturn(memberReference);
    }

    private void stubMembershipSnapshot() {
        when(transaction.get(memberReference))
                .thenReturn(ApiFutures.immediateFuture(memberSnapshot));
    }
}
