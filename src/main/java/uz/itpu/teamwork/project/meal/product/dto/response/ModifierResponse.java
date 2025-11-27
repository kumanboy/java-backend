package uz.itpu.teamwork.project.meal.product.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ModifierResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal priceAdjustment;
    private Boolean isActive;
    private LocalDateTime createdAt;
}