import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import {
  Bell,
  Home,
  LogIn,
  LogOut,
  Menu,
  Package,
  ReceiptText,
  Search,
  ShieldCheck,
  ShoppingCart,
  Store,
  UserRound
} from "lucide-react";
import { useCart } from "../state/CartContext";
import { useAuth } from "../context/AuthContext";

export function Layout() {
  const { cart } = useCart();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState("");

  const handleLogout = () => {
    logout();
    navigate('/login');
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
            <span>COD/VNPay · GHN Standard · Nhieu shop handmade</span>
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
              aria-label="Tim san pham"
              placeholder="Tim thiep handmade, qua tang custom, decor..."
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
            <button type="submit">Tim kiem</button>
          </form>
          <nav className="main-nav" aria-label="Main navigation">
            <NavLink to="/" end>
              <Home size={17} /> San pham
            </NavLink>
            <NavLink to="/cart">
              <ShoppingCart size={17} /> Gio hang <span className="cart-pill">{cart.totalQuantity || 0}</span>
            </NavLink>
            {user ? (
              <>
                <NavLink to="/orders">
                  <ReceiptText size={17} /> Don mua
                </NavLink>
                {user.shopId && (
                  <NavLink to="/seller/orders">
                    <Store size={17} /> Kenh ban
                  </NavLink>
                )}
                {user.role === 'ADMIN' && (
                  <NavLink to="/admin/orders">
                    <ShieldCheck size={17} /> Admin
                  </NavLink>
                )}
                <div className="auth-menu">
                  <button className="icon-btn header-icon" title="Thong bao" type="button">
                    <Bell size={17} />
                  </button>
                  <span className="user-greeting">
                    <UserRound size={15} />
                    {user.fullName || 'User'}
                  </span>
                  <button onClick={handleLogout} className="btn-logout" title="Log Out">
                    <LogOut size={17} />
                  </button>
                </div>
              </>
            ) : (
              <NavLink to="/login" className="login-link">
                <LogIn size={17} /> Login
              </NavLink>
            )}
          </nav>
        </div>
        <div className="category-strip">
          <div className="container category-row">
            <span><Menu size={16} /> Danh muc</span>
            <a href="/?category=Thiep%20handmade">Thiep handmade</a>
            <a href="/?category=Qua%20tang%20custom">Qua tang custom</a>
            <a href="/?category=Decor%20thu%20cong">Decor thu cong</a>
            <a href="/cart">Gio hang da shop</a>
            <a href="/orders">Theo doi don</a>
          </div>
        </div>
      </header>
      <main className="container page">
        <Outlet />
      </main>
      <footer className="site-footer">
        <div className="container">
          <span><Package size={16} /> TMDT Market van hanh don theo tung shop.</span>
          <span className="footer-note">COD · VNPay · GHN</span>
        </div>
      </footer>
    </div>
  );
}
