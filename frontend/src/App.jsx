import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { Layout } from "./components/Layout";
import { useAuth } from "./context/AuthContext";
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

function ProtectedRoute({ children, roles, requireShop = false }) {
  const { user, token } = useAuth();
  const location = useLocation();

  if (!user || !token) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location, message: "Vui long dang nhap de tiep tuc." }}
      />
    );
  }

  const userRole = String(user.role || "").toUpperCase();
  const allowedRoles = roles?.map((role) => role.toUpperCase());
  const hasRole = !allowedRoles?.length || allowedRoles.includes(userRole);

  if (!hasRole || (requireShop && !user.shopId)) {
    return <Navigate to="/" replace />;
  }

  return children;
}

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
        <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
        <Route path="/wishlist" element={<ProtectedRoute roles={["BUYER"]}><WishlistPage /></ProtectedRoute>} />
        <Route path="/chat" element={<ProtectedRoute roles={["BUYER", "SELLER"]}><ChatPage /></ProtectedRoute>} />
        <Route path="/custom-requests" element={<ProtectedRoute roles={["BUYER", "SELLER"]}><CustomRequestsPage /></ProtectedRoute>} />
        <Route path="/custom-orders" element={<ProtectedRoute roles={["BUYER", "SELLER"]}><CustomOrdersPage /></ProtectedRoute>} />
        <Route path="/notifications" element={<ProtectedRoute><NotificationsPage /></ProtectedRoute>} />
        <Route path="/modules" element={<ProtectedRoute roles={["ADMIN"]}><MarketplaceModulesPage /></ProtectedRoute>} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<ProtectedRoute roles={["BUYER"]}><CheckoutPage /></ProtectedRoute>} />
        <Route path="/payment-result" element={<PaymentResultPage />} />
        <Route path="/orders" element={<ProtectedRoute roles={["BUYER"]}><OrdersPage /></ProtectedRoute>} />
        <Route path="/orders/:orderId" element={<ProtectedRoute roles={["BUYER"]}><OrderDetailPage /></ProtectedRoute>} />
        <Route path="/seller" element={<ProtectedRoute roles={["SELLER"]} requireShop><SellerDashboardPage /></ProtectedRoute>} />
        <Route path="/seller/profile" element={<ProtectedRoute roles={["SELLER"]} requireShop><SellerShopProfilePage /></ProtectedRoute>} />
        <Route path="/seller/products" element={<ProtectedRoute roles={["SELLER"]} requireShop><CatalogManagementPage /></ProtectedRoute>} />
        <Route path="/seller/custom-orders" element={<ProtectedRoute roles={["SELLER"]} requireShop><CustomOrdersPage /></ProtectedRoute>} />
        <Route path="/seller/media" element={<ProtectedRoute roles={["SELLER"]} requireShop><MediaLibraryPage /></ProtectedRoute>} />
        <Route path="/seller/orders" element={<ProtectedRoute roles={["SELLER"]} requireShop><SellerOrdersPage /></ProtectedRoute>} />
        <Route path="/seller/transactions" element={<ProtectedRoute roles={["SELLER"]} requireShop><SellerTransactionsPage /></ProtectedRoute>} />
        <Route path="/admin" element={<ProtectedRoute roles={["ADMIN"]}><AdminDashboardPage /></ProtectedRoute>} />
        <Route path="/admin/manage" element={<ProtectedRoute roles={["ADMIN"]}><AdminManagementPage /></ProtectedRoute>} />
        <Route path="/admin/catalog" element={<ProtectedRoute roles={["ADMIN"]}><CatalogManagementPage /></ProtectedRoute>} />
        <Route path="/admin/orders" element={<ProtectedRoute roles={["ADMIN"]}><AdminOrdersPage /></ProtectedRoute>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
