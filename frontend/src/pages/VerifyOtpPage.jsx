import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import axiosInstance from "../api/axiosInstance";

export default function VerifyOtpPage() {
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState(searchParams.get("email") || "");
  const [otp, setOtp] = useState("123456");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  async function submit(event) {
    event.preventDefault();
    setError("");
    try {
      const response = await axiosInstance.post("/auth/verify-otp", { email, otp });
      setMessage(response.data.message);
      setTimeout(() => navigate("/login"), 600);
    } catch (err) {
      setError(err.message || "OTP không hợp lệ");
    }
  }

  return (
    <div className="auth-container auth-shell">
      <section className="auth-market-panel">
        <div className="brand auth-brand">
          <span className="brand-mark">T</span>
          <span>TMDT Market<small>Email OTP</small></span>
        </div>
        <h1>Xác thực email để kích hoạt tài khoản buyer</h1>
      </section>
      <div className="auth-card">
        <h2>Xác thực OTP</h2>
        <p>Demo local dung ma 123456.</p>
        {error && <div className="alert-error">{error}</div>}
        {message && <div className="alert success">{message}</div>}
        <form onSubmit={submit}>
          <div className="form-group">
            <label>Email</label>
            <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
          </div>
          <div className="form-group">
            <label>OTP</label>
            <input value={otp} onChange={(event) => setOtp(event.target.value)} required />
          </div>
          <button className="btn primary w-100" type="submit">Xác thực</button>
        </form>
        <div className="auth-footer">
          <Link to="/login">Quay lại đăng nhập</Link>
        </div>
      </div>
    </div>
  );
}
