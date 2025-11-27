package uz.itpu.teamwork.project.meal.report.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.itpu.teamwork.project.meal.report.dto.SalesReportResponse;
import uz.itpu.teamwork.project.meal.report.service.SalesReportService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class SalesReportController {

    private final SalesReportService salesReportService;

    /**
     * GET /api/reports/sales?fromDate=2025-01-01&toDate=2025-03-31
     *
     * Both query params are optional:
     *  - if missing, backend uses sensible defaults (YTD) in service.
     */
    @GetMapping("/sales")
    public ResponseEntity<SalesReportResponse> getSalesReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        SalesReportResponse report = salesReportService.getSalesReport(fromDate, toDate);
        return ResponseEntity.ok(report);
    }
}
