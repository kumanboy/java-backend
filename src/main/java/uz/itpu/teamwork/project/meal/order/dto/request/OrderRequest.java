package uz.itpu.teamwork.project.meal.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.itpu.teamwork.project.meal.order.enums.FulfillmentMethod;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {

    private FulfillmentMethod fulfillmentMethod;  // DELIVERY / PICKUP / DINE_IN

    private String deliveryAddress;               // Only required if DELIVERY

    private String customerNote;                  // Optional note for kitchen

    private List<OrderItemRequest> items;         // The cart items
}
