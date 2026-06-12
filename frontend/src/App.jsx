import { Navigate, Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import AdminOrdersPage from "./pages/AdminOrdersPage";
import CartPage from "./pages/CartPage";
import CheckoutPage from "./pages/CheckoutPage";
import OrderDetailPage from "./pages/OrderDetailPage";
import OrdersPage from "./pages/OrdersPage";
import PaymentResultPage from "./pages/PaymentResultPage";
import ProductsPage from "./pages/ProductsPage";
import SellerOrdersPage from "./pages/SellerOrdersPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<Layout />}>
        <Route index element={<ProductsPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/payment-result" element={<PaymentResultPage />} />
        <Route path="/orders" element={<OrdersPage />} />
        <Route path="/orders/:orderId" element={<OrderDetailPage />} />
        <Route path="/seller/orders" element={<SellerOrdersPage />} />
        <Route path="/admin/orders" element={<AdminOrdersPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
