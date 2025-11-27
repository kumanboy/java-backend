package uz.itpu.teamwork.project.meal.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Aggregated sales for a logical region
 * (e.g. "Central Asia", "Caucasus", etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionSalesBucket {

    /**
     * Human-readable region name, e.g. "Central Asia"
     */
    private String region;

    /**
     * Total sales amount in this region (sum of totalAmount)
     */
    private BigDecimal totalAmount;

    /**
     * Number of orders in this region
     */
    private Long ordersCount;
}
