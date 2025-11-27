package uz.itpu.teamwork.project.meal.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.itpu.teamwork.project.meal.order.entity.Order;
import uz.itpu.teamwork.project.meal.order.enums.OrderStatus;
import uz.itpu.teamwork.project.meal.order.repository.OrderRepository;
import uz.itpu.teamwork.project.meal.report.dto.MonthlySalesBucket;
import uz.itpu.teamwork.project.meal.report.dto.RegionSalesBucket;
import uz.itpu.teamwork.project.meal.report.dto.SalesReportResponse;
import uz.itpu.teamwork.project.meal.report.service.SalesReportService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesReportServiceImpl implements SalesReportService {

    private final OrderRepository orderRepository;

    /**
     * Build aggregated sales report for given date range.
     * We include only "real" orders (e.g. NEW / IN_PROGRESS / COMPLETED),
     * and usually skip CANCELLED / REFUNDED from revenue stats.
     */
    @Override
    @Transactional(readOnly = true)
    public SalesReportResponse getSalesReport(LocalDate fromDate, LocalDate toDate) {
        // Defensive defaults: if frontend forgets to send dates
        LocalDate from = (fromDate != null) ? fromDate : LocalDate.now().withDayOfYear(1);
        LocalDate to   = (toDate != null)   ? toDate   : LocalDate.now();

        if (to.isBefore(from)) {
            // swap if accidentally reversed
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }

        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay(); // inclusive range

        // Which statuses count as "revenue"?
        Set<OrderStatus> includedStatuses = EnumSet.of(
                OrderStatus.NEW,
                OrderStatus.IN_PROGRESS,
                OrderStatus.COMPLETED
                // CANCELLED, REFUNDED usually not included
        );

        log.info("Building sales report from {} to {} for statuses={}", from, to, includedStatuses);

        List<Order> orders = orderRepository
                .findByOrderDateBetweenAndStatusIn(fromDateTime, toDateTime, includedStatuses);

        if (orders.isEmpty()) {
            log.info("No orders found in given period. Returning empty report.");
            return SalesReportResponse.builder()
                    .fromDate(from)
                    .toDate(to)
                    .totalSubtotal(BigDecimal.ZERO)
                    .totalVat(BigDecimal.ZERO)
                    .totalDiscount(BigDecimal.ZERO)
                    .totalDeliveryFee(BigDecimal.ZERO)
                    .totalAmount(BigDecimal.ZERO)
                    .averageCheck(BigDecimal.ZERO)
                    .ordersCount(0L)
                    .regionBuckets(Collections.emptyList())
                    .monthBuckets(Collections.emptyList())
                    .build();
        }

        // ---- Global aggregates ------------------------------------------------

        BigDecimal totalSubtotal = orders.stream()
                .map(o -> o.getItemsSubtotal() != null ? o.getItemsSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVat = orders.stream()
                .map(o -> o.getVatAmount() != null ? o.getVatAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount = orders.stream()
                .map(o -> o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDeliveryFee = orders.stream()
                .map(o -> o.getDeliveryFee() != null ? o.getDeliveryFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = orders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long ordersCount = orders.size();

        // we already returned for empty list, so ordersCount >= 1
        BigDecimal averageCheck = totalAmount.divide(
                BigDecimal.valueOf(ordersCount),
                2,
                RoundingMode.HALF_UP
        );

        // ---- Region buckets ---------------------------------------------------
        Map<String, BigDecimal> regionTotalMap = new LinkedHashMap<>();
        Map<String, Long> regionCountMap = new LinkedHashMap<>();

        for (Order o : orders) {
            BigDecimal amount = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            String region = resolveRegionForOrder(o);

            regionTotalMap.merge(region, amount, BigDecimal::add);
            regionCountMap.merge(region, 1L, Long::sum);
        }

        List<RegionSalesBucket> regionBuckets = regionTotalMap.entrySet()
                .stream()
                .map(entry -> RegionSalesBucket.builder()
                        .region(entry.getKey())
                        .totalAmount(entry.getValue())
                        .ordersCount(regionCountMap.getOrDefault(entry.getKey(), 0L))
                        .build())
                .collect(Collectors.toList());

        // ---- Monthly buckets (YYYY-MM) ---------------------------------------

        Map<String, BigDecimal> monthTotalMap = new LinkedHashMap<>();
        Map<String, Long> monthCountMap = new LinkedHashMap<>();

        for (Order o : orders) {
            if (o.getOrderDate() == null) {
                continue;
            }
            YearMonth ym = YearMonth.from(o.getOrderDate());
            String monthKey = ym.toString(); // "2025-11"

            BigDecimal amount = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;

            monthTotalMap.merge(monthKey, amount, BigDecimal::add);
            monthCountMap.merge(monthKey, 1L, Long::sum);
        }

        List<MonthlySalesBucket> monthBuckets = monthTotalMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey()) // chronological (YYYY-MM)
                .map(entry -> MonthlySalesBucket.builder()
                        .monthKey(entry.getKey())
                        .totalAmount(entry.getValue())
                        .ordersCount(monthCountMap.getOrDefault(entry.getKey(), 0L))
                        .build())
                .collect(Collectors.toList());

        // ---- Build DTO --------------------------------------------------------

        return SalesReportResponse.builder()
                .fromDate(from)
                .toDate(to)
                .totalSubtotal(totalSubtotal)
                .totalVat(totalVat)
                .totalDiscount(totalDiscount)
                .totalDeliveryFee(totalDeliveryFee)
                .totalAmount(totalAmount)
                .averageCheck(averageCheck)
                .ordersCount(ordersCount)
                .regionBuckets(regionBuckets)
                .monthBuckets(monthBuckets)
                .build();
    }

    /**
     * Map order to logical region based on countryCode stored on Order.
     */
    private String resolveRegionForOrder(Order order) {
        if (order == null) {
            return "Unknown";
        }

        String code = order.getCountryCode();
        if (code == null || code.isBlank()) {
            return "Unknown";
        }

        String countryCode = code.trim().toUpperCase();

        return switch (countryCode) {
            // Central Asia
            case "UZ", "UZB", "KZ", "KAZ", "KG", "KGZ", "TJ", "TJK", "TM", "TKM" ->
                    "Central Asia";

            // Caucasus
            case "GE", "GEO", "AM", "ARM", "AZ", "AZE" ->
                    "Caucasus";

            // Eastern Europe
            case "UA", "UKR", "PL", "POL", "RO", "ROU", "BG", "BGR" ->
                    "Eastern Europe";

            // East Asia
            case "CN", "CHN", "JP", "JPN", "KR", "KOR" ->
                    "East Asia";

            default -> "Other";
        };
    }
}
