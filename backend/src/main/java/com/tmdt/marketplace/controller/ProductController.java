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
public class ProductController {

    private final MarketplaceService marketplaceService;
    private final RequestGuard requestGuard;

    public ProductController(MarketplaceService marketplaceService, RequestGuard requestGuard) {
        this.marketplaceService = marketplaceService;
        this.requestGuard = requestGuard;
    }

    @GetMapping({"/products", "/v1/products"})
    public List<ProductCard> products() {
        return marketplaceService.getProducts();
    }

    @GetMapping("/v1/homepage")
    public HomepageResponse homepage() {
        return marketplaceService.getHomepage();
    }

    @GetMapping("/v1/products/{productId}")
    public ProductDetailResponse productDetail(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long productId) {
        return marketplaceService.getProductDetail(tokenUserId, productId);
    }

    @GetMapping("/v1/shops/{shopId}")
    public PublicShopResponse publicShop(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long shopId) {
        return marketplaceService.getPublicShop(tokenUserId, shopId);
    }

    @PostMapping("/v1/shops/{shopId}/follow")
    public FollowToggleResponse toggleFollowShop(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long shopId) {
        return marketplaceService.toggleFollowShop(requestGuard.requireUserId(tokenUserId), shopId);
    }

    @GetMapping("/v1/wishlist")
    public List<ProductCard> wishlist(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId) {
        return marketplaceService.getWishlist(requestGuard.requireUserId(tokenUserId));
    }

    @PostMapping("/v1/wishlist/{productId}")
    public WishlistToggleResponse toggleWishlist(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long productId) {
        return marketplaceService.toggleWishlist(requestGuard.requireUserId(tokenUserId), productId);
    }

    @PostMapping("/v1/products/{productId}/questions")
    public QuestionSummary askQuestion(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long productId,
            @RequestBody QuestionRequest request) {
        return marketplaceService.askQuestion(requestGuard.requireUserId(tokenUserId), productId, request);
    }

    @PostMapping("/v1/products/{productId}/reviews")
    public ReviewSummary addReview(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long productId,
            @RequestBody ReviewRequest request) {
        return marketplaceService.addReview(requestGuard.requireUserId(tokenUserId), productId, request);
    }
}
