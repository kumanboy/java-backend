package uz.itpu.teamwork.project.auth.service;

public interface EmailService {

    void sendWelcomeEmail(String to, String firstName);

    void sendPasswordResetEmail(String to, String firstName, String resetToken);

    void sendPasswordChangedEmail(String to, String firstName);
}