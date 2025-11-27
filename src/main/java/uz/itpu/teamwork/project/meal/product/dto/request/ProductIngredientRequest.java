package uz.itpu.teamwork.project.meal.product.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductIngredientRequest {

    @NotNull(message = "Ingredient ID is required")
    private Long ingredientId;

    private String quantity;
}