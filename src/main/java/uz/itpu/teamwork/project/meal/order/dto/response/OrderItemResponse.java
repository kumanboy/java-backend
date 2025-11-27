package uz.itpu.teamwork.project.meal.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long id;

    private Long productId;
    private String productName;

    private Integer quantity;

    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    // Map<String, String> e.g. {"Size":"Large","Sugar":"Low"}
    private Map<String, String> selectedModifiers;

    // List<String> — removed ingredients ["Onion","Cheese"]
    private List<String> removedIngredients;
}
