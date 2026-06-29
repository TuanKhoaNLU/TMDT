import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axiosInstance from '../api/axiosInstance';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../state/CartContext';

const GOOGLE_CLIENT_ID = '304173374708-a1c1okfa4lvtjlvp9j6p774mbnbj2f1n.apps.googleusercontent.com';

export function useGoogleAuth() {
  const { login } = useAuth();
  const { refreshCart } = useCart();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleGoogleCredential = useCallback(
    async (accessToken) => {
      setLoading(true);
      setError(null);
      try {
        const response = await axiosInstance.post('/auth/google-login', { accessToken });
        const { token, ...userData } = response.data;
        
        login(userData, token);
        await refreshCart();
        navigate('/');
      } catch (err) {
        setError(err.response?.data?.message || err.message || 'Google sign-in failed. Please try again.');
      } finally {
        setLoading(false);
      }
    },
    [login, refreshCart, navigate]
  );

  const signInWithGoogle = useCallback(() => {
    if (!window.google) {
      setError('Google Identity Services chưa tải xong. Thử lại sau.');
      return;
    }

    const tokenClient = window.google.accounts.oauth2.initTokenClient({
      client_id: GOOGLE_CLIENT_ID,
      scope: 'openid email profile',
      callback: async (response) => {
        if (response.error || !response.access_token) {
          setError('Đăng nhập Google thất bại. Vui lòng thử lại.');
          return;
        }
        handleGoogleCredential(response.access_token);
      },
    });

    tokenClient.requestAccessToken();
  }, [handleGoogleCredential]);

  return { signInWithGoogle, loading, error };
}
