package org.connecthub.backend.service;

import org.connecthub.backend.dto.request.LoginRequest;
import org.connecthub.backend.dto.request.RegisterRequest;
import org.connecthub.backend.dto.response.LoginResponse;
import org.connecthub.backend.dto.response.UserDto;
import org.connecthub.backend.enums.UserStatus;
import org.connecthub.backend.exception.EmailAlreadyExistsException;
import org.connecthub.backend.exception.InvalidCredentialsException;
import org.connecthub.backend.exception.UsernameAlreadyExistsException;
import org.connecthub.backend.mapper.UserMapper;
import org.connecthub.backend.model.User;
import org.connecthub.backend.repository.UserRepository;
import org.connecthub.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    // Dependencies injected via constructor
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    // Register a new user
    @Transactional
    public UserDto register(RegisterRequest request) {

        // Check if email or username already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.email());
        }
        // Check if username already exists
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException("Username already exists: " + request.username());
        }

        // Create and save the new user
        User user = User.builder()
                .email(request.email())
                .username(request.username())
                .hashedPassword(passwordEncoder.encode(request.password()))
                .dateOfBirth(request.dateOfBirth())
                .status(UserStatus.OFFLINE)
                .build();

        User saved = userRepository.save(user);

        log.info("User registered: {}", saved.getEmail());
        
        // Convert to UserDto and return
        return userMapper.toUserDto(saved);
    }

    // Authenticate user and return token + user info
    @Transactional
    public LoginResponse login(LoginRequest request) {

        // Find user by email
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getHashedPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Update user status to ONLINE on successful login
        user.setStatus(UserStatus.ONLINE);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());

        log.info("User logged in: {}", user.getEmail());

        UserDto userDto = userMapper.toUserDto(user);

        // Return token and user info
        return new LoginResponse(token, userDto);
    }

    // Invalidate the user's token (logout) ********************
    @Transactional
    public void logout(String email) {

        userRepository.findByEmail(email).ifPresent(user -> {
            user.setStatus(UserStatus.OFFLINE);
            log.info("User logged out: {}", email);
        });
    }

    // Update user status (ONLINE, OFFLINE)
    @Transactional
    public void updateStatus(String email, UserStatus status) {

        userRepository.findByEmail(email).ifPresent(user -> {
            user.setStatus(status);
        });
    }

    // Get current authenticated user's info
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new InvalidCredentialsException("User not found"));

        return userMapper.toUserDto(user);
    }
}