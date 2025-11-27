package uz.itpu.teamwork.project.config;

import uz.itpu.teamwork.project.auth.security.CustomUserDetailsService;
import uz.itpu.teamwork.project.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configure(http))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // -------------------------
                        // PUBLIC AUTH ENDPOINTS
                        // -------------------------
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/password-reset",
                                "/api/auth/password-reset/confirm"
                        ).permitAll()

                        // -------------------------
                        // SWAGGER
                        // -------------------------
                        .requestMatchers(
                                "/api/docs/**",
                                "/api/swagger-ui/**",
                                "/api/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // -------------------------
                        // ADMIN ONLY
                        // -------------------------
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/categories/**").hasRole("ADMIN")
                        .requestMatchers("/api/ingredients/**").hasRole("ADMIN")
                        .requestMatchers("/api/modifiers/**").hasRole("ADMIN")

                        // -------------------------
                        // PRODUCT CRUD (Admin + Manager)
                        // -------------------------
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        // Product images
                        .requestMatchers(HttpMethod.POST, "/api/products/*/image").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/*/image").hasAnyRole("ADMIN", "MANAGER")

                        // -------------------------
                        // PUBLIC CATALOG (GET ONLY)
                        // -------------------------
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/ingredients/**",
                                "/api/modifiers/**"
                        ).permitAll()

                        // -------------------------
                        // ORDERS (CART + CHECKOUT)
                        // -------------------------
                        // Create order - only CUSTOMER
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")

                        // View orders (my orders) - any authenticated user
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").authenticated()

                        // -------------------------
                        // SALES REPORTS (Admin + Manager)
                        // -------------------------
                        .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "MANAGER")

                        // -------------------------
                        // ALL OTHER ENDPOINTS
                        // -------------------------
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
