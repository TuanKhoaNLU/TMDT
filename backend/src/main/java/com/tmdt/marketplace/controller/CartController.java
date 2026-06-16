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
public class CartController {

    private final MarketplaceService marketplaceService;
    private final RequestGuard requestGuard;

    public CartController(MarketplaceService marketplaceService, RequestGuard requestGuard) {
        this.marketplaceService = marketplaceService;
        this.requestGuard = requestGuard;
    }

    @GetMapping("/v1/cart")
    public CartResponse cart(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        if (tokenUserId == null) {
            return new CartResponse(0L, List.of(), BigDecimal.ZERO, 0, false);
        }
        return marketplaceService.getCart(tokenUserId);
    }

    @PostMapping("/v1/cart/items")
    public CartResponse addCartItem(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody CartItemRequest request) {
        return marketplaceService.addCartItem(requestGuard.requireUserId(tokenUserId), request);
    }

    @PutMapping("/v1/cart/items/{itemKey}")
    public CartResponse updateCartItem(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable String itemKey,
            @RequestBody UpdateCartItemRequest request) {
        return marketplaceService.updateCartItem(requestGuard.requireUserId(tokenUserId), itemKey, request);
    }

    @DeleteMapping("/v1/cart/items/{itemKey}")
    public CartResponse deleteCartItem(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable String itemKey) {
        return marketplaceService.deleteCartItem(requestGuard.requireUserId(tokenUserId), itemKey);
    }

    @DeleteMapping("/v1/cart")
    public CartResponse clearCart(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceService.clearCart(requestGuard.requireUserId(tokenUserId));
    }
}
