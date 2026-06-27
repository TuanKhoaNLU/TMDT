import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BadgeCheck, Image, Loader2, Save, Store } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { marketplaceApi } from "../api/marketplaceApi";
import { StatusBadge } from "../components/StatusBadge";

const emptyProfile = {
  shopName: "",
  logoUrl: "",
  heroUrl: "",
  description: "",
  about: "",
  materials: "",
  yearsExperience: 1
};

export default function SellerShopProfilePage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState(emptyProfile);
  const [message, setMessage] = useState("");
  const profileQuery = useQuery({ queryKey: ["seller-shop-profile"], queryFn: marketplaceApi.sellerShopProfile });

  useEffect(() => {
    if (profileQuery.data) {
      setForm({
        shopName: profileQuery.data.shopName || "",
        logoUrl: profileQuery.data.logoUrl || "",
        heroUrl: profileQuery.data.heroUrl || "",
        description: profileQuery.data.description || "",
        about: profileQuery.data.about || "",
        materials: profileQuery.data.materials || "",
        yearsExperience: profileQuery.data.yearsExperience ?? 1
      });
    }
  }, [profileQuery.data]);

  const updateProfile = useMutation({
    mutationFn: marketplaceApi.updateSellerShopProfile,
    onSuccess: (data) => {
      queryClient.setQueryData(["seller-shop-profile"], data);
      queryClient.invalidateQueries({ queryKey: ["seller-dashboard"] });
      queryClient.invalidateQueries({ queryKey: ["shop", data.id] });
      setMessage("Đã cập nhật hồ sơ gian hàng.");
    }
  });

  if (profileQuery.isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Đang tải hồ sơ shop...</section>;
  }
  if (profileQuery.error) return <section className="alert error">{profileQuery.error.message}</section>;

  const profile = profileQuery.data;
  const update = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));

  return (
    <div className="dashboard-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><Store size={16} /> Gian hàng #{profile.id}</span>
          <h1>Quản lý hồ sơ gian hàng</h1>
          <p className="muted">Thông tin này được hiển thị trên trang shop công khai.</p>
        </div>
        <div className="badge-row">
          {profile.verifiedArtisan && <span className="verified-label"><BadgeCheck size={17} /> Nghệ nhân đã xác minh</span>}
          <StatusBadge status={profile.status} />
          <Link className="btn secondary" to={`/shops/${profile.id}`}>Xem trang công khai</Link>
        </div>
      </section>

      {message && <section className="alert success">{message}</section>}
      {updateProfile.error && <section className="alert error">{updateProfile.error.message}</section>}

      <section className="shop-profile-layout">
        <form
          className="panel shop-profile-form"
          onSubmit={(event) => {
            event.preventDefault();
            setMessage("");
            updateProfile.mutate({ ...form, yearsExperience: Number(form.yearsExperience || 0) });
          }}
        >
          <h2>Thông tin gian hàng</h2>
          <label><span>Tên shop</span><input maxLength="120" value={form.shopName} onChange={update("shopName")} required /></label>
          <label><span>URL logo</span><input placeholder="https://..." value={form.logoUrl} onChange={update("logoUrl")} /></label>
          <label><span>URL ảnh bìa</span><input placeholder="https://..." value={form.heroUrl} onChange={update("heroUrl")} /></label>
          <label><span>Mô tả ngắn</span><textarea rows="3" value={form.description} onChange={update("description")} /></label>
          <label><span>Giới thiệu chi tiết</span><textarea rows="5" value={form.about} onChange={update("about")} /></label>
          <label><span>Vật liệu chủ đạo</span><input placeholder="Gỗ, gốm, vải, giấy..." value={form.materials} onChange={update("materials")} /></label>
          <label><span>Số năm kinh nghiệm</span><input type="number" min="0" max="100" value={form.yearsExperience} onChange={update("yearsExperience")} /></label>
          <button className="btn primary" disabled={updateProfile.isPending} type="submit"><Save size={17} /> Lưu hồ sơ shop</button>
        </form>

        <aside className="panel shop-profile-preview">
          <h2><Image size={18} /> Xem trước</h2>
          <div className="shop-preview-hero" style={form.heroUrl ? { backgroundImage: `url(${form.heroUrl})` } : undefined}>
            {form.logoUrl ? <img src={form.logoUrl} alt="Logo shop" /> : <span className="brand-mark">{form.shopName?.charAt(0) || "S"}</span>}
          </div>
          <h2>{form.shopName || "Tên gian hàng"}</h2>
          <p>{form.description || "Mô tả ngắn của gian hàng sẽ xuất hiện tại đây."}</p>
          <div className="shop-preview-meta">
            <span><strong>{form.yearsExperience || 0}</strong> năm kinh nghiệm</span>
            <span><strong>{form.materials || "Chưa cập nhật"}</strong> vật liệu</span>
          </div>
          <p className="muted">{form.about || "Phần giới thiệu chi tiết về câu chuyện và quy trình thủ công của shop."}</p>
        </aside>
      </section>
    </div>
  );
}
