package uz.itpu.teamwork.project.meal.product.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class IngredientResponse {
    private Long id;
    private String name;
    private Boolean isAllergen;
    private LocalDateTime createdAt;
}