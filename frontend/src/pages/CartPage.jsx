import { ArrowRight, Loader2, Minus, Plus, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import { EmptyState } from "../components/EmptyState";
import { StatusBadge } from "../components/StatusBadge";
import { useCart } from "../state/CartContext";
import { formatMoney } from "../utils/format";

export default function CartPage() {
  const { cart, loading, updateItem, removeItem, clear } = useCart();

  if (loading) {
    return (
      <section className="loading-panel">
        <Loader2 className="spin" size={22} /> Đang tải giỏ hàng...
      </section>
    );
  }

  if (!cart.shops.length) {
    return <EmptyState title="Giỏ hàng đang trống" />;
  }

  return (
    <div className="two-col">
      <section className="stack">
        <div className="page-head">
          <div>
            <h1>Giỏ hàng</h1>
            <p className="muted">Sản phẩm được gom theo từng shop.</p>
          </div>
          <button className="btn secondary" onClick={clear}>
            <Trash2 size={17} /> Xóa tất cả
          </button>
        </div>

        {cart.shops.map((shop) => (
          <article className="shop-panel" key={shop.shopId}>
            <div className="shop-head">
              <h2>{shop.shopName}</h2>
              <strong>{formatMoney(shop.subtotal)}</strong>
            </div>
            {shop.items.map((item) => (
              <div className="cart-line" key={item.itemKey}>
                <img src={item.image} alt={item.productName} />
                <div>
                  <h3>{item.productName}</h3>
                  <p className="muted">{formatMoney(item.unitPrice)}</p>
                  {item.note && <p className="muted">{item.note}</p>}
                  {!item.available && <p className="danger">{item.message}</p>}
                </div>
                <div className="quantity-control">
                  <button
                    className="icon-btn"
                    title="Giam so luong"
                    onClick={() => updateItem(item.itemKey, Math.max(1, item.quantity - 1))}
                  >
                    <Minus size={16} />
                  </button>
                  <input
                    aria-label="So luong"
                    min="1"
                    type="number"
                    value={item.quantity}
                    onChange={(event) => updateItem(item.itemKey, Number(event.target.value || 1))}
                  />
                  <button
                    className="icon-btn"
                    title="Tang so luong"
                    onClick={() => updateItem(item.itemKey, item.quantity + 1)}
                  >
                    <Plus size={16} />
                  </button>
                </div>
                <strong>{formatMoney(item.lineTotal)}</strong>
                <button className="icon-btn danger-btn" title="Xóa sản phẩm" onClick={() => removeItem(item.itemKey)}>
                  <Trash2 size={17} />
                </button>
              </div>
            ))}
          </article>
        ))}
      </section>

      <aside className="summary-panel">
        <h2>Tổng kết</h2>
        <div className="summary-row">
          <span>So luong</span>
          <strong>{cart.totalQuantity}</strong>
        </div>
        <div className="summary-row">
          <span>Tam tinh</span>
          <strong>{formatMoney(cart.subtotal)}</strong>
        </div>
        <div className="summary-row">
          <span>Trang thai</span>
          <StatusBadge status={cart.canCheckout ? "ACTIVE" : "FAILED"} />
        </div>
        <p className="muted">Phi ship tinh o checkout theo tung shop.</p>
        {cart.canCheckout ? (
          <Link className="btn primary full" to="/checkout">
            Đặt hàng <ArrowRight size={18} />
          </Link>
        ) : (
          <button className="btn primary full" disabled>
            Đặt hàng <ArrowRight size={18} />
          </button>
        )}
      </aside>
    </div>
  );
}
