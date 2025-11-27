package uz.itpu.teamwork.project.meal.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uz.itpu.teamwork.project.auth.entity.User;
import uz.itpu.teamwork.project.meal.order.enums.FulfillmentMethod;
import uz.itpu.teamwork.project.meal.order.enums.OrderStatus;
import uz.itpu.teamwork.project.meal.order.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_id", columnList = "user_id"),
                @Index(name = "idx_orders_order_number", columnList = "order_number"),
                @Index(name = "idx_orders_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable order number, e.g. ORD-20250101-ABC12345
     */
    @Column(name = "order_number", nullable = false, unique = true, length = 64)
    private String orderNumber;

    /**
     * Customer who placed the order
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * NEW / IN_PROGRESS / COMPLETED / CANCELLED …
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    /**
     * PICKUP / DELIVERY / BOOKING (table)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_method", nullable = false, length = 32)
    private FulfillmentMethod fulfillmentMethod;

    /**
     * CARD / COD
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 32)
    private PaymentMethod paymentMethod;

    // ---- Monetary totals ----

    @Column(name = "items_subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal itemsSubtotal;      // sum of item subtotals

    @Column(name = "discount_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountAmount;     // overall discount

    @Column(name = "vat_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal vatAmount;          // VAT total

    @Column(name = "delivery_fee", precision = 12, scale = 2, nullable = false)
    private BigDecimal deliveryFee;        // delivery charge (0 for pickup / booking)

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;        // final total to pay

    // ---- Fulfillment details ----

    /**
     * When the customer wants the order (or booking) fulfilled.
     * For bookings this is the visit datetime; for pickup/delivery it can be the requested date/time.
     */
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    /**
     * ID of the chosen slot (e.g. "10-1030")
     */
    @Column(name = "time_slot_id", length = 64)
    private String timeSlotId;

    /**
     * Number of guests for table booking (nullable for pickup/delivery)
     */
    @Column(name = "guests")
    private Integer guests;

    /**
     * For pickup — which venue was selected
     */
    @Column(name = "pickup_venue_id", length = 64)
    private String pickupVenueId;

    /**
     * For delivery — zone id ("zone-1", "zone-2", …)
     */
    @Column(name = "delivery_zone_id", length = 64)
    private String deliveryZoneId;

    // ---- Delivery address ----

    @Column(name = "delivery_address_line1")
    private String deliveryAddressLine1;

    @Column(name = "delivery_address_line2")
    private String deliveryAddressLine2;

    @Column(name = "delivery_city", length = 100)
    private String deliveryCity;

    @Column(name = "delivery_state", length = 100)
    private String deliveryState;

    @Column(name = "delivery_zip", length = 20)
    private String deliveryZip;

    @Column(name = "delivery_instructions", length = 500)
    private String deliveryInstructions;

    // ---- Order origin / country ----

    /**
     * Country code where this order was placed from.
     * Example: "UZB", "KAZ", "GEO", "UKR", "CHN"
     */
    @Column(name = "country_code", length = 10)
    private String countryCode;

    // ---- Relations ----

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> items;

    // ---- Audit ----

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
