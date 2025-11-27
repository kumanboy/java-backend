package uz.itpu.teamwork.project.auth.controller;

import uz.itpu.teamwork.project.auth.dto.request.LoginRequest;
import uz.itpu.teamwork.project.auth.dto.request.PasswordResetConfirmRequest;
import uz.itpu.teamwork.project.auth.dto.request.PasswordResetRequest;
import uz.itpu.teamwork.project.auth.dto.request.RefreshTokenRequest;
import uz.itpu.teamwork.project.auth.dto.request.RegisterRequest;
import uz.itpu.teamwork.project.auth.dto.response.ApiResponse;
import uz.itpu.teamwork.project.auth.dto.response.AuthResponse;
import uz.itpu.teamwork.project.auth.dto.response.UserResponse;
import uz.itpu.teamwork.project.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and Authorization endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new user account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpSession session) {
        log.info("Registration request received for email: {}", request.getEmail());

        AuthResponse authResponse = authService.register(request);

        if (session != null) {
            try {
                log.debug("Invalidating old session after registration");
                session.invalidate();
            } catch (IllegalStateException e) {
                log.warn("Session already invalidated: {}", e.getMessage());
            }
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(authResponse, "User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session) {
        log.info("Login request received for email: {}", request.getEmail());

        String oldSessionId;
        if (session != null) {
            oldSessionId = session.getId();
            log.debug("Old session ID before login: {}", oldSessionId);

            try {
                 session.invalidate();
                log.debug("Old session invalidated for email: {}", request.getEmail());
            } catch (IllegalStateException e) {
                log.warn("Session already invalidated: {}", e.getMessage());
            }
        }

        AuthResponse authResponse = authService.login(request);

        log.info("User logged in successfully: {}, new session will be created by container",
                request.getEmail());

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generate new access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");

        AuthResponse authResponse = authService.refreshToken(request);

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "User logout", description = "Revoke refresh token and clear session")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpSession session) {
        log.info("Logout request received");

        authService.logout(request.getRefreshToken());

        if (session != null) {
            try {
                log.debug("Invalidating session on logout");
                session.invalidate();
            } catch (IllegalStateException e) {
                log.warn("Session already invalidated: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    @PostMapping("/password-reset")
    @Operation(summary = "Request password reset", description = "Send password reset email")
    public ResponseEntity<ApiResponse<Void>> initiatePasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        log.info("Password reset request for email: {}", request.getEmail());

        authService.initiatePasswordReset(request);

        return ResponseEntity.ok(ApiResponse.success(
                null,
                "If the email exists, a password reset link has been sent"
        ));
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "Confirm password reset", description = "Reset password using token")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {
        log.info("Password reset confirmation request received");

        authService.confirmPasswordReset(request);

        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successful"));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user", description = "Get currently authenticated user details")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        log.info("Get current user request received");

        UserResponse userResponse = authService.getCurrentUser();

        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }
}