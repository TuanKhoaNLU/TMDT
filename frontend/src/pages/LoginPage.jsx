import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ShieldCheck, ShoppingBag, Store } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../state/CartContext';
import axiosInstance from '../api/axiosInstance';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login } = useAuth();
  const { refreshCart } = useCart();
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const response = await axiosInstance.post('/auth/login', { username, password });
      const { token, ...userData } = response.data;
      login(userData, token);
      await refreshCart();
      navigate('/');
    } catch (err) {
      setError(err.message || 'Login failed. Please check your credentials.');
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
        <h1>Dang nhap de mua hang va quan ly don tren marketplace</h1>
        <div className="auth-benefits">
          <span><ShoppingBag size={17} /> Buyer dat hang da shop</span>
          <span><Store size={17} /> Seller xu ly shop order</span>
          <span><ShieldCheck size={17} /> Admin theo doi payment/shipment</span>
        </div>
      </section>
      <div className="auth-card">
        <h2>Dang nhap</h2>
        <p>Su dung tai khoan buyer, seller hoac admin</p>
        {error && <div className="alert-error">{error}</div>}
        <form onSubmit={handleLogin}>
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
          <button type="submit" className="btn primary w-100">Dang nhap</button>
        </form>
        <div className="auth-footer">
          Chua co tai khoan? <Link to="/register">Dang ky</Link>
        </div>
      </div>
    </div>
  );
}
