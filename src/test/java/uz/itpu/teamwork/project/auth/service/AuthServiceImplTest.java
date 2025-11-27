package uz.itpu.teamwork.project.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import uz.itpu.teamwork.project.auth.dto.request.LoginRequest;
import uz.itpu.teamwork.project.auth.dto.request.PasswordResetConfirmRequest;
import uz.itpu.teamwork.project.auth.dto.request.RefreshTokenRequest;
import uz.itpu.teamwork.project.auth.dto.request.RegisterRequest;
import uz.itpu.teamwork.project.auth.dto.response.AuthResponse;
import uz.itpu.teamwork.project.auth.entity.PasswordResetToken;
import uz.itpu.teamwork.project.auth.entity.RefreshToken;
import uz.itpu.teamwork.project.auth.entity.Role;
import uz.itpu.teamwork.project.auth.entity.User;
import uz.itpu.teamwork.project.auth.enums.UserRole;
import uz.itpu.teamwork.project.auth.repository.PasswordResetTokenRepository;
import uz.itpu.teamwork.project.auth.repository.RefreshTokenRepository;
import uz.itpu.teamwork.project.auth.repository.RoleRepository;
import uz.itpu.teamwork.project.auth.repository.UserRepository;
import uz.itpu.teamwork.project.auth.service.EmailService;
import uz.itpu.teamwork.project.auth.security.JwtTokenProvider;
import uz.itpu.teamwork.project.auth.service.impl.AuthServiceImpl;
import uz.itpu.teamwork.project.auth.validator.EmailValidator;
import uz.itpu.teamwork.project.auth.validator.PasswordValidator;
import uz.itpu.teamwork.project.exception.AuthException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private EmailService emailService;
    @Mock
    private EmailValidator emailValidator;
    @Mock
    private PasswordValidator passwordValidator;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "passwordResetTokenExpiration", 3_600_000L);
    }

    @Test
    void register_ShouldPersistUser_SendEmail_AndReturnTokens() {
        RegisterRequest request = new RegisterRequest(
                "NewUser@example.com",
                "Password1!",
                "Password1!",
                "John",
                "Doe",
                "+123456789"
        );
        Role customerRole = Role.builder()
                .id(10L)
                .name(UserRole.CUSTOMER)
                .build();

        when(emailValidator.isValid(request.getEmail())).thenReturn(true);
        when(userRepository.existsByEmail(request.getEmail().toLowerCase())).thenReturn(false);
        when(passwordValidator.validate(request.getPassword())).thenReturn(Collections.emptyList());
        when(roleRepository.findByName(UserRole.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        Authentication authentication = new UsernamePasswordAuthenticationToken("principal", "credentials");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.generateAccessToken(authentication)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(authentication)).thenReturn("refresh-token");
        when(tokenProvider.getAccessTokenExpiration()).thenReturn(3_600_000L);
        when(tokenProvider.getRefreshTokenExpiration()).thenReturn(7_200_000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo(request.getEmail().toLowerCase());

        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail(request.getEmail().toLowerCase(), request.getFirstName());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_WhenAuthenticationFails_ShouldIncrementAttemptsAndLockAccount() {
        LoginRequest request = new LoginRequest("user@test.com", "wrong");
        User user = User.builder()
                .id(5L)
                .email(request.getEmail().toLowerCase())
                .isActive(true)
                .failedLoginAttempts(4)
                .role(Role.builder().name(UserRole.CUSTOMER).build())
                .build();

        when(emailValidator.isValid(request.getEmail())).thenReturn(true);
        when(userRepository.findByEmail(request.getEmail().toLowerCase())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getAccountLockedUntil()).isNotNull();

        verify(userRepository).save(user);
    }

    @Test
    void refreshToken_WhenTokenInvalid_ShouldThrowAuthException() {
        RefreshTokenRequest request = new RefreshTokenRequest("expired");
        when(refreshTokenRepository.findValidToken(anyString(), any(LocalDateTime.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid or expired refresh token");

        verifyNoInteractions(tokenProvider);
    }

    @Test
    void confirmPasswordReset_ShouldUpdatePassword_AndRevokeTokens() {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
                "token-123",
                "NewPassword1!",
                "NewPassword1!"
        );

        User user = User.builder()
                .id(7L)
                .email("reset@test.com")
                .firstName("Jane")
                .failedLoginAttempts(3)
                .accountLockedUntil(LocalDateTime.now())
                .role(Role.builder().name(UserRole.CUSTOMER).build())
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(request.getToken())
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        when(passwordValidator.validate(request.getNewPassword())).thenReturn(Collections.emptyList());
        when(passwordResetTokenRepository.findValidToken(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("hashed");
        when(userRepository.save(user)).thenReturn(user);

        authService.confirmPasswordReset(request);

        assertThat(user.getPasswordHash()).isEqualTo("hashed");
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getAccountLockedUntil()).isNull();
        assertThat(resetToken.getUsed()).isTrue();

        verify(refreshTokenRepository).revokeAllUserTokens(user);
        verify(passwordResetTokenRepository).save(resetToken);
        verify(emailService).sendPasswordChangedEmail(user.getEmail(), user.getFirstName());
    }
}
