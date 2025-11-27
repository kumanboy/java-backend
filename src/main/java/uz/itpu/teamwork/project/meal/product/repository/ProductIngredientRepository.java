package uz.itpu.teamwork.project.meal.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.itpu.teamwork.project.meal.product.model.ProductIngredient;

import java.util.List;

public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, Long> {
    
    List<ProductIngredient> findByProductId(Long productId);
    
    void deleteByProductId(Long productId);
}