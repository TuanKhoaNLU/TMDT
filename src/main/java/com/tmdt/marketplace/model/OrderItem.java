package com.tmdt.marketplace.model;

public record OrderItem(
        Long productId,
        String productName,
        int quantity,
        double unitPrice
) {
}
