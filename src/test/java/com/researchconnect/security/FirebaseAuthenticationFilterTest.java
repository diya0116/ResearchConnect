package com.researchconnect.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class FirebaseAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void downstreamArgumentErrorsAreNotMisreportedAsInvalidTokens() throws Exception {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        when(auth.verifyIdToken("valid-token")).thenReturn(token);
        when(token.getClaims()).thenReturn(Map.of("email_verified", true));
        when(token.getEmail()).thenReturn("member@thapar.edu");
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FirebaseAuthenticationFilter filter = new FirebaseAuthenticationFilter();

        try (MockedStatic<FirebaseAuth> firebaseAuth = Mockito.mockStatic(FirebaseAuth.class)) {
            firebaseAuth.when(FirebaseAuth::getInstance).thenReturn(auth);

            assertThrows(IllegalArgumentException.class, () -> filter.doFilterInternal(
                    request,
                    response,
                    (servletRequest, servletResponse) -> {
                        throw new IllegalArgumentException("Invalid project request");
                    }));
        }

        assertEquals(200, response.getStatus());
        assertEquals("", response.getContentAsString());
    }

    @Test
    void unverifiedEmailIsRejectedWithJsonError() throws Exception {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        when(auth.verifyIdToken("invalid-token")).thenReturn(token);
        when(token.getClaims()).thenReturn(Map.of("email_verified", false));
        when(token.getEmail()).thenReturn("member@thapar.edu");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<FirebaseAuth> firebaseAuth = Mockito.mockStatic(FirebaseAuth.class)) {
            firebaseAuth.when(FirebaseAuth::getInstance).thenReturn(auth);
            new FirebaseAuthenticationFilter().doFilterInternal(
                    request("invalid-token"), response, (servletRequest, servletResponse) -> {
                        throw new AssertionError("Rejected token must not reach the application");
                    });
        }

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        assertEquals("{\"message\":\"Invalid or unauthorized Firebase token\"}", response.getContentAsString());
    }

    @Test
    void emailOutsideUniversityDomainIsRejected() throws Exception {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        when(auth.verifyIdToken("invalid-token")).thenReturn(token);
        when(token.getClaims()).thenReturn(Map.of("email_verified", true));
        when(token.getEmail()).thenReturn("member@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<FirebaseAuth> firebaseAuth = Mockito.mockStatic(FirebaseAuth.class)) {
            firebaseAuth.when(FirebaseAuth::getInstance).thenReturn(auth);
            new FirebaseAuthenticationFilter().doFilterInternal(
                    request("invalid-token"), response, (servletRequest, servletResponse) -> {
                        throw new AssertionError("Rejected token must not reach the application");
                    });
        }

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid or unauthorized Firebase token"));
    }

    private MockHttpServletRequest request() {
        return request("valid-token");
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }
}
