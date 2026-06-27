import { Navigate, Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import AdminOrdersPage from "./pages/AdminOrdersPage";
import AdminDashboardPage from "./pages/AdminDashboardPage";
import CartPage from "./pages/CartPage";
import ChatPage from "./pages/ChatPage";
import CheckoutPage from "./pages/CheckoutPage";
import CatalogManagementPage from "./pages/CatalogManagementPage";
import CustomOrdersPage from "./pages/CustomOrdersPage";
import AdminManagementPage from "./pages/AdminManagementPage";
import MediaLibraryPage from "./pages/MediaLibraryPage";
import NotificationsPage from "./pages/NotificationsPage";
import OrderDetailPage from "./pages/OrderDetailPage";
import OrdersPage from "./pages/OrdersPage";
import PaymentResultPage from "./pages/PaymentResultPage";
import ProductDetailPage from "./pages/ProductDetailPage";
import ProductsPage from "./pages/ProductsPage";
import ProfilePage from "./pages/ProfilePage";
import SellerDashboardPage from "./pages/SellerDashboardPage";
import SellerOrdersPage from "./pages/SellerOrdersPage";
import ShopPage from "./pages/ShopPage";
import LoginPage from "./pages/LoginPage";
import MarketplaceModulesPage from "./pages/MarketplaceModulesPage";
import RegisterPage from "./pages/RegisterPage";
import VerifyOtpPage from "./pages/VerifyOtpPage";
import WishlistPage from "./pages/WishlistPage";
import ForgotPasswordPage from "./pages/ForgotPasswordPage";
import CustomRequestsPage from "./pages/CustomRequestsPage";
import SellerTransactionsPage from "./pages/SellerTransactionsPage";
import SellerShopProfilePage from "./pages/SellerShopProfilePage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verify-otp" element={<VerifyOtpPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route element={<Layout />}>
        <Route index element={<ProductsPage />} />
        <Route path="/products/:productId" element={<ProductDetailPage />} />
        <Route path="/shops/:shopId" element={<ShopPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/wishlist" element={<WishlistPage />} />
        <Route path="/chat" element={<ChatPage />} />
        <Route path="/custom-requests" element={<CustomRequestsPage />} />
        <Route path="/custom-orders" element={<CustomOrdersPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/modules" element={<MarketplaceModulesPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/payment-result" element={<PaymentResultPage />} />
        <Route path="/orders" element={<OrdersPage />} />
        <Route path="/orders/:orderId" element={<OrderDetailPage />} />
        <Route path="/seller" element={<SellerDashboardPage />} />
        <Route path="/seller/profile" element={<SellerShopProfilePage />} />
        <Route path="/seller/products" element={<CatalogManagementPage />} />
        <Route path="/seller/custom-orders" element={<CustomOrdersPage />} />
        <Route path="/seller/media" element={<MediaLibraryPage />} />
        <Route path="/seller/orders" element={<SellerOrdersPage />} />
        <Route path="/seller/transactions" element={<SellerTransactionsPage />} />
        <Route path="/admin" element={<AdminDashboardPage />} />
        <Route path="/admin/manage" element={<AdminManagementPage />} />
        <Route path="/admin/catalog" element={<CatalogManagementPage />} />
        <Route path="/admin/orders" element={<AdminOrdersPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
