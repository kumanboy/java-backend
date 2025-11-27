package uz.itpu.teamwork.project.meal.order.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.itpu.teamwork.project.auth.entity.User;
import uz.itpu.teamwork.project.auth.repository.UserRepository;
import uz.itpu.teamwork.project.exception.ResourceNotFoundException;
import uz.itpu.teamwork.project.meal.order.dto.request.CreateOrderRequest;
import uz.itpu.teamwork.project.meal.order.dto.request.OrderItemRequest;
import uz.itpu.teamwork.project.meal.order.dto.response.OrderItemResponse;
import uz.itpu.teamwork.project.meal.order.dto.response.OrderResponse;
import uz.itpu.teamwork.project.meal.order.entity.Order;
import uz.itpu.teamwork.project.meal.order.entity.OrderItem;
import uz.itpu.teamwork.project.meal.order.enums.OrderStatus;
import uz.itpu.teamwork.project.meal.order.repository.OrderItemRepository;
import uz.itpu.teamwork.project.meal.order.repository.OrderRepository;
import uz.itpu.teamwork.project.meal.order.service.OrderService;
import uz.itpu.teamwork.project.meal.product.model.Product;
import uz.itpu.teamwork.project.meal.product.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {

        log.info("Creating order for userId={}, fulfillment={}, payment={}",
                userId, request.getFulfillmentMethod(), request.getPaymentMethod());

        // 1) Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 2) Build order items
        List<OrderItem> orderItems = request.getItems().stream()
                .map(this::createOrderItemFromRequest)
                .collect(Collectors.toList());

        // 3) Backend totals
        BigDecimal itemsSubtotal = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO;
        BigDecimal vatAmount = request.getVatAmount() != null ? request.getVatAmount() : BigDecimal.ZERO;
        BigDecimal deliveryFee = request.getDeliveryFee() != null ? request.getDeliveryFee() : BigDecimal.ZERO;

        BigDecimal totalAmount = itemsSubtotal
                .subtract(discount)
                .add(vatAmount)
                .add(deliveryFee);

        // 4) Build Order entity
        LocalDateTime orderDateTime = request.getOrderDate() != null
                ? request.getOrderDate().atStartOfDay()
                : LocalDateTime.now();

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .fulfillmentMethod(request.getFulfillmentMethod())
                .paymentMethod(request.getPaymentMethod())
                .status(OrderStatus.NEW)

                .itemsSubtotal(itemsSubtotal)
                .discountAmount(discount)
                .vatAmount(vatAmount)
                .deliveryFee(deliveryFee)
                .totalAmount(totalAmount)

                .orderDate(orderDateTime)
                .timeSlotId(request.getTimeSlotId())
                .guests(request.getGuests())

                .pickupVenueId(request.getPickupVenueId())

                .deliveryZoneId(request.getDeliveryZoneId())
                .deliveryAddressLine1(request.getDeliveryAddressLine1())
                .deliveryAddressLine2(request.getDeliveryAddressLine2())
                .deliveryCity(request.getDeliveryCity())
                .deliveryState(request.getDeliveryState())
                .deliveryZip(request.getDeliveryZip())
                .deliveryInstructions(request.getDeliveryInstructions())

                // ⭐ NEW: persist country code from request
                .countryCode(request.getCountryCode())

                .build();

        // Set back-reference
        orderItems.forEach(i -> i.setOrder(order));
        order.setItems(orderItems);

        // 5) Save order + items
        Order saved = orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);

        log.info("Order created. orderId={}, orderNumber={}", saved.getId(), saved.getOrderNumber());

        // 6) Map to response
        return mapToOrderResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    // ----------------- Helpers -----------------

    private OrderItem createOrderItemFromRequest(OrderItemRequest req) {

        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", "id", req.getProductId())
                );

        int qty = req.getQuantity() != null ? req.getQuantity() : 1;
        BigDecimal quantity = BigDecimal.valueOf(qty);

        BigDecimal unitPrice = product.getBasePrice() != null ? product.getBasePrice() : BigDecimal.ZERO;
        BigDecimal subtotal = unitPrice.multiply(quantity);

        return OrderItem.builder()
                .product(product)
                .productName(product.getName())
                .quantity(qty)
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .selectedModifiers(req.getSelectedModifiers())
                .removedIngredients(req.getRemovedIngredients())
                .build();
    }

    private String generateOrderNumber() {
        String shortUuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String datePart = java.time.LocalDate.now().toString().replace("-", "");
        return "ORD-" + datePart + "-" + shortUuid;
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())

                .fulfillmentMethod(order.getFulfillmentMethod())
                .paymentMethod(order.getPaymentMethod())

                .itemsSubtotal(order.getItemsSubtotal())
                .discountAmount(order.getDiscountAmount())
                .vatAmount(order.getVatAmount())
                .deliveryFee(order.getDeliveryFee())
                .totalAmount(order.getTotalAmount())

                .orderDate(order.getOrderDate())
                .timeSlotId(order.getTimeSlotId())
                .guests(order.getGuests())
                .pickupVenueId(order.getPickupVenueId())

                .deliveryZoneId(order.getDeliveryZoneId())
                .deliveryAddressLine1(order.getDeliveryAddressLine1())
                .deliveryAddressLine2(order.getDeliveryAddressLine2())
                .deliveryCity(order.getDeliveryCity())
                .deliveryState(order.getDeliveryState())
                .deliveryZip(order.getDeliveryZip())
                .deliveryInstructions(order.getDeliveryInstructions())

                .createdAt(order.getCreatedAt())

                .items(order.getItems().stream()
                        .map(this::mapToOrderItemResponse)
                        .collect(Collectors.toList())
                )
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .selectedModifiers(item.getSelectedModifiers())
                .removedIngredients(item.getRemovedIngredients())
                .build();
    }
}
