import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, BarChart3, Image, Loader2, MessageSquare, Package, ShoppingBag, Star, Store, Ticket } from "lucide-react";
import { Link } from "react-router-dom";
import { marketplaceApi } from "../api/marketplaceApi";
import { formatMoney } from "../utils/format";

export default function SellerDashboardPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["seller-dashboard"],
    queryFn: marketplaceApi.sellerDashboard
  });
  const analytics = useQuery({ queryKey: ["seller-analytics"], queryFn: marketplaceApi.sellerAnalytics });
  const conversations = useQuery({ queryKey: ["chat-conversations"], queryFn: marketplaceApi.conversations });
  const customOrders = useQuery({ queryKey: ["custom-orders"], queryFn: marketplaceApi.customOrders });
  const media = useQuery({ queryKey: ["media-folders"], queryFn: marketplaceApi.mediaFolders });

  if (isLoading || analytics.isLoading || conversations.isLoading || customOrders.isLoading || media.isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Dang tai kenh ban...</section>;
  }

  const stats = [
    ["San pham", data.products, Package],
    ["Sap het hang", data.lowStockProducts, AlertTriangle],
    ["Don shop", data.orders, Store],
    ["Review", data.reviews, Star],
    ["Cau hoi cho tra loi", data.pendingQuestions, BarChart3]
  ];

  return (
    <div className="dashboard-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><Store size={16} /> Seller Center</span>
          <h1>Tong quan shop #{data.shopId}</h1>
          <p className="muted">San pham, ton kho, review va don hang theo shop.</p>
        </div>
        <div className="detail-actions">
          <Link className="btn secondary" to="/seller/products">Quan ly san pham</Link>
          <Link className="btn primary" to="/seller/orders">Quan ly don</Link>
        </div>
      </section>
      <section className="metric-grid">
        {stats.map(([label, value, Icon]) => (
          <div key={label}>
            <Icon size={18} />
            <strong>{value}</strong>
            <span>{label}</span>
          </div>
        ))}
        <div>
          <BarChart3 size={18} />
          <strong>{formatMoney(data.revenue)}</strong>
          <span>Doanh thu hang</span>
        </div>
      </section>

      <section className="module-grid">
        <Link className="module-card" to="/seller/products"><Package size={20} /><strong>Quan ly san pham</strong><span>{data.products}</span><p>Listing, inventory, low stock</p></Link>
        <Link className="module-card" to="/seller/orders"><Store size={20} /><strong>Orders</strong><span>{data.orders}</span><p>Cap nhat kien hang theo shop</p></Link>
        <Link className="module-card" to="/chat"><MessageSquare size={20} /><strong>Chat</strong><span>{conversations.data?.length || 0}</span><p>Tra loi customer va gui quote</p></Link>
        <Link className="module-card" to="/seller/custom-orders"><ShoppingBag size={20} /><strong>Custom orders</strong><span>{customOrders.data?.length || 0}</span><p>Theo doi crafting status</p></Link>
        <Link className="module-card" to="/seller/media"><Image size={20} /><strong>Media</strong><span>{media.data?.length || 0}</span><p>Folder anh cua shop</p></Link>
        <Link className="module-card" to="/modules"><Ticket size={20} /><strong>Marketing</strong><span>Voucher</span><p>Voucher, flash sale, shipping profile</p></Link>
      </section>

      <section className="content-grid">
        <article className="panel">
          <h2><BarChart3 size={18} /> Revenue by category</h2>
          <div className="stack-list">
            {(analytics.data?.revenueByCategory || []).map((point) => (
              <div className="soft-row" key={point.label}>
                <strong>{point.label}</strong>
                <span>{formatMoney(point.value)}</span>
              </div>
            ))}
            {!analytics.data?.revenueByCategory?.length && <p className="muted">Chua co doanh thu theo category.</p>}
          </div>
        </article>
        <article className="panel">
          <h2><Star size={18} /> Latest reviews</h2>
          <div className="stack-list">
            {(analytics.data?.latestReviews || []).map((review) => (
              <div className="soft-row" key={review.id}>
                <strong>{review.productName} - {review.rating}/5</strong>
                <span>{review.comment}</span>
                <small className="muted">{review.sellerReply || "Chua reply"}</small>
              </div>
            ))}
          </div>
        </article>
      </section>
    </div>
  );
}
