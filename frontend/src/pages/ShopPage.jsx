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
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Đang tải shop...</section>;
  }
  if (error) {
    return <section className="alert error"><AlertCircle size={18} /> {error.message}</section>;
  }

  const { shop, products, followed } = data;

  return (
    <div className="shop-page">
      <section className="shop-hero" style={{ backgroundImage: `url(${shop.heroUrl})` }}>
        <div>
          <span className="section-kicker"><Store size={16} /> Shop handmade</span>
          <h1>{shop.shopName}</h1>
          <p>{shop.description}</p>
          <div className="shop-meta-row">
            <span><Star size={15} fill="currentColor" /> {shop.rating}</span>
            <span><HeartHandshake size={15} /> {shop.followerCount} follower</span>
            {shop.verifiedArtisan && <span><ShieldCheck size={15} /> Đã xác minh nghệ nhân</span>}
          </div>
          <button className="btn primary" onClick={() => followMutation.mutate()} type="button">
            {followed ? "Đang theo dõi" : "Theo dõi shop"}
          </button>
        </div>
      </section>

      <section className="content-grid">
        <article className="panel">
          <h2>Hồ sơ nghệ nhân</h2>
          <p>{shop.about || shop.description}</p>
          <div className="metric-grid compact">
            <div><strong>{shop.yearsExperience}</strong><span>Năm kinh nghiệm</span></div>
            <div><strong>{shop.materials}</strong><span>Vật liệu chủ đạo</span></div>
          </div>
        </article>
        <article className="panel">
          <h2>Chính sách shop</h2>
          <p className="muted">Sản phẩm handmade duoc dong goi theo tung shop, phi ship tinh tai checkout. Hang custom co the can them thoi gian xu ly.</p>
        </article>
      </section>

      <section className="panel">
        <h2>Sản phẩm của shop</h2>
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
