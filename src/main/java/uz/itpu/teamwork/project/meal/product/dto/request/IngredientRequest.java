package uz.itpu.teamwork.project.meal.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IngredientRequest {

    @NotBlank(message = "Ingredient name is required")
    private String name;

    private Boolean isAllergen = false;
}