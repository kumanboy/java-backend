package uz.itpu.teamwork.project.exception;

public class ForbiddenException extends BaseException {
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN");
    }
}