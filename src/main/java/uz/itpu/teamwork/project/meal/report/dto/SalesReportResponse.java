package uz.itpu.teamwork.project.meal.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportResponse {

    // filter period
    private LocalDate fromDate;
    private LocalDate toDate;

    // aggregated sums
    private BigDecimal totalSubtotal;
    private BigDecimal totalVat;
    private BigDecimal totalDiscount;
    private BigDecimal totalDeliveryFee;
    private BigDecimal totalAmount;

    // average order amount
    private BigDecimal averageCheck;

    // how many orders
    private Long ordersCount;

    // ---- NEW: buckets for charts -----------------------------------------

    /**
     * Aggregated totals per logical region
     * (e.g. "Central Asia", "Caucasus", etc.).
     */
    private List<RegionSalesBucket> regionBuckets;

    /**
     * Aggregated totals per month (YYYY-MM).
     */
    private List<MonthlySalesBucket> monthBuckets;
}
