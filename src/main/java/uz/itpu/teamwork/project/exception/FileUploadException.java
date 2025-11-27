package uz.itpu.teamwork.project.exception;

public class FileUploadException extends BaseException {
    public FileUploadException(String message) {
        super(message, "FILE_UPLOAD_ERROR");
    }
    
    public FileUploadException(String message, Throwable cause) {
        super(message, "FILE_UPLOAD_ERROR", cause);
    }
}