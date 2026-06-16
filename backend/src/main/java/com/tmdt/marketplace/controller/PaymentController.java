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
public class PaymentController {

    private final MarketplaceService marketplaceService;
    private final MarketplaceModuleService marketplaceModuleService;
    private final RequestGuard requestGuard;

    public PaymentController(MarketplaceService marketplaceService, MarketplaceModuleService marketplaceModuleService, RequestGuard requestGuard) {
        this.marketplaceService = marketplaceService;
        this.marketplaceModuleService = marketplaceModuleService;
        this.requestGuard = requestGuard;
    }

    @GetMapping("/v1/payments/history")
    public List<PaymentHistorySummary> paymentHistory(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceModuleService.paymentHistory(requestGuard.requireUserId(tokenUserId));
    }

    @PostMapping("/v1/payments/vnpay/create")
    public PaymentCreateResponse createVnpayPayment(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody PaymentCreateRequest request) {
        return marketplaceService.createVnpayPayment(requestGuard.requireUserId(tokenUserId), request);
    }

    @GetMapping("/v1/payments/vnpay-return")
    public PaymentReturnResponse vnpayReturn(@RequestParam Map<String, String> params) {
        return marketplaceService.verifyVnpayReturn(params);
    }
}
