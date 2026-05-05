package com.tmdt.marketplace.controller;

import com.tmdt.marketplace.model.Order;
import com.tmdt.marketplace.model.Product;
import com.tmdt.marketplace.service.MarketplaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final MarketplaceService marketplaceService;

    public ApiController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    @GetMapping("/products")
    public List<Product> products() {
        return marketplaceService.getProducts();
    }

    @GetMapping("/orders")
    public List<Order> orders() {
        return marketplaceService.getOrders();
    }
}
