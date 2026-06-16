import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertCircle, HeartHandshake, Loader2, ShieldCheck, Star, Store } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { marketplaceApi } from "../api/marketplaceApi";
import { formatMoney } from "../utils/format";

export default function ShopPage() {
  const { shopId } = useParams();
  const queryClient = useQueryClient();
  const { data, isLoading, error } = useQuery({
    queryKey: ["shop", shopId],
    queryFn: () => marketplaceApi.shopDetail(shopId)
  });

  const followMutation = useMutation({
    mutationFn: () => marketplaceApi.toggleFollowShop(shopId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["shop", shopId] })
  });

  if (isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Dang tai shop...</section>;
  }
  if (error) {
    return <section className="alert error"><AlertCircle size={18} /> {error.message}</section>;
  }

  const { shop, products, followed } = data;

  return (
    <div className="shop-page">
      <section className="shop-hero" style={{ backgroundImage: `url(${shop.heroUrl})` }}>
        <div>
          <span className="section-kicker"><Store size={16} /> Handmade seller</span>
          <h1>{shop.shopName}</h1>
          <p>{shop.description}</p>
          <div className="shop-meta-row">
            <span><Star size={15} fill="currentColor" /> {shop.rating}</span>
            <span><HeartHandshake size={15} /> {shop.followerCount} follower</span>
            {shop.verifiedArtisan && <span><ShieldCheck size={15} /> Da xac minh nghe nhan</span>}
          </div>
          <button className="btn primary" onClick={() => followMutation.mutate()} type="button">
            {followed ? "Dang theo doi" : "Follow shop"}
          </button>
        </div>
      </section>

      <section className="content-grid">
        <article className="panel">
          <h2>Ho so nghe nhan</h2>
          <p>{shop.about || shop.description}</p>
          <div className="metric-grid compact">
            <div><strong>{shop.yearsExperience}</strong><span>Nam kinh nghiem</span></div>
            <div><strong>{shop.materials}</strong><span>Vat lieu chu dao</span></div>
          </div>
        </article>
        <article className="panel">
          <h2>Chinh sach shop</h2>
          <p className="muted">San pham handmade duoc dong goi theo tung shop, phi ship tinh tai checkout. Hang custom co the can them thoi gian xu ly.</p>
        </article>
      </section>

      <section className="panel">
        <h2>San pham cua shop</h2>
        <div className="product-grid">
          {products.map((product) => (
            <Link className="mini-product" to={`/products/${product.id}`} key={product.id}>
              <img src={product.image} alt={product.name} />
              <strong>{product.name}</strong>
              <span>{formatMoney(product.price)}</span>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
