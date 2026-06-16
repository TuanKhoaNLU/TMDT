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
public class NotificationController {

    private final MarketplaceService marketplaceService;
    private final RequestGuard requestGuard;

    public NotificationController(MarketplaceService marketplaceService, RequestGuard requestGuard) {
        this.marketplaceService = marketplaceService;
        this.requestGuard = requestGuard;
    }

    @GetMapping("/v1/notifications")
    public NotificationCenterResponse notifications(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceService.getNotificationCenter(requestGuard.requireUserId(tokenUserId));
    }

    @PutMapping("/v1/notifications/{notificationId}/read")
    public NotificationCenterResponse markNotificationRead(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long notificationId) {
        return marketplaceService.markNotificationRead(requestGuard.requireUserId(tokenUserId), notificationId);
    }

    @PutMapping("/v1/notifications/read-all")
    public NotificationCenterResponse markAllNotificationsRead(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceService.markAllNotificationsRead(requestGuard.requireUserId(tokenUserId));
    }
}
