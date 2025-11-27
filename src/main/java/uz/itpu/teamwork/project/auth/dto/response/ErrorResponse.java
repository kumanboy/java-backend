package uz.itpu.teamwork.project.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success = false;
    private String error;
    private String message;
    private String code;
    private Map<String, String> details;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private String path;
}