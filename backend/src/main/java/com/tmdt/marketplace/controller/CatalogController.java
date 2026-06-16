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
public class CatalogController {

    private final MarketplaceModuleService marketplaceModuleService;

    public CatalogController(MarketplaceModuleService marketplaceModuleService) {
        this.marketplaceModuleService = marketplaceModuleService;
    }

    @GetMapping("/v1/marketplace/modules")
    public MarketplaceModulesResponse modules() {
        return marketplaceModuleService.modules();
    }

    @GetMapping("/v1/categories")
    public List<CategoryAdminSummary> categories() {
        return marketplaceModuleService.listCategories();
    }

    @GetMapping("/v1/vouchers")
    public List<VoucherSummary> vouchers() {
        return marketplaceModuleService.listVouchers();
    }

    @GetMapping("/v1/flash-sales")
    public List<FlashSaleSummary> flashSales() {
        return marketplaceModuleService.listFlashSales();
    }

    @GetMapping("/v1/gift-wrap-tiers")
    public List<GiftWrapTier> giftWrapTiers() {
        return marketplaceModuleService.listGiftWrapTiers();
    }
}
