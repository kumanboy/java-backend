package uz.itpu.teamwork.project.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleValidationExceptions_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ValidationRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.path").value("/test/validate"));
    }

    @Test
    void handleResourceNotFound_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error").value("Resource Not Found"))
                .andExpect(jsonPath("$.path").value("/test/not-found"));
    }

    @Test
    void handleAuthException_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/test/auth-error"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_ERROR"))
                .andExpect(jsonPath("$.error").value("Authentication Error"))
                .andExpect(jsonPath("$.path").value("/test/auth-error"));
    }

    @Test
    void handleGlobalException_ShouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/test/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.path").value("/test/generic-error"));
    }

    
    @RestController
    @RequestMapping("/test")
    static class TestController {

        @PostMapping("/validate")
        public void triggerValidation(@Valid @RequestBody ValidationRequest request) {
        }

        @GetMapping("/not-found")
        public void triggerNotFound() {
            throw new ResourceNotFoundException("Resource not found");
        }

        @GetMapping("/auth-error")
        public void triggerAuthError() {
            throw new AuthException("Auth error");
        }

        @GetMapping("/generic-error")
        public void triggerGenericError() {
            throw new RuntimeException("Unexpected");
        }
    }

    record ValidationRequest(@NotBlank String name) { }
}
