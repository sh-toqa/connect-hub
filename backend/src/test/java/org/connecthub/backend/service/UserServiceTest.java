package org.connecthub.backend.service;
import org.connecthub.backend.dto.request.LoginRequest;
import org.connecthub.backend.dto.request.RegisterRequest;
import org.connecthub.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.connecthub.backend.dto.response.LoginResponse;
import org.connecthub.backend.dto.response.UserDto;
import org.connecthub.backend.enums.UserStatus;
import org.connecthub.backend.exception.EmailAlreadyExistsException;
import org.connecthub.backend.exception.InvalidCredentialsException;
import org.connecthub.backend.model.User;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import org.connecthub.backend.mapper.UserMapper;
import org.connecthub.backend.security.JwtUtil;
import java.util.Optional;
import java.util.UUID;



@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    // A reusable valid registration request
    private RegisterRequest validRequest;



    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest(
                "alice@example.com",
                "alice99",
                "Secret123!",
                LocalDate.of(2000, 5, 15)
        );

    }

    @Test
    @DisplayName("Valid registration — saves user and returns DTO")
    void register_withValidData_savesUserAndReturnsDto() {
        // Arrange
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("$2a$10$hashedpw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        when(userMapper.toUserDto(any(User.class)))
                .thenReturn(new UserDto(
                        UUID.randomUUID(),
                        "alice99",
                        "alice@example.com",
                        null,
                        null,
                        null,
                        UserStatus.OFFLINE
                ));
        // Act
        UserDto result = userService.register(validRequest);

        // Assert
        assertThat(result.email()).isEqualTo("alice@example.com");
        assertThat(result.username()).isEqualTo("alice99");

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("Secret123!");
    }

    @Test
    @DisplayName("Duplicate email — throws exception")
    void register_withDuplicateEmail_throwsEmailAlreadyExistsException() {
        // Arrange
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.register(validRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("alice@example.com");

        verify(userRepository, never()).save(any());   // nothing was saved
    }

    @Test
    @DisplayName("Password is hashed before storage")
    void register_passwordIsHashedBeforeStorage() {
        // Arrange
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("$2a$10$hashedpw");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        userService.register(validRequest);

        // Assert
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getHashedPassword()).isEqualTo("$2a$10$hashedpw");
        assertThat(savedUser.getHashedPassword()).doesNotContain("Secret123!");
    }

    @Test
    @DisplayName("Correct credentials — returns JWT")
    void login_withCorrectCredentials_returnsToken() {
        // Arrange
        User user = User.builder()
                .email("alice@example.com")
                .hashedPassword("$2a$10$hashedpw")
                .status(UserStatus.OFFLINE)
                .build();

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("Secret123!", "$2a$10$hashedpw"))
                .thenReturn(true);

        when(jwtUtil.generateToken("alice@example.com"))
                .thenReturn("fake-token");

        when(userMapper.toUserDto(any(User.class)))
                .thenReturn(new UserDto(
                        UUID.randomUUID(),
                        "alice99",
                        "alice@example.com",
                        null,
                        null,
                        null,
                        UserStatus.OFFLINE
                ));

        LoginRequest request = new LoginRequest(
                "alice@example.com",
                "Secret123!"
        );

        // Act
        LoginResponse response = userService.login(request);

        // Assert
        assertThat(response.token()).isEqualTo("fake-token");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ONLINE);
    }

    @Test
    @DisplayName("Wrong password — throws exception")
    void login_withWrongPassword_throwsInvalidCredentialsException() {
        // Arrange
        User user = User.builder().hashedPassword("$2a$10$hashedpw").build();
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass!", "$2a$10$hashedpw")).thenReturn(false);

        // Act & Assert
        LoginRequest request = new LoginRequest(
                "alice@example.com",
                "WrongPass!"
        );
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Non-existent email — throws InvalidCredentialsException")
    void login_withNonExistentEmail_throwsInvalidCredentialsException() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        LoginRequest request = new LoginRequest(
                "ghost@example.com",
                "anything"
        );

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // Test cases for UserService.logout()
    @Test
    @DisplayName("Logout sets user status to OFFLINE")
    void logout_setsUserStatusToOffline() {
        // Arrange
        String email = "alice@example.com";

        User user = User.builder()
                .email(email)
                .status(UserStatus.ONLINE)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        userService.logout(email);

        // Assert
        assertThat(user.getStatus()).isEqualTo(UserStatus.OFFLINE);

    }

    
}

