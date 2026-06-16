import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Heart, Loader2, ShoppingCart, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import { marketplaceApi } from "../api/marketplaceApi";
import { useCart } from "../state/CartContext";
import { formatMoney } from "../utils/format";

export default function WishlistPage() {
  const queryClient = useQueryClient();
  const { addItem } = useCart();
  const { data = [], isLoading } = useQuery({ queryKey: ["wishlist"], queryFn: marketplaceApi.wishlist });
  const toggle = useMutation({
    mutationFn: marketplaceApi.toggleWishlist,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["wishlist"] })
  });

  if (isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Dang tai wishlist...</section>;
  }

  return (
    <div className="dashboard-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><Heart size={16} /> Wishlist</span>
          <h1>San pham yeu thich</h1>
          <p className="muted">Luu san pham handmade de mua sau hoac chat voi shop.</p>
        </div>
      </section>

      <section className="product-grid">
        {data.map((product) => (
          <article className="product-card" key={product.id}>
            <div className="img-wrapper">
              <Link to={`/products/${product.id}`}><img src={product.image} alt={product.name} /></Link>
              <span className="mall-tag">{product.customizable ? "Dat rieng" : "Co san"}</span>
            </div>
            <div className="product-body">
              <div className="product-info">
                <Link className="shop-name" to={`/shops/${product.shopId}`}>{product.shopName}</Link>
                <Link to={`/products/${product.id}`}><h2>{product.name}</h2></Link>
                <span className="muted">{product.category}</span>
              </div>
              <div className="product-meta">
                <strong>{formatMoney(product.price)}</strong>
              </div>
              <div className="product-actions">
                <button className="btn primary" onClick={() => addItem(product.id, 1)} type="button">
                  <ShoppingCart size={17} /> Them gio
                </button>
                <button className="btn secondary danger-btn" onClick={() => toggle.mutate(product.id)} type="button">
                  <Trash2 size={17} /> Bo luu
                </button>
              </div>
            </div>
          </article>
        ))}
        {!data.length && <section className="empty-state compact-empty"><Heart size={32} /><h2>Wishlist dang trong</h2></section>}
      </section>
    </div>
  );
}
