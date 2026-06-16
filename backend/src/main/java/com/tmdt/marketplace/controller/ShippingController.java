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
public class ShippingController {

    private final MarketplaceService marketplaceService;

    public ShippingController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    @PostMapping("/v1/shipping/fee")
    public ShippingFeeResponse shippingFee(@RequestBody ShippingFeeRequest request) {
        return marketplaceService.calculateShippingFee(request);
    }

    @GetMapping("/v1/shipping/provinces")
    public List<MasterDataOption> provinces() {
        return marketplaceService.getProvinces();
    }

    @GetMapping("/v1/shipping/districts")
    public List<MasterDataOption> districts(@RequestParam(required = false) Integer provinceId) {
        return marketplaceService.getDistricts(provinceId);
    }

    @GetMapping("/v1/shipping/wards")
    public List<MasterDataOption> wards(@RequestParam(required = false) Integer districtId) {
        return marketplaceService.getWards(districtId);
    }
}
