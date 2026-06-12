import axios from "axios";

export const axiosInstance = axios.create({
  baseURL: "/api/v1",
  headers: {
    Accept: "application/json"
  }
});

axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem("auth_token");
  
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !error.config.url.includes('/auth/')) {
      localStorage.removeItem("auth_token");
      localStorage.removeItem("auth_user");
      window.location.href = '/login';
    }
    const message =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      "Request failed";
    return Promise.reject(new Error(message));
  }
);

export default axiosInstance;
