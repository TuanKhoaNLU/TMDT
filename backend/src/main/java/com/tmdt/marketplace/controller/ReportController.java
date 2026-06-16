package com.tmdt.marketplace.controller;

import com.tmdt.marketplace.service.MarketplaceModuleService;
import com.tmdt.marketplace.service.MarketplaceModuleService.*;
import com.tmdt.marketplace.service.MarketplaceService;
import com.tmdt.marketplace.service.MarketplaceService.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReportController {

    private final MarketplaceModuleService marketplaceModuleService;
    private final RequestGuard requestGuard;

    public ReportController(MarketplaceModuleService marketplaceModuleService, RequestGuard requestGuard) {
        this.marketplaceModuleService = marketplaceModuleService;
        this.requestGuard = requestGuard;
    }

    @PostMapping("/v1/reports")
    public ReportSummary createReport(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody ReportRequest request) {
        return marketplaceModuleService.createReport(requestGuard.requireUserId(tokenUserId), request);
    }
}
