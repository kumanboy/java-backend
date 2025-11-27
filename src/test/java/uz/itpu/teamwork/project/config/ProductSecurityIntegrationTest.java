package uz.itpu.teamwork.project.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import uz.itpu.teamwork.project.auth.security.CustomUserDetailsService;
import uz.itpu.teamwork.project.auth.security.JwtAuthenticationFilter;
import uz.itpu.teamwork.project.meal.product.controller.ProductController;
import uz.itpu.teamwork.project.meal.product.dto.request.ProductRequest;
import uz.itpu.teamwork.project.meal.product.dto.response.ProductResponse;
import uz.itpu.teamwork.project.meal.product.service.ProductService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        ProductSecurityIntegrationTest.TestApplication.class,
        ProductController.class,
        SecurityConfig.class,
        CorsConfig.class
})
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.frontend-url=http://localhost:3000")
class ProductSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setup() throws Exception {
        // Let requests through the JWT filter; MockMvc supplies the user via .with(user(...))
        Mockito.doAnswer(invocation -> {
                    FilterChain chain = invocation.getArgument(2);
                    chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                    return null;
                })
                .when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any());
    }

    @Test
    void getProducts_ShouldBePublic() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(
                ProductResponse.builder().id(1L).name("Coffee").basePrice(new BigDecimal("2.50")).build()
        ));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createProduct_AsManager_ShouldSucceed() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("Latte");
        request.setBasePrice(new BigDecimal("4.00"));
        request.setIsActive(true);

        ProductResponse response = ProductResponse.builder()
                .id(10L)
                .name(request.getName())
                .basePrice(request.getBasePrice())
                .isActive(true)
                .build();

        when(productService.createProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .with(SecurityMockMvcRequestPostProcessors.user("manager").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Latte"));
    }

    @Test
    void createProduct_WithoutAuth_ShouldBeForbidden() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("Mocha");
        request.setBasePrice(new BigDecimal("5.00"));
        request.setIsActive(true);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProduct_AsAdmin_ShouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/products/5")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProduct_AsCustomer_ShouldBeForbidden() throws Exception {
        mockMvc.perform(delete("/api/products/5")
                        .with(SecurityMockMvcRequestPostProcessors.user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
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
