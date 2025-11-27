package uz.itpu.teamwork.project.meal.order.service;

import uz.itpu.teamwork.project.meal.order.dto.request.CreateOrderRequest;
import uz.itpu.teamwork.project.meal.order.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {

    /**
     * Create a new order for the given user.
     */
    OrderResponse createOrder(CreateOrderRequest request, Long userId);

    /**
     * Get single order by id (can be used by admin or by owner).
     */
    OrderResponse getOrderById(Long orderId);

    /**
     * Get all orders for a given user (for profile "My Orders" page).
     */
    List<OrderResponse> getOrdersForUser(Long userId);
}
