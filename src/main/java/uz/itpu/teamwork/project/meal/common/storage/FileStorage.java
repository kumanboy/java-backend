package uz.itpu.teamwork.project.meal.common.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;


public interface FileStorage {
    String uploadFile(MultipartFile file, String folder) throws IOException;

    void deleteFile(String fileUrl) throws IOException;

    String getPublicUrl(String filePath);
}