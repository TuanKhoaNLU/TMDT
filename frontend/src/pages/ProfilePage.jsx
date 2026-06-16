import { useQuery } from "@tanstack/react-query";
import { Bell, Heart, Loader2, MapPin, Store, Trophy, UserRound } from "lucide-react";
import { Link } from "react-router-dom";
import { marketplaceApi } from "../api/marketplaceApi";
import { formatMoney } from "../utils/format";

export default function ProfilePage() {
  const { data, isLoading } = useQuery({
    queryKey: ["profile"],
    queryFn: marketplaceApi.profile
  });

  if (isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Dang tai profile...</section>;
  }

  const { profile, addresses, wishlist, followedShops } = data;

  return (
    <div className="profile-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><UserRound size={16} /> Profile</span>
          <h1>{profile.fullName}</h1>
          <p className="muted">{profile.email} - {profile.role} - {profile.status}</p>
        </div>
        <div className="metric-grid compact">
          <div><strong>{profile.rewardPoints}</strong><span>Diem thuong</span></div>
          <div><strong>{wishlist.length}</strong><span>Wishlist</span></div>
          <div><strong>{followedShops.length}</strong><span>Shop follow</span></div>
        </div>
      </section>

      <section className="content-grid">
        <article className="panel">
          <h2><MapPin size={18} /> Dia chi giao hang</h2>
          <div className="stack-list">
            {addresses.map((item) => (
              <div className="soft-row" key={item.id}>
                <strong>{item.label || "Dia chi"} {item.defaultAddress ? "- Mac dinh" : ""}</strong>
                <span>{item.receiverName} - {item.phone}</span>
                <span className="muted">{item.address}, {item.ward}, {item.district}, {item.province}</span>
              </div>
            ))}
            {!addresses.length && <p className="muted">Ban chua luu dia chi rieng, checkout van co the nhap truc tiep.</p>}
          </div>
        </article>
        <article className="panel">
          <h2><Trophy size={18} /> Thanh toan va diem</h2>
          <p className="muted">Lich su thanh toan dang nam trong trang don mua. Diem thuong da san sang de ap dung o cac dot nang cap voucher.</p>
          <Link className="btn secondary" to="/orders">Xem don mua</Link>
        </article>
      </section>

      <section className="panel">
        <h2><Heart size={18} /> Wishlist</h2>
        <div className="product-grid mini-grid">
          {wishlist.map((product) => (
            <Link className="mini-product" to={`/products/${product.id}`} key={product.id}>
              <img src={product.image} alt={product.name} />
              <strong>{product.name}</strong>
              <span>{formatMoney(product.price)}</span>
            </Link>
          ))}
          {!wishlist.length && <p className="muted">Chua co san pham yeu thich.</p>}
        </div>
      </section>

      <section className="panel">
        <h2><Store size={18} /> Shop dang theo doi</h2>
        <div className="stack-list">
          {followedShops.map((shop) => (
            <Link className="soft-row" to={`/shops/${shop.id}`} key={shop.id}>
              <strong>{shop.shopName}</strong>
              <span className="muted">{shop.description}</span>
            </Link>
          ))}
          {!followedShops.length && <p className="muted">Chua follow shop nao.</p>}
        </div>
      </section>

      <Link className="btn secondary" to="/notifications"><Bell size={17} /> Trung tam thong bao</Link>
    </div>
  );
}
