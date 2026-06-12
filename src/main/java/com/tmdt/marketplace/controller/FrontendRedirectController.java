package com.tmdt.marketplace.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class FrontendRedirectController {

    private final String frontendUrl;

    public FrontendRedirectController(@Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    @GetMapping("/")
    public RedirectView root() {
        return new RedirectView(frontendUrl + "/");
    }

    @GetMapping("/{page}.html")
    public RedirectView legacyPage(@PathVariable String page, HttpServletRequest request) {
        String route = switch (page) {
            case "login" -> "/login";
            case "cart" -> "/cart";
            case "checkout" -> "/checkout";
            case "order-history", "order-confirmation" -> "/orders";
            case "order-detail" -> orderDetailRoute(request);
            case "payment-result" -> "/payment-result";
            case "seller-orders" -> "/seller/orders";
            case "admin-orders" -> "/admin/orders";
            default -> "/";
        };
        String query = request.getQueryString();
        String suffix = query == null || query.isBlank() || route.startsWith("/orders/") ? "" : "?" + query;
        return new RedirectView(frontendUrl + route + suffix);
    }

    private String orderDetailRoute(HttpServletRequest request) {
        String id = request.getParameter("id");
        return id == null || id.isBlank() ? "/orders" : "/orders/" + id;
    }
}
