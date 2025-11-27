package uz.itpu.teamwork.project.exception;

public class ResourceAlreadyExistsException extends BaseException {
    public ResourceAlreadyExistsException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue), "RESOURCE_ALREADY_EXISTS");
    }
}