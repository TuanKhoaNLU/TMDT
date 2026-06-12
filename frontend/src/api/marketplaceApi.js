import { axiosInstance } from "./axiosInstance";

export const marketplaceApi = {
  products: () => axiosInstance.get("/products").then((res) => res.data),

  cart: () => axiosInstance.get("/cart").then((res) => res.data),
  addCartItem: (payload) => axiosInstance.post("/cart/items", payload).then((res) => res.data),
  updateCartItem: (itemKey, payload) =>
    axiosInstance.put(`/cart/items/${encodeURIComponent(itemKey)}`, payload).then((res) => res.data),
  removeCartItem: (itemKey) =>
    axiosInstance.delete(`/cart/items/${encodeURIComponent(itemKey)}`).then((res) => res.data),
  clearCart: () => axiosInstance.delete("/cart").then((res) => res.data),

  checkoutSummary: (districtId, wardCode) =>
    axiosInstance
      .get("/checkout/summary", { params: { districtId, wardCode } })
      .then((res) => res.data),
  checkout: (payload) => axiosInstance.post("/orders/checkout", payload).then((res) => res.data),

  createVnpayPayment: (orderId) =>
    axiosInstance.post("/payments/vnpay/create", { orderId }).then((res) => res.data),
  verifyVnpayReturn: (queryString) =>
    axiosInstance.get(`/payments/vnpay-return${queryString}`).then((res) => res.data),

  provinces: () => axiosInstance.get("/shipping/provinces").then((res) => res.data),
  districts: (provinceId) =>
    axiosInstance.get("/shipping/districts", { params: { provinceId } }).then((res) => res.data),
  wards: (districtId) =>
    axiosInstance.get("/shipping/wards", { params: { districtId } }).then((res) => res.data),

  buyerOrders: () => axiosInstance.get("/orders").then((res) => res.data),
  orderDetail: (orderId) => axiosInstance.get(`/orders/${orderId}`).then((res) => res.data),
  cancelOrder: (orderId) => axiosInstance.put(`/orders/${orderId}/cancel`).then((res) => res.data),
  cancelShopOrder: (orderId, shopOrderId) =>
    axiosInstance.put(`/orders/${orderId}/shop-orders/${shopOrderId}/cancel`).then((res) => res.data),

  sellerOrders: () => axiosInstance.get("/seller/orders").then((res) => res.data),
  updateSellerOrderStatus: (shopOrderId, status) =>
    axiosInstance.put(`/seller/orders/${shopOrderId}/status`, { status }).then((res) => res.data),
  createGhnShipment: (shopOrderId) =>
    axiosInstance
      .post(`/seller/orders/${shopOrderId}/shipments/ghn`, {
        serviceName: "GHN Standard",
        note: "Tao tu seller dashboard"
      })
      .then((res) => res.data),

  adminOrders: () => axiosInstance.get("/admin/orders").then((res) => res.data),
  cancelAdminOrder: (orderId) => axiosInstance.put(`/admin/orders/${orderId}/cancel`).then((res) => res.data),
  cancelAdminShopOrder: (orderId, shopOrderId) =>
    axiosInstance.put(`/admin/orders/${orderId}/shop-orders/${shopOrderId}/cancel`).then((res) => res.data)
};
