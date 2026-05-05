package com.tmdt.marketplace.service;

import com.tmdt.marketplace.model.Order;
import com.tmdt.marketplace.model.OrderItem;
import com.tmdt.marketplace.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MarketplaceService {

    private final List<Product> products = new CopyOnWriteArrayList<>(List.of(
            new Product(1L, "Petals & Parchment", "Letterpress", 24.0, false, "/images/placeholder-1.png", "Luna Press"),
            new Product(2L, "Golden Solstice", "Birthday", 36.0, true, "/images/placeholder-2.png", "Golden Fold"),
            new Product(3L, "Indigo Dreams", "Anniversary", 48.0, true, "/images/placeholder-3.png", "Indigo Studio"),
            new Product(4L, "Botanical Keepsake", "Thinking of You", 52.0, true, "/images/placeholder-4.png", "Vine Atelier")
    ));

    private final List<Order> orders = new CopyOnWriteArrayList<>(List.of(
            new Order(
                    88292024L,
                    "Sketch Phase",
                    "1284 Artisans Row, Hudson Valley, NY 12534",
                    "Eleanor Varce",
                    List.of(
                            new OrderItem(2L, "Hand-Carved Earthen Vase", 1, 320.0),
                            new OrderItem(4L, "Botanical Keepsake", 1, 52.0)
                    ),
                    390.45
            )
    ));

    public List<Product> getProducts() {
        return products;
    }

    public List<Order> getOrders() {
        return orders;
    }
}
