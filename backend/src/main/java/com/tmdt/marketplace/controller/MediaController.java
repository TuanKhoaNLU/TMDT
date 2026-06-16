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
public class MediaController {

    private final MarketplaceModuleService marketplaceModuleService;
    private final RequestGuard requestGuard;

    public MediaController(MarketplaceModuleService marketplaceModuleService, RequestGuard requestGuard) {
        this.marketplaceModuleService = marketplaceModuleService;
        this.requestGuard = requestGuard;
    }

    @GetMapping("/v1/media/folders")
    public List<MediaFolderSummary> mediaFolders(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceModuleService.listMediaFolders(requestGuard.requireUserId(tokenUserId));
    }

    @PostMapping("/v1/media/folders")
    public MediaFolderSummary createMediaFolder(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody MediaFolderRequest request) {
        return marketplaceModuleService.createMediaFolder(requestGuard.requireUserId(tokenUserId), request);
    }

    @GetMapping("/v1/media/images")
    public List<MediaImageSummary> mediaImages(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceModuleService.listMediaImages(requestGuard.requireUserId(tokenUserId));
    }

    @PostMapping("/v1/media/images")
    public MediaImageSummary addMediaImage(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody MediaImageRequest request) {
        return marketplaceModuleService.addMediaImage(requestGuard.requireUserId(tokenUserId), request);
    }
}
