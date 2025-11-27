package uz.itpu.teamwork.project.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.itpu.teamwork.project.auth.security.CustomUserDetailsService;
import uz.itpu.teamwork.project.auth.security.JwtAuthenticationFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(
        classes = {
                SecurityConfigTest.TestApplication.class,
                SecurityConfig.class,
                CorsConfig.class,
                SecurityConfigTest.TestController.class
        }
)
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.frontend-url=http://localhost:3000")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void allowFilterChain() throws Exception {
        Mockito.doAnswer(invocation -> {
                    FilterChain chain = invocation.getArgument(2);
                    chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                    return null;
                })
                .when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any());
    }

    @Test
    void publicProductRead_ShouldAllowAnonymous() throws Exception {
        mockMvc.perform(get("/api/products/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("public-ok"));
    }

    @Test
    void productWrite_WithoutAuth_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void productWrite_WithManagerRole_ShouldAllow() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(user("manager").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("write-ok"));
    }

    @Test
    void productDelete_WithCustomerRole_ShouldBeForbidden() throws Exception {
        mockMvc.perform(delete("/api/products/1")
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_WithAdminRole_ShouldAllow() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("admin-ok"));
    }

    @Test
    void adminEndpoint_WithoutAuth_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isForbidden());
    }

    @RestController
    @RequestMapping("/api")
    static class TestController {

        @GetMapping("/products/test")
        public String publicProducts() {
            return "public-ok";
        }

        @PostMapping("/products")
        public String createProduct() {
            return "write-ok";
        }

        @DeleteMapping("/products/{id}")
        public String deleteProduct(@PathVariable Long id) {
            return "delete-ok";
        }

        @GetMapping("/admin/stats")
        public String adminStats() {
            return "admin-ok";
        }
    }

    @SpringBootApplication(
            exclude = {
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    FlywayAutoConfiguration.class,
                    MailSenderAutoConfiguration.class
            }
    )
    static class TestApplication {
    }
}
