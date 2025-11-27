package uz.itpu.teamwork.project.auth.repository;

import uz.itpu.teamwork.project.auth.entity.Role;
import uz.itpu.teamwork.project.auth.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(UserRole name);

    boolean existsByName(UserRole name);
}