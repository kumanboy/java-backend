package uz.itpu.teamwork.project.meal.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import uz.itpu.teamwork.project.auth.security.UserPrincipal;
import uz.itpu.teamwork.project.auth.dto.response.ApiResponse;
import uz.itpu.teamwork.project.exception.AuthException;
import uz.itpu.teamwork.project.meal.order.dto.request.CreateOrderRequest;
import uz.itpu.teamwork.project.meal.order.dto.response.OrderResponse;
import uz.itpu.teamwork.project.meal.order.service.OrderService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Endpoints for cart checkout and user orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * Helper to get current authenticated user's ID from JWT
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException("No authenticated user");
        }

        return principal.getId();
    }

    /**
     * Create a new order for the authenticated user.
     */
    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Create order",
            description = "Creates a new order for the authenticated user"
    )
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        Long userId = getCurrentUserId();
        log.info("CreateOrder request by userId={}, method={}, payment={}",
                userId, request.getFulfillmentMethod(), request.getPaymentMethod());

        OrderResponse response = orderService.createOrder(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Order created successfully"));
    }

    /**
     * Get single order by its ID.
     */
    @GetMapping("/{orderId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Get order by ID",
            description = "Returns a single order by ID"
    )
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long orderId
    ) {
        log.info("GetOrderById request orderId={}", orderId);

        OrderResponse response = orderService.getOrderById(orderId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all orders for currently authenticated user.
     */
    @GetMapping("/my")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Get user's orders",
            description = "Returns all orders for the authenticated user"
    )
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders() {
        Long userId = getCurrentUserId();
        log.info("GetMyOrders request from userId={}", userId);

        List<OrderResponse> orders = orderService.getOrdersForUser(userId);

        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}
