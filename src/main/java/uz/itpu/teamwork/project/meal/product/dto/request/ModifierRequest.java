package uz.itpu.teamwork.project.meal.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ModifierRequest {

    @NotBlank(message = "Modifier name is required")
    private String name;

    private String description;

    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    private Boolean isActive = true;
}