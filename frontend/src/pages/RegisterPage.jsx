import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { PackageCheck, ShieldCheck, ShoppingCart } from 'lucide-react';
import axiosInstance from '../api/axiosInstance';

export default function RegisterPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleRegister = async (e) => {
    e.preventDefault();
    try {
      const response = await axiosInstance.post('/auth/register', { username, password, email, fullName });
      localStorage.setItem('auth_token', response.data.token);
      localStorage.setItem('auth_user', JSON.stringify(response.data));
      navigate(`/verify-otp?email=${encodeURIComponent(email)}`);
    } catch (err) {
      setError(err.message || 'Registration failed.');
    }
  };

  return (
    <div className="auth-container auth-shell">
      <section className="auth-market-panel">
        <div className="brand auth-brand">
          <span className="brand-mark">T</span>
          <span>
            TMDT Market
            <small>Handmade commerce</small>
          </span>
        </div>
        <h1>Tao buyer account de bat dau mua hang tren san</h1>
        <div className="auth-benefits">
          <span><ShoppingCart size={17} /> Gio hang theo tung shop</span>
          <span><PackageCheck size={17} /> Checkout COD/VNPay</span>
          <span><ShieldCheck size={17} /> Theo doi trang thai don</span>
        </div>
      </section>
      <div className="auth-card">
        <h2>Dang ky buyer</h2>
        <p>Tai khoan moi se co vai tro BUYER</p>
        {error && <div className="alert-error">{error}</div>}
        <form onSubmit={handleRegister}>
          <div className="form-group">
            <label>Full Name</label>
            <input 
              type="text" 
              value={fullName} 
              onChange={e => setFullName(e.target.value)} 
              required 
            />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input 
              type="email" 
              value={email} 
              onChange={e => setEmail(e.target.value)} 
              required 
            />
          </div>
          <div className="form-group">
            <label>Username</label>
            <input 
              type="text" 
              value={username} 
              onChange={e => setUsername(e.target.value)} 
              required 
            />
          </div>
          <div className="form-group">
            <label>Password</label>
            <input 
              type="password" 
              value={password} 
              onChange={e => setPassword(e.target.value)} 
              required 
            />
          </div>
          <button type="submit" className="btn primary w-100">Tao tai khoan</button>
        </form>
        <div className="auth-footer">
          Da co tai khoan? <Link to="/login">Dang nhap</Link>
        </div>
      </div>
    </div>
  );
}
