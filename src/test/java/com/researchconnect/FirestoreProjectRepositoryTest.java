package com.researchconnect;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QuerySnapshot;
import com.researchconnect.repository.FirestoreProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FirestoreProjectRepositoryTest {

    @Mock
    private Firestore firestore;

    @Mock
    private CollectionReference projectsCollection;

    @Mock
    private ApiFuture<QuerySnapshot> queryFuture;

    @Mock
    private QuerySnapshot querySnapshot;

    @Mock
    private FirestoreOptions firestoreOptions;

    private FirestoreProjectRepository projectRepository;

    @BeforeEach
    void setUp() {
        projectRepository = new FirestoreProjectRepository(firestore);
    }

    @Test
    void readsProjectsCollectionWithoutCreatingDocuments() throws Exception {
        when(firestore.collection("projects")).thenReturn(projectsCollection);
        when(projectsCollection.get()).thenReturn(queryFuture);
        when(queryFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(java.util.List.of());
        when(firestore.getOptions()).thenReturn(firestoreOptions);
        when(firestoreOptions.getProjectId()).thenReturn("test-project");

        assertTrue(projectRepository.findAll().isEmpty());
        verify(projectsCollection).get();
    }
}
