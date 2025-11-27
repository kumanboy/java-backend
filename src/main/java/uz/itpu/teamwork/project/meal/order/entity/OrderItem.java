package uz.itpu.teamwork.project.meal.order.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.itpu.teamwork.project.meal.product.model.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_items_order_id", columnList = "order_id"),
                @Index(name = "idx_order_items_product_id", columnList = "product_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // relation to Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // relation to Product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // quantity of this product
    @Column(nullable = false)
    private Integer quantity;

    // copy product name at the time of order (safe for history)
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    // copy product base price at the moment of purchase
    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    // final price * quantity
    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;

    // --- Modifiers and ingredient removals ---

    // Map<String, String> e.g. {"Size":"Large","Sugar":"Low"}
    @ElementCollection
    @CollectionTable(
            name = "order_item_modifiers",
            joinColumns = @JoinColumn(name = "order_item_id")
    )
    @MapKeyColumn(name = "modifier_name")
    @Column(name = "modifier_value")
    private Map<String, String> selectedModifiers;

    // List<String> — removed ingredients ["Onion","Cheese"]
    @ElementCollection
    @CollectionTable(
            name = "order_item_removed_ingredients",
            joinColumns = @JoinColumn(name = "order_item_id")
    )
    @Column(name = "ingredient_name")
    private List<String> removedIngredients;
}
