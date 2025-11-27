package uz.itpu.teamwork.project.meal.common.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uz.itpu.teamwork.project.exception.FileUploadException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Profile({"dev", "local", "default"})
@Slf4j
public class LocalFileStorage implements FileStorage {

    @Value("${app.storage.local.base-path:uploads}")
    private String basePath;

    @Value("${app.storage.local.base-url:http://localhost:8080/uploads}")
    private String baseUrl;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"};

    @PostConstruct
    public void init() throws IOException {
        Path baseDir = Paths.get(basePath);
        if (!Files.exists(baseDir)) {
            Files.createDirectories(baseDir);
            log.info("Created base upload directory: {}", baseDir.toAbsolutePath());
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        log.info("Uploading file to local storage: {}/{}", folder, file.getOriginalFilename());
        
        validateFile(file);

        Path uploadPath = Paths.get(basePath, folder);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String relativePath = folder + "/" + uniqueFilename;
        log.info("File uploaded successfully: {}", relativePath);
        
        return getPublicUrl(relativePath);
    }

    @Override
    public void deleteFile(String fileUrl) throws IOException {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        String relativePath = fileUrl.replace(baseUrl + "/", "");
        Path filePath = Paths.get(basePath, relativePath);

        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("File deleted: {}", relativePath);
        }
    }

    @Override
    public String getPublicUrl(String filePath) {
        return baseUrl + "/" + filePath;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileUploadException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException("File size exceeds maximum allowed size (5MB)");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new FileUploadException("Invalid file name");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        boolean isAllowed = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (extension.equals(allowedExt)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            throw new FileUploadException("File type not allowed. Allowed types: jpg, jpeg, png, webp");
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }
}