package uz.itpu.teamwork.project.meal.product.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private BigDecimal basePrice;
    private String imageUrl;
    private Boolean isActive;
    private BigDecimal rating;
    private Integer ratingCount;

    private List<IngredientInfo> ingredients;
    private Map<String, AvailabilityInfo> availability;
    private List<ModifierInfo> modifiers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @Builder
    public static class IngredientInfo {
        private Long id;
        private String name;
        private String quantity;
        private Boolean isAllergen;
    }

    @Getter
    @Setter
    @Builder
    public static class AvailabilityInfo {
        private Boolean isAvailable;
        private Integer stockQuantity;
        private String countryName;
    }

    @Getter
    @Setter
    @Builder
    public static class ModifierInfo {
        private Long id;
        private String name;
        private String description;
        private BigDecimal priceAdjustment;
        private Boolean isActive;
    }
}
