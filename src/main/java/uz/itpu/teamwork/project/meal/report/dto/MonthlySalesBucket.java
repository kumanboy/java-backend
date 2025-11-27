package uz.itpu.teamwork.project.meal.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Aggregated sales per month (YYYY-MM).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySalesBucket {

    /**
     * Month key in format "YYYY-MM", e.g. "2025-01"
     */
    private String monthKey;

    /**
     * Total sales amount in that month (sum of totalAmount)
     */
    private BigDecimal totalAmount;

    /**
     * Number of orders in that month
     */
    private Long ordersCount;
}
