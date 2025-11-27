package uz.itpu.teamwork.project.auth.service.impl;

import uz.itpu.teamwork.project.auth.dto.request.LoginRequest;
import uz.itpu.teamwork.project.auth.dto.request.PasswordResetConfirmRequest;
import uz.itpu.teamwork.project.auth.dto.request.PasswordResetRequest;
import uz.itpu.teamwork.project.auth.dto.request.RefreshTokenRequest;
import uz.itpu.teamwork.project.auth.dto.request.RegisterRequest;
import uz.itpu.teamwork.project.auth.dto.response.AuthResponse;
import uz.itpu.teamwork.project.auth.dto.response.UserResponse;
import uz.itpu.teamwork.project.auth.entity.PasswordResetToken;
import uz.itpu.teamwork.project.auth.entity.RefreshToken;
import uz.itpu.teamwork.project.auth.entity.Role;
import uz.itpu.teamwork.project.auth.entity.User;
import uz.itpu.teamwork.project.auth.enums.UserRole;
import uz.itpu.teamwork.project.exception.AuthException;
import uz.itpu.teamwork.project.exception.BadRequestException;
import uz.itpu.teamwork.project.exception.ResourceNotFoundException;
import uz.itpu.teamwork.project.auth.repository.PasswordResetTokenRepository;
import uz.itpu.teamwork.project.auth.repository.RefreshTokenRepository;
import uz.itpu.teamwork.project.auth.repository.RoleRepository;
import uz.itpu.teamwork.project.auth.repository.UserRepository;
import uz.itpu.teamwork.project.auth.security.JwtTokenProvider;
import uz.itpu.teamwork.project.auth.security.UserPrincipal;
import uz.itpu.teamwork.project.auth.service.AuthService;
import uz.itpu.teamwork.project.auth.service.EmailService;
import uz.itpu.teamwork.project.auth.validator.EmailValidator;
import uz.itpu.teamwork.project.auth.validator.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private final EmailValidator emailValidator;
    private final PasswordValidator passwordValidator;

    @Value("${app.password-reset.token-expiration}")
    private long passwordResetTokenExpiration;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_TIME_DURATION_MINUTES = 15;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Processing registration for email: {}", request.getEmail());

        // Validate email format
        if (!emailValidator.isValid(request.getEmail())) {
            throw new BadRequestException("Invalid email format");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new BadRequestException("Email address is already registered");
        }

        // Validate password
        List<String> passwordErrors = passwordValidator.validate(request.getPassword());
        if (!passwordErrors.isEmpty()) {
            throw new BadRequestException("Password validation failed: " + String.join(", ", passwordErrors));
        }

        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and confirmation password do not match");
        }

        // Get or create CUSTOMER role
        Role customerRole = roleRepository.findByName(UserRole.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", UserRole.CUSTOMER));

        // Create user
        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .role(customerRole)
                .isActive(true)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
        }

        // Authenticate and generate tokens
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        // Save refresh token
        saveRefreshToken(user, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpiration() / 1000) // in seconds
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Processing login for email: {}", request.getEmail());

        // Validate email format
        if (!emailValidator.isValid(request.getEmail())) {
            throw new BadRequestException("Invalid email format");
        }

        // Find user
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Check if account is locked
        if (user.isAccountLocked()) {
            log.warn("Login attempt for locked account: {}", user.getEmail());
            throw new LockedException("Account is locked due to multiple failed login attempts. Please try again later or reset your password.");
        }

        // Check if account is active
        if (!user.getIsActive()) {
            log.warn("Login attempt for inactive account: {}", user.getEmail());
            throw new DisabledException("Account is disabled. Please contact support.");
        }

        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Reset failed login attempts
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                user.setAccountLockedUntil(null);
                userRepository.save(user);
            }

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate tokens
            String accessToken = tokenProvider.generateAccessToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication);

            // Revoke old refresh tokens and save new one
            refreshTokenRepository.revokeAllUserTokens(user);
            saveRefreshToken(user, refreshToken);

            log.info("User logged in successfully: {}", user.getEmail());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(tokenProvider.getAccessTokenExpiration() / 1000)
                    .user(mapToUserResponse(user))
                    .build();

        } catch (BadCredentialsException e) {
            // Increment failed login attempts
            handleFailedLogin(user);
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Processing token refresh");

        // Validate refresh token
        RefreshToken refreshToken = refreshTokenRepository
                .findValidToken(request.getRefreshToken(), LocalDateTime.now())
                .orElseThrow(() -> new AuthException("Invalid or expired refresh token"));

        // Check if user is still active
        User user = refreshToken.getUser();
        if (!user.getIsActive()) {
            throw new AuthException("Account is disabled");
        }

        if (user.isAccountLocked()) {
            throw new AuthException("Account is locked");
        }

        // Generate new access token
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, userPrincipal.getAuthorities()
        );

        String newAccessToken = tokenProvider.generateAccessToken(authentication);

        log.info("Access token refreshed for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpiration() / 1000)
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        log.info("Processing logout");

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        SecurityContextHolder.clearContext();

        log.info("User logged out successfully");
    }

    @Override
    @Transactional
    public void initiatePasswordReset(PasswordResetRequest request) {
        log.info("Processing password reset request for email: {}", request.getEmail());

        // Validate email format
        if (!emailValidator.isValid(request.getEmail())) {
            // Don't reveal if email exists or not for security
            log.warn("Invalid email format for password reset: {}", request.getEmail());
            return;
        }

        // Find user (don't reveal if user doesn't exist)
        User user = userRepository.findByEmail(request.getEmail().toLowerCase()).orElse(null);

        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", request.getEmail());
            // Don't throw exception - don't reveal if email exists
            return;
        }

        // Generate reset token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(passwordResetTokenExpiration / 1000);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Send password reset email
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);
            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
            throw new AuthException("Failed to send password reset email. Please try again later.");
        }
    }

    @Override
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        log.info("Processing password reset confirmation");

        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and confirmation password do not match");
        }

        // Validate password strength
        List<String> passwordErrors = passwordValidator.validate(request.getNewPassword());
        if (!passwordErrors.isEmpty()) {
            throw new BadRequestException("Password validation failed: " + String.join(", ", passwordErrors));
        }

        // Find valid token
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findValidToken(request.getToken(), LocalDateTime.now())
                .orElseThrow(() -> new AuthException("Invalid or expired password reset token"));

        User user = resetToken.getUser();

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllUserTokens(user);

        log.info("Password reset successfully for user: {}", user.getEmail());

        // Send confirmation email
        try {
            emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            log.error("Failed to send password changed confirmation email", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException("No authenticated user found");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        return mapToUserResponse(user);
    }

    // Helper methods

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_DURATION_MINUTES));
            log.warn("Account locked due to failed login attempts: {}", user.getEmail());
        }

        userRepository.save(user);
        log.warn("Failed login attempt {} for user: {}", attempts, user.getEmail());
    }

    private void saveRefreshToken(User user, String token) {
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(tokenProvider.getRefreshTokenExpiration() / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole().getName())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }
}