package uz.itpu.teamwork.project.auth.service;

import uz.itpu.teamwork.project.auth.dto.response.AuthResponse;
import uz.itpu.teamwork.project.auth.dto.response.UserResponse;
import uz.itpu.teamwork.project.auth.dto.request.LoginRequest;
import uz.itpu.teamwork.project.auth.dto.request.PasswordResetConfirmRequest;
import uz.itpu.teamwork.project.auth.dto.request.PasswordResetRequest;
import uz.itpu.teamwork.project.auth.dto.request.RefreshTokenRequest;
import uz.itpu.teamwork.project.auth.dto.request.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken);

    void initiatePasswordReset(PasswordResetRequest request);

    void confirmPasswordReset(PasswordResetConfirmRequest request);

    UserResponse getCurrentUser();
}