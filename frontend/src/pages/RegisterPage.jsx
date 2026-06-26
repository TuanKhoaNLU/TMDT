import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { PackageCheck, ShieldCheck, ShoppingCart, Store } from "lucide-react";
import axiosInstance from "../api/axiosInstance";

export default function RegisterPage() {
  const [mode, setMode] = useState("BUYER");
  const [form, setForm] = useState({
    username: "",
    password: "",
    email: "",
    fullName: "",
    shopName: "",
    phone: "",
    description: ""
  });
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const navigate = useNavigate();

  const update = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
  };

  const handleRegister = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");
    try {
      if (mode === "SELLER") {
        const response = await axiosInstance.post("/auth/register-seller", form);
        setMessage(response.data?.message || "Đã gửi hồ sơ người bán, vui lòng chờ quản trị viên phê duyệt.");
        return;
      }
      await axiosInstance.post("/auth/register", {
        username: form.username,
        password: form.password,
        email: form.email,
        fullName: form.fullName
      });
      navigate(`/verify-otp?email=${encodeURIComponent(form.email)}`);
    } catch (err) {
      setError(err.message || "Đăng ký thất bại.");
    }
  };

  return (
    <div className="auth-container auth-shell">
      <section className="auth-market-panel">
        <div className="brand auth-brand">
          <span className="brand-mark">T</span>
          <span>
            TMDT Market
            <small>Sàn handmade Việt</small>
          </span>
        </div>
        <h1>{mode === "SELLER" ? "Đăng ký shop handmade và chờ phê duyệt" : "Tạo tài khoản để mua sắm sản phẩm thủ công"}</h1>
        <div className="auth-benefits">
          <span><ShoppingCart size={17} /> Giỏ hàng theo từng shop</span>
          <span><PackageCheck size={17} /> Theo dõi đơn hàng rõ ràng</span>
          <span><ShieldCheck size={17} /> Tài khoản được bảo vệ</span>
        </div>
      </section>

      <div className="auth-card">
        <div className="segmented-control">
          <button type="button" className={mode === "BUYER" ? "active" : ""} onClick={() => setMode("BUYER")}>
            <ShoppingCart size={16} /> Khách hàng
          </button>
          <button type="button" className={mode === "SELLER" ? "active" : ""} onClick={() => setMode("SELLER")}>
            <Store size={16} /> Người bán
          </button>
        </div>
        <h2>{mode === "SELLER" ? "Đăng ký tài khoản người bán" : "Đăng ký tài khoản khách hàng"}</h2>
        <p>{mode === "SELLER" ? "Hồ sơ shop sẽ được quản trị viên xét duyệt trước khi đăng bán." : "Tài khoản mới cần xác thực OTP qua email demo."}</p>

        {error && <div className="alert-error">{error}</div>}
        {message && <div className="alert-success">{message}</div>}

        <form onSubmit={handleRegister}>
          <div className="form-group">
            <label>Họ tên</label>
            <input type="text" value={form.fullName} onChange={update("fullName")} required />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input type="email" value={form.email} onChange={update("email")} required />
          </div>
          <div className="form-group">
            <label>Tên đăng nhập</label>
            <input type="text" value={form.username} onChange={update("username")} required />
          </div>
          <div className="form-group">
            <label>Mật khẩu</label>
            <input type="password" value={form.password} onChange={update("password")} required />
          </div>

          {mode === "SELLER" && (
            <>
              <div className="form-group">
                <label>Tên shop</label>
                <input type="text" value={form.shopName} onChange={update("shopName")} required />
              </div>
              <div className="form-group">
                <label>Số điện thoại</label>
                <input type="tel" value={form.phone} onChange={update("phone")} required />
              </div>
              <div className="form-group">
                <label>Mô tả sản phẩm thủ công</label>
                <textarea rows="3" value={form.description} onChange={update("description")} required />
              </div>
            </>
          )}

          <button type="submit" className="btn primary w-100">
            {mode === "SELLER" ? "Gửi hồ sơ người bán" : "Tạo tài khoản"}
          </button>
        </form>
        <div className="auth-footer">
          Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
        </div>
      </div>
    </div>
  );
}
