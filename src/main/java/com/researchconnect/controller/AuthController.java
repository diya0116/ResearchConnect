package com.researchconnect.controller;

import com.researchconnect.dto.ProfileRequest;
import com.researchconnect.entity.UserProfile;
import com.researchconnect.security.FirebaseUserPrincipal;
import com.researchconnect.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser(@AuthenticationPrincipal FirebaseUserPrincipal principal) {
        UserProfile profile = userService.findByUid(principal.getUsername());
        if (profile == null) {
            return ResponseEntity.ok(Map.of(
                    "profileComplete", false,
                    "email", principal.token().getEmail(),
                    "fullName", principal.token().getName() == null ? "" : principal.token().getName()
            ));
        }
        return ResponseEntity.ok(Map.of("profileComplete", true, "profile", profile));
    }

    @PostMapping("/profile")
    public ResponseEntity<UserProfile> createProfile(
            @AuthenticationPrincipal FirebaseUserPrincipal principal,
            @Valid @RequestBody ProfileRequest request
    ) {
        return ResponseEntity.ok(userService.createProfile(principal.token(), request));
    }
}
