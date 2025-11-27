package uz.itpu.teamwork.project.meal.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.itpu.teamwork.project.meal.product.model.Modifier;

import java.util.List;

public interface ModifierRepository extends JpaRepository<Modifier, Long> {
    
    List<Modifier> findByIsActiveTrue();
}