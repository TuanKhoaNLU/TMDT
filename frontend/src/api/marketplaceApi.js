import { axiosInstance } from "./axiosInstance";

export const marketplaceApi = {
  homepage: () => axiosInstance.get("/homepage").then((res) => res.data),
  products: () => axiosInstance.get("/products").then((res) => res.data),
  productDetail: (productId) => axiosInstance.get(`/products/${productId}`).then((res) => res.data),
  shopDetail: (shopId) => axiosInstance.get(`/shops/${shopId}`).then((res) => res.data),
  toggleFollowShop: (shopId) => axiosInstance.post(`/shops/${shopId}/follow`).then((res) => res.data),
  profile: () => axiosInstance.get("/users/me").then((res) => res.data),
  wishlist: () => axiosInstance.get("/wishlist").then((res) => res.data),
  toggleWishlist: (productId) => axiosInstance.post(`/wishlist/${productId}`).then((res) => res.data),
  askQuestion: (productId, payload) => axiosInstance.post(`/products/${productId}/questions`, payload).then((res) => res.data),
  answerQuestion: (questionId, payload) => axiosInstance.post(`/seller/questions/${questionId}/answer`, payload).then((res) => res.data),
  addReview: (productId, payload) => axiosInstance.post(`/products/${productId}/reviews`, payload).then((res) => res.data),
  reviewEligibility: (productId) => axiosInstance.get(`/products/${productId}/review-eligibility`).then((res) => res.data),
  notifications: () => axiosInstance.get("/notifications").then((res) => res.data),
  markNotificationRead: (notificationId) => axiosInstance.put(`/notifications/${notificationId}/read`).then((res) => res.data),
  markAllNotificationsRead: () => axiosInstance.put("/notifications/read-all").then((res) => res.data),
  modules: () => axiosInstance.get("/marketplace/modules").then((res) => res.data),
  categories: () => axiosInstance.get("/categories").then((res) => res.data),
  vouchers: () => axiosInstance.get("/vouchers").then((res) => res.data),
  applyVoucher: (payload) => axiosInstance.post("/vouchers/apply", payload).then((res) => res.data),
  flashSales: () => axiosInstance.get("/flash-sales").then((res) => res.data),
  giftWrapTiers: () => axiosInstance.get("/gift-wrap-tiers").then((res) => res.data),
  conversations: () => axiosInstance.get("/chat/conversations").then((res) => res.data),
  startConversation: (payload) => axiosInstance.post("/chat/conversations", payload).then((res) => res.data),
  messages: (conversationId) => axiosInstance.get(`/chat/conversations/${conversationId}/messages`).then((res) => res.data),
  sendMessage: (conversationId, payload) => axiosInstance.post(`/chat/conversations/${conversationId}/messages`, payload).then((res) => res.data),
  unsendMessage: (messageId) => axiosInstance.delete(`/chat/messages/${messageId}`).then((res) => res.data),
  uploadChatAttachment: async (conversationId, file) => {
    const formData = new FormData();
    formData.append("file", file);
    const token = localStorage.getItem("auth_token");
    const headers = {};
    if (token) headers["Authorization"] = `Bearer ${token}`;
    const res = await fetch(`${import.meta.env.VITE_API_URL || "/api/v1"}/chat/conversations/${conversationId}/attachments`, {
      method: "POST",
      body: formData,
      headers
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || "Lỗi tải file lên");
    }
    return res.json();
  },
  sendCustomQuote: (conversationId, payload) => axiosInstance.post(`/chat/conversations/${conversationId}/custom-quotes`, payload).then((res) => res.data),
  customOrders: () => axiosInstance.get("/custom-orders").then((res) => res.data),
  updateCustomOrderStatus: (customOrderId, payload) => axiosInstance.put(`/custom-orders/${customOrderId}/status`, payload).then((res) => res.data),
  customOrderRevisions: () => axiosInstance.get("/custom-orders/revisions").then((res) => res.data),
  requestCustomOrderRevision: (customOrderId, payload) => axiosInstance.post(`/custom-orders/${customOrderId}/revisions`, payload).then((res) => res.data),
  resolveCustomOrderRevision: (revisionId, payload) => axiosInstance.put(`/custom-orders/revisions/${revisionId}`, payload).then((res) => res.data),
  commissions: () => axiosInstance.get("/commissions").then((res) => res.data),
  createCommission: (payload) => axiosInstance.post("/commissions", payload).then((res) => res.data),
  proposals: (postId) => axiosInstance.get(`/commissions/${postId}/proposals`).then((res) => res.data),
  createProposal: (postId, payload) => axiosInstance.post(`/commissions/${postId}/proposals`, payload).then((res) => res.data),
  acceptProposal: (postId, proposalId) => axiosInstance.put(`/commissions/${postId}/proposals/${proposalId}/accept`).then((res) => res.data),
  mediaFolders: () => axiosInstance.get("/media/folders").then((res) => res.data),
  createMediaFolder: (payload) => axiosInstance.post("/media/folders", payload).then((res) => res.data),
  mediaImages: () => axiosInstance.get("/media/images").then((res) => res.data),
  addMediaImage: (payload) => axiosInstance.post("/media/images", payload).then((res) => res.data),
  createReport: (payload) => axiosInstance.post("/reports", payload).then((res) => res.data),
  platformSettings: () => axiosInstance.get("/settings/platform").then((res) => res.data),
  updatePlatformSettings: (payload) => axiosInstance.put("/settings/platform", payload).then((res) => res.data),
  paymentHistory: () => axiosInstance.get("/payments/history").then((res) => res.data),

  cart: () => axiosInstance.get("/cart").then((res) => res.data),
  addCartItem: (payload) => axiosInstance.post("/cart/items", payload).then((res) => res.data),
  updateCartItem: (itemKey, payload) => axiosInstance.put(`/cart/items/${encodeURIComponent(itemKey)}`, payload).then((res) => res.data),
  removeCartItem: (itemKey) => axiosInstance.delete(`/cart/items/${encodeURIComponent(itemKey)}`).then((res) => res.data),
  clearCart: () => axiosInstance.delete("/cart").then((res) => res.data),

  checkoutSummary: (districtId, wardCode) =>
    axiosInstance.get("/checkout/summary", { params: { districtId, wardCode } }).then((res) => res.data),
  checkout: (payload) => axiosInstance.post("/orders/checkout", payload).then((res) => res.data),

  createVnpayPayment: (orderId) => axiosInstance.post("/payments/vnpay/create", { orderId }).then((res) => res.data),
  verifyVnpayReturn: (queryString) => axiosInstance.get(`/payments/vnpay-return${queryString}`).then((res) => res.data),

  provinces: () => axiosInstance.get("/shipping/provinces").then((res) => res.data),
  districts: (provinceId) => axiosInstance.get("/shipping/districts", { params: { provinceId } }).then((res) => res.data),
  wards: (districtId) => axiosInstance.get("/shipping/wards", { params: { districtId } }).then((res) => res.data),
  updateShipmentStatus: (shipmentId, payload) => axiosInstance.put(`/shipping/shipments/${shipmentId}/status`, payload).then((res) => res.data),

  buyerOrders: () => axiosInstance.get("/orders").then((res) => res.data),
  orderDetail: (orderId) => axiosInstance.get(`/orders/${orderId}`).then((res) => res.data),
  cancelOrder: (orderId) => axiosInstance.put(`/orders/${orderId}/cancel`).then((res) => res.data),
  cancelShopOrder: (orderId, shopOrderId) => axiosInstance.put(`/orders/${orderId}/shop-orders/${shopOrderId}/cancel`).then((res) => res.data),
  requestReturn: (orderId, shopOrderId, payload) =>
    axiosInstance.post(`/orders/${orderId}/shop-orders/${shopOrderId}/returns`, payload).then((res) => res.data),
  buyerReturns: () => axiosInstance.get("/orders/returns").then((res) => res.data),

  sellerOrders: () => axiosInstance.get("/seller/orders").then((res) => res.data),
  sellerDashboard: () => axiosInstance.get("/seller/dashboard").then((res) => res.data),
  sellerShopProfile: () => axiosInstance.get("/seller/shop-profile").then((res) => res.data),
  updateSellerShopProfile: (payload) => axiosInstance.put("/seller/shop-profile", payload).then((res) => res.data),
  sellerProducts: () => axiosInstance.get("/seller/products").then((res) => res.data),
  createSellerProduct: (payload) => axiosInstance.post("/seller/products", payload).then((res) => res.data),
  updateSellerProduct: (productId, payload) => axiosInstance.put(`/seller/products/${productId}`, payload).then((res) => res.data),
  updateSellerInventory: (productId, payload) => axiosInstance.put(`/seller/products/${productId}/inventory`, payload).then((res) => res.data),
  sellerAnalytics: () => axiosInstance.get("/analytics/seller/summary").then((res) => res.data),
  sellerTransactions: () => axiosInstance.get("/seller/transactions").then((res) => res.data),
  sellerReviews: () => axiosInstance.get("/seller/reviews").then((res) => res.data),
  sellerReplyReview: (reviewId, payload) => axiosInstance.put(`/seller/reviews/${reviewId}/reply`, payload).then((res) => res.data),
  shippingProfiles: () => axiosInstance.get("/shipping-profiles").then((res) => res.data),
  createSellerVoucher: (payload) => axiosInstance.post("/seller/vouchers", payload).then((res) => res.data),
  createCustomOrder: (payload) => axiosInstance.post("/seller/custom-orders", payload).then((res) => res.data),
  updateSellerOrderStatus: (shopOrderId, status) => axiosInstance.put(`/seller/orders/${shopOrderId}/status`, { status }).then((res) => res.data),
  createGhnShipment: (shopOrderId) =>
    axiosInstance.post(`/seller/orders/${shopOrderId}/shipments/ghn`, {
      serviceName: "GHN Standard",
      note: "Tạo từ seller dashboard"
    }).then((res) => res.data),
  sellerReturns: () => axiosInstance.get("/seller/returns").then((res) => res.data),
  updateSellerReturn: (returnId, payload) => axiosInstance.put(`/seller/returns/${returnId}`, payload).then((res) => res.data),

  adminOrders: () => axiosInstance.get("/admin/orders").then((res) => res.data),
  adminDashboard: () => axiosInstance.get("/admin/dashboard").then((res) => res.data),
  adminProducts: () => axiosInstance.get("/admin/products").then((res) => res.data),
  moderateProduct: (productId, status) => axiosInstance.put(`/admin/products/${productId}/status`, { status }).then((res) => res.data),
  createCategory: (payload) => axiosInstance.post("/admin/categories", payload).then((res) => res.data),
  updateCategory: (categoryId, payload) => axiosInstance.put(`/admin/categories/${categoryId}`, payload).then((res) => res.data),
  adminReports: () => axiosInstance.get("/admin/reports").then((res) => res.data),
  updateReport: (reportId, payload) => axiosInstance.put(`/admin/reports/${reportId}`, payload).then((res) => res.data),
  adminUsers: () => axiosInstance.get("/admin/users").then((res) => res.data),
  createAdminUser: (payload) => axiosInstance.post("/admin/users", payload).then((res) => res.data),
  updateAdminUser: (userId, payload) => axiosInstance.put(`/admin/users/${userId}`, payload).then((res) => res.data),
  verifySeller: (userId, verified = true) => axiosInstance.put(`/admin/users/${userId}/verify-seller`, null, { params: { verified } }).then((res) => res.data),
  adminReviews: () => axiosInstance.get("/admin/reviews").then((res) => res.data),
  adminReplyReview: (reviewId, payload) => axiosInstance.put(`/admin/reviews/${reviewId}/reply`, payload).then((res) => res.data),
  adminReturns: () => axiosInstance.get("/admin/returns").then((res) => res.data),
  updateAdminReturn: (returnId, payload) => axiosInstance.put(`/admin/returns/${returnId}`, payload).then((res) => res.data),
  paymentReliability: () => axiosInstance.get("/admin/payment-reliability").then((res) => res.data),
  createPlatformVoucher: (payload) => axiosInstance.post("/admin/vouchers", payload).then((res) => res.data),
  createFlashSale: (payload) => axiosInstance.post("/admin/flash-sales", payload).then((res) => res.data),
  cancelAdminOrder: (orderId) => axiosInstance.put(`/admin/orders/${orderId}/cancel`).then((res) => res.data),
  cancelAdminShopOrder: (orderId, shopOrderId) => axiosInstance.put(`/admin/orders/${orderId}/shop-orders/${shopOrderId}/cancel`).then((res) => res.data)
};
