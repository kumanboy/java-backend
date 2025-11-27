package uz.itpu.teamwork.project.exception;

public class InvalidOperationException extends BaseException {
    public InvalidOperationException(String message) {
        super(message, "INVALID_OPERATION");
    }
}