package com.rest.restaurantsystem.report;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
public class DailyReportController {

    private final DailyReportService service;

    public DailyReportController(DailyReportService service) {
        this.service = service;
    }

    @GetMapping("/daily")
    public DailyReportResponse findDaily(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication
    ) {
        return service.findDaily(branchId, date, authentication.getName());
    }
}
