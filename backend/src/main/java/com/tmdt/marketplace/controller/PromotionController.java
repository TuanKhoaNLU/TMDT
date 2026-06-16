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
public class PromotionController {

    private final MarketplaceModuleService marketplaceModuleService;
    private final RequestGuard requestGuard;

    public PromotionController(MarketplaceModuleService marketplaceModuleService, RequestGuard requestGuard) {
        this.marketplaceModuleService = marketplaceModuleService;
        this.requestGuard = requestGuard;
    }

    @PostMapping("/v1/vouchers/apply")
    public VoucherApplyResponse applyVoucher(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody VoucherApplyRequest request) {
        return marketplaceModuleService.applyVoucher(requestGuard.requireUserId(tokenUserId), request);
    }
}
