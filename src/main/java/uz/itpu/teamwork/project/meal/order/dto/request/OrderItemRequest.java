package uz.itpu.teamwork.project.meal.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {

    private Long productId;

    private Integer quantity;

    // Selected modifiers → example: {"Size": "Large", "Sugar": "Low"}
    private Map<String, String> selectedModifiers;

    // Selected ingredient removals → example: ["Onion", "Cheese"]
    private List<String> removedIngredients;
}
