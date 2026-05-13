package org.connecthub.backend.Integration;

import org.connecthub.backend.dto.request.LoginRequest;
import org.connecthub.backend.dto.request.RegisterRequest;
import org.connecthub.backend.enums.UserStatus;
import org.connecthub.backend.model.User;
import org.connecthub.backend.repository.FriendshipRepository;
import org.connecthub.backend.repository.ContentRepository;
import org.connecthub.backend.repository.UserRepository;
import org.connecthub.backend.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController
 * Uses a real H2 in-memory database
 * Tests the full stack: HTTP → Security → Controller → Service → Repository → DB
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ContentRepository contentRepository;
    @Autowired
    private FriendshipRepository friendshipRepository;

    private static final String REGISTER_URL = "/auth/register";
    private static final String LOGIN_URL = "/auth/login";
    private static final String LOGOUT_URL = "/auth/logout";

    @BeforeEach
    void cleanDb() {
        friendshipRepository.deleteAll();
        contentRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser(String email, String username, String rawPassword) {
        return userRepository.save(User.builder()
                .email(email)
                .username(username)
                .hashedPassword(passwordEncoder.encode(rawPassword))
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .status(UserStatus.OFFLINE)
                .build());
    }

    private String tokenFor(User user) {
        return jwtUtil.generateToken(user.getEmail());
    }

    @Nested
    @DisplayName("POST " + REGISTER_URL)
    class RegistrationTests {

        @Test
        @DisplayName("TC-AC-01 | Register with valid data — 201 Created, returns UserDto")
        void register_validData_returns201() throws Exception {
            RegisterRequest req = new RegisterRequest(
                    "alice@example.com", "alice", "Password1!", LocalDate.of(2000, 5, 15));

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("alice@example.com"))
                    .andExpect(jsonPath("$.username").value("alice"))
                    .andExpect(jsonPath("$.userId").exists())
                    .andExpect(jsonPath("$.password").doesNotExist());   // never expose password

            assertThat(userRepository.findByEmail("alice@example.com")).isPresent();
        }

        @Test
        @DisplayName("TC-AC-02 | Register — password is BCrypt hashed in the database")
        void register_passwordIsHashedInDb() throws Exception {
            RegisterRequest req = new RegisterRequest(
                    "bob@example.com", "bob", "Password1!", LocalDate.of(1998, 3, 10));

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            User saved = userRepository.findByEmail("bob@example.com").orElseThrow();
            assertThat(saved.getHashedPassword()).startsWith("$2a$");          // BCrypt prefix
            assertThat(saved.getHashedPassword()).doesNotContain("Password1!");
        }

        @Test
        @DisplayName("TC-AC-03 | Register with duplicate email — 409 Conflict")
        void register_duplicateEmail_returns409() throws Exception {
            createUser("alice@example.com", "alice", "Password1!");

            RegisterRequest req = new RegisterRequest(
                    "alice@example.com", "alice2", "Password1!", LocalDate.of(2000, 1, 1));

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("TC-AC-04 | Register with invalid email format — 400 Bad Request")
        void register_invalidEmail_returns400() throws Exception {
            RegisterRequest req = new RegisterRequest(
                    "not-an-email", "alice", "Password1!", LocalDate.of(2000, 1, 1));

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.email").exists());
        }

        @Test
        @DisplayName("TC-AC-05 | Register with short password — 400 Bad Request")
        void register_shortPassword_returns400() throws Exception {
            RegisterRequest req = new RegisterRequest(
                    "alice@example.com", "alice", "short", LocalDate.of(2000, 1, 1));

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.password").exists());
        }

        @Test
        @DisplayName("TC-AC-06 | Register with missing fields — 400 Bad Request with field errors")
        void register_missingFields_returns400() throws Exception {
            String body = "{\"email\":\"\",\"username\":\"\",\"password\":\"\"}";

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors").exists());
        }

        @Test
        @DisplayName("TC-AC-07 | Register with future date of birth — 400 Bad Request")
        void register_futureDob_returns400() throws Exception {
            RegisterRequest req = new RegisterRequest(
                    "alice@example.com", "alice", "Password1!", LocalDate.now().plusDays(1));

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.dateOfBirth").exists());
        }
    }

    @Nested
    @DisplayName("POST " + LOGIN_URL)
    class LoginTests {
        @Test
        @DisplayName("TC-AC-08 | Login with correct credentials — 200, returns token and user")
        void login_correctCredentials_returns200WithToken() throws Exception {
            createUser("alice@example.com", "alice", "Password1!");

            LoginRequest req = new LoginRequest("alice@example.com", "Password1!");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.user.email").value("alice@example.com"))
                    .andExpect(jsonPath("$.user.status").value("ONLINE"))
                    .andExpect(jsonPath("$.user.password").doesNotExist());
        }

        @Test
        @DisplayName("TC-AC-09 | Login sets user status to ONLINE in the database")
        void login_setsStatusOnline() throws Exception {
            createUser("alice@example.com", "alice", "Password1!");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest("alice@example.com", "Password1!"))))
                    .andExpect(status().isOk());

            User user = userRepository.findByEmail("alice@example.com").orElseThrow();
            assertThat(user.getStatus()).isEqualTo(UserStatus.ONLINE);
        }

        @Test
        @DisplayName("TC-AC-10 | Login with wrong password — 401 Unauthorized")
        void login_wrongPassword_returns401() throws Exception {
            createUser("alice@example.com", "alice", "Password1!");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest("alice@example.com", "WrongPass!"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("TC-AC-11 | Login with non-existent email — 401 Unauthorized (no info leak)")
        void login_unknownEmail_returns401() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest("ghost@example.com", "Password1!"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("TC-AC-12 | Login with invalid email format — 400 Bad Request")
        void login_invalidEmailFormat_returns400() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest("not-an-email", "Password1!"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST " + LOGOUT_URL)
    class LogoutTests {
        @Test
        @DisplayName("TC-AC-13 | Logout with valid token — 204 No Content, status set to OFFLINE")
        void logout_validToken_returns204AndSetsOffline() throws Exception {
            User user = createUser("alice@example.com", "alice", "Password1!");
            user.setStatus(UserStatus.ONLINE);
            userRepository.save(user);
            String token = tokenFor(user);

            mockMvc.perform(post(LOGOUT_URL)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
            User updated = userRepository.findById(user.getUserId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(UserStatus.OFFLINE);
        }

        @Test
        @DisplayName("TC-AC-14 | Logout without token — 401 Unauthorized")
        void logout_noToken_returns401() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }
}