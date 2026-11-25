package com.researchconnect.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final Pattern THAPAR_EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@thapar\\.edu$", Pattern.CASE_INSENSITIVE);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = authorization.substring("Bearer ".length()).trim();
        try {
            FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);
            validateEmail(token);
            var authentication = new UsernamePasswordAuthenticationToken(
                    new FirebaseUserPrincipal(token),
                    null,
                    new FirebaseUserPrincipal(token).getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (FirebaseAuthException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or unauthorized Firebase token");
        }
    }

    private void validateEmail(FirebaseToken token) {
        Object verified = token.getClaims().get("email_verified");
        String email = token.getEmail();
        if (!Boolean.TRUE.equals(verified)
                || email == null
                || !THAPAR_EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Only verified thapar.edu accounts are allowed");
        }
    }
}
