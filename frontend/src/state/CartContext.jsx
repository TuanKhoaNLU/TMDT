import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { marketplaceApi } from "../api/marketplaceApi";

const emptyCart = {
  buyerId: 1,
  shops: [],
  subtotal: 0,
  totalQuantity: 0,
  canCheckout: false
};

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const [cart, setCart] = useState(emptyCart);
  const [loading, setLoading] = useState(true);

  async function refreshCart() {
    setLoading(true);
    try {
      const data = await marketplaceApi.cart();
      setCart(data);
      return data;
    } finally {
      setLoading(false);
    }
  }

  async function addItem(productId, quantity = 1, extras = {}) {
    const data = await marketplaceApi.addCartItem({ productId, quantity, ...extras });
    setCart(data);
    return data;
  }

  async function updateItem(itemKey, quantity) {
    const data = await marketplaceApi.updateCartItem(itemKey, { quantity });
    setCart(data);
    return data;
  }

  async function removeItem(itemKey) {
    const data = await marketplaceApi.removeCartItem(itemKey);
    setCart(data);
    return data;
  }

  async function clear() {
    const data = await marketplaceApi.clearCart();
    setCart(data);
    return data;
  }

  useEffect(() => {
    refreshCart().catch(() => setLoading(false));
  }, []);

  const value = useMemo(
    () => ({
      cart,
      loading,
      refreshCart,
      addItem,
      updateItem,
      removeItem,
      clear
    }),
    [cart, loading]
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const value = useContext(CartContext);
  if (!value) {
    throw new Error("useCart must be used inside CartProvider");
  }
  return value;
}
