package uz.itpu.teamwork.project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import uz.itpu.teamwork.project.auth.entity.Role;
import uz.itpu.teamwork.project.auth.enums.UserRole;
import uz.itpu.teamwork.project.auth.repository.RoleRepository;

@Slf4j
@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Initialize default roles
     */
    @Bean
    public CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            for (UserRole userRole : UserRole.values()) {
                if (!roleRepository.existsByName(userRole)) {
                    Role role = Role.builder()
                            .name(userRole)
                            .description(userRole.getDescription())
                            .build();
                    roleRepository.save(role);
                    log.info("Created role: {}", userRole);
                }
            }
            log.info("Role initialization completed");
        };
    }
}
