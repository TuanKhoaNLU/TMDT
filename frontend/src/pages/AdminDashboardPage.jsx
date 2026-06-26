import { useQuery } from "@tanstack/react-query";
import { BarChart3, Loader2, Package, ReceiptText, ShieldCheck, Star, Store, UsersRound } from "lucide-react";
import { Link } from "react-router-dom";
import { marketplaceApi } from "../api/marketplaceApi";
import { formatMoney } from "../utils/format";

export default function AdminDashboardPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["admin-dashboard"],
    queryFn: marketplaceApi.adminDashboard
  });

  if (isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Đang tải admin...</section>;
  }

  const stats = [
    ["Users", data.users, UsersRound],
    ["Shops", data.shops, Store],
    ["Products", data.products, Package],
    ["Orders", data.orders, ReceiptText],
    ["Reviews", data.reviews, Star],
    ["Q&A pending", data.pendingQuestions, ShieldCheck]
  ];

  return (
    <div className="dashboard-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><ShieldCheck size={16} /> Admin Dashboard</span>
          <h1>Tổng quan marketplace</h1>
          <p className="muted">Số liệu vận hành, doanh thu và moderation theo kế hoạch.</p>
        </div>
        <div className="detail-actions">
          <Link className="btn secondary" to="/admin/manage">Quản lý tổng</Link>
          <Link className="btn secondary" to="/admin/catalog">Duyệt catalog</Link>
          <Link className="btn primary" to="/admin/orders">Quản lý đơn hàng</Link>
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
          <span>Revenue</span>
        </div>
      </section>
    </div>
  );
}
