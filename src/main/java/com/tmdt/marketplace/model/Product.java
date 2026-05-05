package com.tmdt.marketplace.model;

public record Product(
        Long id,
        String name,
        String category,
        double price,
        boolean customizable,
        String image,
        String artisan
) {
}
