package uz.itpu.teamwork.project.meal.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.itpu.teamwork.project.meal.order.entity.OrderItem;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * All items that belong to a specific order.
     */
    List<OrderItem> findByOrderId(Long orderId);
}
