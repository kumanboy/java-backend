package uz.itpu.teamwork.project.meal.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.itpu.teamwork.project.meal.order.enums.FulfillmentMethod;
import uz.itpu.teamwork.project.meal.order.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    private Long userId;

    // MUST BE ENUMS (NOT STRING)
    private FulfillmentMethod fulfillmentMethod;
    private PaymentMethod paymentMethod;

    private BigDecimal discount;
    private BigDecimal vatAmount;
    private BigDecimal deliveryFee;

    // MUST BE LocalDate (NOT String)
    private LocalDate orderDate;

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

    private String countryCode;

    private List<OrderItemRequest> items;
}
