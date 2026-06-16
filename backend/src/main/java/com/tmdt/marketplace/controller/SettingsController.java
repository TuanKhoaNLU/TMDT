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
public class SettingsController {

    private final MarketplaceModuleService marketplaceModuleService;

    public SettingsController(MarketplaceModuleService marketplaceModuleService) {
        this.marketplaceModuleService = marketplaceModuleService;
    }

    @GetMapping("/v1/settings/platform")
    public Map<String, String> platformSettings() {
        return marketplaceModuleService.platformSettings();
    }
}
