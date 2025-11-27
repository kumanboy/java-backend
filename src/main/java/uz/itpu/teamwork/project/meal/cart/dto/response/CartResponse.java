package uz.itpu.teamwork.project.meal.cart.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    
    private Long id;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    private List<CartItemResponse> items;
    
    @JsonProperty("total_items")
    private Integer totalItems;
    
    @JsonProperty("total_price")
    private BigDecimal totalPrice;
}