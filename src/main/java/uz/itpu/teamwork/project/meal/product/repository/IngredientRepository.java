package uz.itpu.teamwork.project.meal.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.itpu.teamwork.project.meal.product.model.Ingredient;

import java.util.List;
import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    
    Optional<Ingredient> findByName(String name);
    
    List<Ingredient> findByIsAllergenTrue();
    
    boolean existsByName(String name);
}