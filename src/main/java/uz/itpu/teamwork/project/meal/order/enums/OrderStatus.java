package uz.itpu.teamwork.project.meal.order.enums;

/**
 * High-level lifecycle of an order in the cafe.
 */
public enum OrderStatus {

    /**
     * Order was just created from the cart.
     */
    NEW,

    /**
     * Manager/admin confirmed the order.
     */
    CONFIRMED,

    /**
     * Order is being prepared in the kitchen.
     */
    IN_PROGRESS,

    /**
     * Order is ready (for pickup / delivery).
     */
    READY,

    /**
     * Order is successfully delivered / picked up / served.
     */
    COMPLETED,

    /**
     * Order was cancelled by customer or staff.
     */
    CANCELLED
}
