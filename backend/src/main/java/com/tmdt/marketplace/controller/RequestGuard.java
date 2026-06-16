package com.tmdt.marketplace.controller;

import com.tmdt.marketplace.service.MarketplaceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RequestGuard {
    private final MarketplaceService marketplaceService;

    public RequestGuard(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    public Long requireUserId(Long tokenUserId) {
        if (tokenUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap de tiep tuc");
        }
        return tokenUserId;
    }

    public void requireRole(String actualRole, String expectedRole) {
        if (actualRole == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap de tiep tuc");
        }
        if (!expectedRole.equalsIgnoreCase(actualRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan khong co quyen thuc hien thao tac nay");
        }
    }

    public Long requireSellerShopId(Long requestedShopId, Long tokenShopId, Long tokenUserId, String role) {
        Long userId = requireUserId(tokenUserId);
        requireRole(role, "SELLER");
        Long shopId = requestedShopId != null ? requestedShopId : tokenShopId;
        if (shopId == null || !marketplaceService.userOwnsShop(userId, shopId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan khong quan ly shop nay");
        }
        return shopId;
    }
}
