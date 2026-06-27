import { ArrowLeft, KeyRound, Mail, ShieldCheck } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import axiosInstance from "../api/axiosInstance";

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "", otp: "", newPassword: "", confirmPassword: "" });
  const [step, setStep] = useState("REQUEST");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);

  const update = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));

  async function requestOtp(event) {
    event.preventDefault();
    setPending(true);
    setError("");
    try {
      const response = await axiosInstance.post("/auth/forgot-password", { email: form.email });
      setStep("RESET");
      setForm((current) => ({ ...current, otp: response.data?.otp || "" }));
      setMessage(response.data?.message || "Mã OTP đã được tạo.");
    } catch (err) {
      setError(err.message);
    } finally {
      setPending(false);
    }
  }

  async function resetPassword(event) {
    event.preventDefault();
    setError("");
    if (form.newPassword !== form.confirmPassword) {
      setError("Mật khẩu xác nhận không khớp.");
      return;
    }
    setPending(true);
    try {
      await axiosInstance.post("/auth/reset-password", {
        email: form.email,
        otp: form.otp,
        newPassword: form.newPassword
      });
      navigate("/login", { replace: true, state: { message: "Đổi mật khẩu thành công. Bạn có thể đăng nhập lại." } });
    } catch (err) {
      setError(err.message);
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="auth-container auth-shell">
      <section className="auth-market-panel">
        <div className="brand auth-brand">
          <span className="brand-mark">T</span>
          <span>TMDT Market<small>Khôi phục tài khoản</small></span>
        </div>
        <h1>Lấy lại quyền truy cập tài khoản của bạn</h1>
        <div className="auth-benefits">
          <span><Mail size={17} /> Xác nhận bằng email</span>
          <span><KeyRound size={17} /> OTP có hiệu lực trong 10 phút</span>
          <span><ShieldCheck size={17} /> Mật khẩu mới tối thiểu 6 ký tự</span>
        </div>
      </section>

      <div className="auth-card">
        <h2>{step === "REQUEST" ? "Quên mật khẩu" : "Đặt mật khẩu mới"}</h2>
        <p>{step === "REQUEST" ? "Nhập email đã dùng để đăng ký." : `Mã OTP đã gửi cho ${form.email}.`}</p>
        {message && <div className="alert-success">{message}</div>}
        {error && <div className="alert-error">{error}</div>}

        {step === "REQUEST" ? (
          <form onSubmit={requestOtp}>
            <div className="form-group">
              <label>Email</label>
              <input type="email" value={form.email} onChange={update("email")} required />
            </div>
            <button className="btn primary w-100" disabled={pending} type="submit">Gửi mã OTP</button>
          </form>
        ) : (
          <form onSubmit={resetPassword}>
            <div className="form-group">
              <label>Mã OTP</label>
              <input value={form.otp} onChange={update("otp")} required />
            </div>
            <div className="form-group">
              <label>Mật khẩu mới</label>
              <input type="password" minLength="6" value={form.newPassword} onChange={update("newPassword")} required />
            </div>
            <div className="form-group">
              <label>Xác nhận mật khẩu</label>
              <input type="password" minLength="6" value={form.confirmPassword} onChange={update("confirmPassword")} required />
            </div>
            <button className="btn primary w-100" disabled={pending} type="submit">Đổi mật khẩu</button>
          </form>
        )}

        <div className="auth-footer"><Link to="/login"><ArrowLeft size={15} /> Quay lại đăng nhập</Link></div>
      </div>
    </div>
  );
}
