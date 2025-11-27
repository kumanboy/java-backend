package uz.itpu.teamwork.project.meal.report.service;

import uz.itpu.teamwork.project.meal.report.dto.SalesReportResponse;

import java.time.LocalDate;

/**
 * Service for building aggregated sales reports
 * based on orders + order_items.
 */
public interface SalesReportService {

    /**
     * Build sales report for given date range (inclusive).
     *
     * @param fromDate start date (inclusive), e.g. 2025-01-01
     * @param toDate   end date (inclusive), e.g. 2025-01-31
     * @return aggregated sales report DTO
     */
    SalesReportResponse getSalesReport(LocalDate fromDate, LocalDate toDate);
}
