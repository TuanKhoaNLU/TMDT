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
public class ChatController {

    private final MarketplaceModuleService marketplaceModuleService;
    private final RequestGuard requestGuard;

    public ChatController(MarketplaceModuleService marketplaceModuleService, RequestGuard requestGuard) {
        this.marketplaceModuleService = marketplaceModuleService;
        this.requestGuard = requestGuard;
    }

    @GetMapping("/v1/chat/conversations")
    public List<ConversationSummary> conversations(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role) {
        Long userId = requestGuard.requireUserId(tokenUserId);
        Long effectiveShopId = "SELLER".equalsIgnoreCase(role) ? requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role) : null;
        return marketplaceModuleService.listConversations(userId, effectiveShopId, role);
    }

    @PostMapping("/v1/chat/conversations")
    public ConversationSummary startConversation(
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestBody ConversationRequest request) {
        return marketplaceModuleService.startConversation(requestGuard.requireUserId(tokenUserId), request);
    }

    @GetMapping("/v1/chat/conversations/{conversationId}/messages")
    public List<MessageSummary> messages(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long conversationId) {
        Long userId = requestGuard.requireUserId(tokenUserId);
        Long effectiveShopId = "SELLER".equalsIgnoreCase(role) ? requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role) : null;
        return marketplaceModuleService.listMessages(conversationId, userId, role, effectiveShopId);
    }

    @PostMapping("/v1/chat/conversations/{conversationId}/messages")
    public MessageSummary sendMessage(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long conversationId,
            @RequestBody MessageRequest request) {
        Long userId = requestGuard.requireUserId(tokenUserId);
        Long effectiveShopId = "SELLER".equalsIgnoreCase(role) ? requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role) : null;
        return marketplaceModuleService.sendMessage(userId, role, conversationId, effectiveShopId, request);
    }

    @PostMapping("/v1/chat/conversations/{conversationId}/custom-quotes")
    public MessageSummary sendCustomQuote(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestAttribute(value = "shopId", required = false) Long tokenShopId,
            @RequestAttribute(value = "userId", required = false) Long tokenUserId,
            @RequestAttribute(value = "role", required = false) String role,
            @PathVariable Long conversationId,
            @RequestBody CustomQuoteRequest request) {
        return marketplaceModuleService.sendCustomQuote(
                requestGuard.requireUserId(tokenUserId),
                requestGuard.requireSellerShopId(shopId, tokenShopId, tokenUserId, role),
                conversationId,
                request);
    }
}
