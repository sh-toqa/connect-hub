package org.connecthub.backend.controller;

import org.connecthub.backend.dto.request.LoginRequest;
import org.connecthub.backend.dto.request.RegisterRequest;
import org.connecthub.backend.dto.response.LoginResponse;
import org.connecthub.backend.dto.response.UserDto;
import org.connecthub.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor

public class AuthController {

    private final UserService userService;

    // Register a new user
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.register(request));
    }

    // Authenticate user and return token + user info
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }


    // Invalidate the user's token (logout)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        userService.logout(userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    // Get current authenticated user's info
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                userService.getCurrentUser(userDetails.getUsername())
        );
    }
}