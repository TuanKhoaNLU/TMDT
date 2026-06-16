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
public class CommissionController {

    private final MarketplaceModuleService marketplaceModuleService;
    private final RequestGuard requestGuard;

    public CommissionController(MarketplaceModuleService marketplaceModuleService, RequestGuard requestGuard) {
        this.marketplaceModuleService = marketplaceModuleService;
        this.requestGuard = requestGuard;
    }

    @GetMapping("/v1/commissions")
    public List<CommissionPostSummary> commissions() {
        return marketplaceModuleService.listCommissionPosts();
    }

    @PostMapping("/v1/commissions")
    public CommissionPostSummary createCommission(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody CommissionPostRequest request) {
        return marketplaceModuleService.createCommissionPost(requestGuard.requireUserId(tokenUserId), request);
    }

    @GetMapping("/v1/commissions/{postId}/proposals")
    public List<ProposalSummary> proposals(@PathVariable Long postId) {
        return marketplaceModuleService.listProposals(postId);
    }

    @PostMapping("/v1/commissions/{postId}/proposals")
    public ProposalSummary createProposal(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long postId,
            @RequestBody ProposalRequest request) {
        return marketplaceModuleService.createProposal(requestGuard.requireUserId(tokenUserId),
                requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role), postId, request);
    }

    @PutMapping("/v1/commissions/{postId}/proposals/{proposalId}/accept")
    public ProposalSummary acceptProposal(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @PathVariable Long postId,
            @PathVariable Long proposalId) {
        return marketplaceModuleService.acceptProposal(requestGuard.requireUserId(tokenUserId), postId, proposalId);
    }
}
