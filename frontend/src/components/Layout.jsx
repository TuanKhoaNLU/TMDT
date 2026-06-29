import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import {
  Bell,
  Heart,
  Home,
  LogIn,
  LogOut,
  Menu,
  MessageSquare,
  Package,
  ReceiptText,
  Search,
  ShieldCheck,
  ShoppingCart,
  Sparkles,
  Store,
  UserRound
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../state/CartContext";

export function Layout() {
  const { cart } = useCart();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState("");
  const role = String(user?.role || "").toUpperCase();
  const isBuyer = role === "BUYER";
  const isSeller = role === "SELLER" && Boolean(user?.shopId);
  const isAdmin = role === "ADMIN";
  const canUseBuyerTools = !user || isBuyer;
  const canUseCustomTools = isBuyer || isSeller;

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const handleSearch = (event) => {
    event.preventDefault();
    const query = keyword.trim();
    navigate(query ? `/?q=${encodeURIComponent(query)}` : "/");
  };

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="utility-bar">
          <div className="container utility-inner">
            <span>TMDT handmade marketplace</span>
            <span>COD/VNPay · GHN Standard · Nhiều shop handmade</span>
          </div>
        </div>
        <div className="container topbar">
          <NavLink className="brand" to="/">
            <span className="brand-mark">T</span>
            <span>
              TMDT Market
              <small>Handmade commerce</small>
            </span>
          </NavLink>
          <form className="market-search" onSubmit={handleSearch}>
            <Search size={18} />
            <input
              aria-label="Tìm sản phẩm"
              placeholder="Tìm thiệp handmade, quà tặng custom, decor..."
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
            <button type="submit">Tìm kiếm</button>
          </form>
          <nav className="main-nav" aria-label="Main navigation">
            <NavLink to="/" end>
              <Home size={17} /> Sản phẩm
            </NavLink>
            {canUseBuyerTools && (
              <NavLink to="/cart">
                <ShoppingCart size={17} /> Giỏ hàng <span className="cart-pill">{cart.totalQuantity || 0}</span>
              </NavLink>
            )}
            {user ? (
              <>
                {isBuyer && (
                  <>
                    <NavLink to="/wishlist">
                      <Heart size={17} /> Wishlist
                    </NavLink>
                    <NavLink to="/orders">
                      <ReceiptText size={17} /> Đơn mua
                    </NavLink>
                  </>
                )}
                {canUseCustomTools && (
                  <>
                    <NavLink to="/chat">
                      <MessageSquare size={17} /> Chat
                    </NavLink>
                    <NavLink to="/custom-requests">
                      <Sparkles size={17} /> Custom
                    </NavLink>
                  </>
                )}
                {isSeller && (
                  <NavLink to="/seller">
                    <Store size={17} /> Kênh bán
                  </NavLink>
                )}
                {isAdmin && (
                  <NavLink to="/admin">
                    <ShieldCheck size={17} /> Admin
                  </NavLink>
                )}
                <div className="auth-menu">
                  <button className="icon-btn header-icon" title="Thông báo" type="button" onClick={() => navigate("/notifications")}>
                    <Bell size={17} />
                  </button>
                  <button className="user-greeting plain-user" type="button" onClick={() => navigate("/profile")}>
                    <UserRound size={15} />
                    {user.fullName || "User"}
                  </button>
                  <button onClick={handleLogout} className="btn-logout" title="Đăng xuất">
                    <LogOut size={17} />
                  </button>
                </div>
              </>
            ) : (
              <NavLink to="/login" className="login-link">
                <LogIn size={17} /> Đăng nhập
              </NavLink>
            )}
          </nav>
        </div>
        <div className="category-strip">
          <div className="container category-row">
            <span><Menu size={16} /> Danh mục</span>
            <a href="/?category=Thiep%20handmade">Thiệp handmade</a>
            <a href="/?category=Qua%20tang%20custom">Quà tặng custom</a>
            <a href="/?category=Decor%20thu%20cong">Decor thủ công</a>
            {canUseBuyerTools && <NavLink to="/cart">Giỏ hàng đa shop</NavLink>}
            {isBuyer && <NavLink to="/orders">Theo dõi đơn</NavLink>}
            {canUseCustomTools && <NavLink to="/chat">Chat/quote</NavLink>}
            {canUseCustomTools && <NavLink to="/custom-requests">Yêu cầu custom</NavLink>}
            {isSeller && <NavLink to="/seller">Kênh bán</NavLink>}
            {isAdmin && <NavLink to="/admin/manage">Quản trị</NavLink>}
            {isAdmin && <NavLink to="/modules">Tất cả module</NavLink>}
          </div>
        </div>
      </header>
      <main className="container page">
        <Outlet />
      </main>
      <footer className="site-footer">
        <div className="container">
          <span><Package size={16} /> TMDT Market vận hành đơn theo từng shop.</span>
          <span className="footer-note">COD · VNPay · GHN</span>
        </div>
      </footer>
    </div>
  );
}
