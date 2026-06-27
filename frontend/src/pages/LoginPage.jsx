import { ShieldCheck, ShoppingBag, Store } from "lucide-react";
import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import axiosInstance from "../api/axiosInstance";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../state/CartContext";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const { login } = useAuth();
  const { refreshCart } = useCart();
  const navigate = useNavigate();
  const location = useLocation();

  async function handleLogin(event) {
    event.preventDefault();
    setError("");
    try {
      const response = await axiosInstance.post("/auth/login", { username, password });
      const { token, ...userData } = response.data;
      login(userData, token);
      await refreshCart();
      navigate("/");
    } catch (err) {
      setError(err.message || "Đăng nhập thất bại. Vui lòng kiểm tra tài khoản và mật khẩu.");
    }
  }

  return (
    <div className="auth-container auth-shell">
      <section className="auth-market-panel">
        <div className="brand auth-brand">
          <span className="brand-mark">T</span>
          <span>TMDT Market<small>Sàn handmade Việt</small></span>
        </div>
        <h1>Đăng nhập để mua hàng và quản lý đơn trên marketplace</h1>
        <div className="auth-benefits">
          <span><ShoppingBag size={17} /> Khách hàng đặt đơn đa shop</span>
          <span><Store size={17} /> Người bán xử lý đơn theo shop</span>
          <span><ShieldCheck size={17} /> Quản trị viên theo dõi vận hành</span>
        </div>
      </section>
      <div className="auth-card">
        <h2>Đăng nhập</h2>
        <p>Sử dụng tài khoản khách hàng, người bán hoặc quản trị viên</p>
        {location.state?.message && <div className="alert-success">{location.state.message}</div>}
        {error && <div className="alert-error">{error}</div>}
        <form onSubmit={handleLogin}>
          <div className="form-group">
            <label>Tên đăng nhập</label>
            <input type="text" value={username} onChange={(event) => setUsername(event.target.value)} required />
          </div>
          <div className="form-group">
            <label>Mật khẩu</label>
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
          </div>
          <Link className="text-link self-start" to="/forgot-password">Quên mật khẩu?</Link>
          <button type="submit" className="btn primary w-100">Đăng nhập</button>
        </form>
        <div className="auth-footer">Chưa có tài khoản? <Link to="/register">Đăng ký</Link></div>
      </div>
    </div>
  );
}
