package uz.itpu.teamwork.project.meal.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.itpu.teamwork.project.meal.product.model.ProductAvailability;

import java.util.List;
import java.util.Optional;

public interface ProductAvailabilityRepository extends JpaRepository<ProductAvailability, Long> {
    
    List<ProductAvailability> findByProductId(Long productId);
    
    @Query("SELECT pa FROM ProductAvailability pa WHERE pa.country.code = :countryCode AND pa.isAvailable = true")
    List<ProductAvailability> findAvailableByCountryCode(@Param("countryCode") String countryCode);
    
    Optional<ProductAvailability> findByProductIdAndCountryId(Long productId, Long countryId);
    
    void deleteByProductId(Long productId);
}