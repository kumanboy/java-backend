package uz.itpu.teamwork.project.meal.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.itpu.teamwork.project.meal.order.entity.Order;
import uz.itpu.teamwork.project.meal.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * All orders of a specific user (for "My Orders" page).
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Orders in a given date range (used by sales reports).
     * Uses order_date field from database.
     */
    List<Order> findByOrderDateBetweenAndStatusIn(
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Set<OrderStatus> statuses
    );

    /**
     * Still keep createdAt search (optional, not used by reports anymore).
     */
    List<Order> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

}
