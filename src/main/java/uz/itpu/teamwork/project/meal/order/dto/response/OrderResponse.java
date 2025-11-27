package uz.itpu.teamwork.project.meal.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.itpu.teamwork.project.meal.order.enums.FulfillmentMethod;
import uz.itpu.teamwork.project.meal.order.enums.OrderStatus;
import uz.itpu.teamwork.project.meal.order.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;

    private String orderNumber;

    private OrderStatus status;
    private FulfillmentMethod fulfillmentMethod;
    private PaymentMethod paymentMethod;

    private BigDecimal itemsSubtotal;
    private BigDecimal discountAmount;
    private BigDecimal vatAmount;
    private BigDecimal deliveryFee;

    private BigDecimal totalAmount;

    private LocalDateTime orderDate;
    private String timeSlotId;
    private Integer guests;

    private String pickupVenueId;

    private String deliveryZoneId;
    private String deliveryAddressLine1;
    private String deliveryAddressLine2;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryZip;
    private String deliveryInstructions;

    private LocalDateTime createdAt;

    private List<OrderItemResponse> items;
}
