import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BarChart3, CheckCircle2, Loader2, MessageSquareReply, Percent, Settings, ShieldCheck, UsersRound } from "lucide-react";
import { useEffect, useState } from "react";
import { marketplaceApi } from "../api/marketplaceApi";
import { formatMoney } from "../utils/format";

export default function AdminManagementPage() {
  const queryClient = useQueryClient();
  const [userForm, setUserForm] = useState({ username: "", fullName: "", email: "", phone: "", role: "BUYER", status: "ACTIVE" });
  const [platformForm, setPlatformForm] = useState({ platformName: "", commissionPercent: "", defaultShippingFee: "" });
  const [actionError, setActionError] = useState("");
  const users = useQuery({ queryKey: ["admin-users"], queryFn: marketplaceApi.adminUsers });
  const reports = useQuery({ queryKey: ["admin-reports"], queryFn: marketplaceApi.adminReports });
  const reviews = useQuery({ queryKey: ["admin-reviews"], queryFn: marketplaceApi.adminReviews });
  const settings = useQuery({ queryKey: ["platform-settings"], queryFn: marketplaceApi.platformSettings });
  const reliability = useQuery({ queryKey: ["payment-reliability"], queryFn: marketplaceApi.paymentReliability });

  useEffect(() => {
    if (settings.data) {
      setPlatformForm({
        platformName: settings.data.platform_name || "TMDT Market",
        commissionPercent: String(Number(settings.data.commission_bps || 1000) / 100),
        defaultShippingFee: settings.data.default_shipping_fee || "25000"
      });
    }
  }, [settings.data]);

  const createUser = useMutation({
    mutationFn: marketplaceApi.createAdminUser,
    onSuccess: () => { setUserForm({ username: "", fullName: "", email: "", phone: "", role: "BUYER", status: "ACTIVE" }); queryClient.invalidateQueries({ queryKey: ["admin-users"] }); },
    onError: (err) => setActionError(err.message)
  });
  const verifySeller = useMutation({ mutationFn: (userId) => marketplaceApi.verifySeller(userId, true), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-users"] }), onError: (err) => setActionError(err.message) });
  const replyReview = useMutation({ mutationFn: ({ reviewId, reply }) => marketplaceApi.adminReplyReview(reviewId, { reply }), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-reviews"] }), onError: (err) => setActionError(err.message) });
  const updateReport = useMutation({ mutationFn: ({ reportId, status }) => marketplaceApi.updateReport(reportId, { status, adminNote: "Xử lý từ trang quản trị" }), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-reports"] }), onError: (err) => setActionError(err.message) });
  const saveSettings = useMutation({
    mutationFn: () => marketplaceApi.updatePlatformSettings({
      platform_name: platformForm.platformName,
      commission_bps: String(Math.round(Number(platformForm.commissionPercent || 0) * 100)),
      default_shipping_fee: String(Number(platformForm.defaultShippingFee || 0))
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["platform-settings"] }),
    onError: (err) => setActionError(err.message)
  });

  if (users.isLoading || reports.isLoading || reviews.isLoading || settings.isLoading || reliability.isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Đang tải quản trị...</section>;
  }

  return (
    <div className="dashboard-page">
      <section className="page-head">
        <div><span className="section-kicker"><ShieldCheck size={16} /> Quản trị</span><h1>Quản lý marketplace</h1><p className="muted">Tài khoản, báo cáo, đánh giá, phí nền tảng và độ tin cậy thanh toán.</p></div>
      </section>
      {actionError && <section className="alert error">{actionError}</section>}

      <section className="content-grid">
        <article className="panel">
          <h2><UsersRound size={18} /> Tài khoản</h2>
          <form className="catalog-form compact-admin-form" onSubmit={(event) => { event.preventDefault(); createUser.mutate(userForm); }}>
            <input placeholder="Tên đăng nhập" value={userForm.username} onChange={(e) => setUserForm({ ...userForm, username: e.target.value })} required />
            <input placeholder="Họ tên" value={userForm.fullName} onChange={(e) => setUserForm({ ...userForm, fullName: e.target.value })} required />
            <input type="email" placeholder="Email" value={userForm.email} onChange={(e) => setUserForm({ ...userForm, email: e.target.value })} />
            <select value={userForm.role} onChange={(e) => setUserForm({ ...userForm, role: e.target.value })}><option value="BUYER">Khách hàng</option><option value="SELLER">Người bán</option><option value="ADMIN">Quản trị viên</option></select>
            <button className="btn primary" type="submit">Tạo tài khoản</button>
          </form>
          <div className="stack-list">
            {(users.data || []).map((user) => (
              <div className="soft-row" key={user.id}>
                <strong>{user.username} · {user.role} · {user.status}</strong>
                <span>{user.fullName} · Chi tiêu {formatMoney(user.totalSpent)} · Doanh số {formatMoney(user.sales)}</span>
                {user.role === "SELLER" && user.status !== "ACTIVE" && <button className="btn secondary" onClick={() => verifySeller.mutate(user.id)} type="button"><CheckCircle2 size={16} /> Duyệt seller</button>}
              </div>
            ))}
          </div>
        </article>

        <article className="panel">
          <h2><BarChart3 size={18} /> Độ tin cậy thanh toán</h2>
          <div className="metric-grid compact">
            <div><strong>{reliability.data.paidMissingLedger}</strong><span>Đã trả thiếu ledger</span></div>
            <div><strong>{reliability.data.pendingTransactions}</strong><span>Đang chờ</span></div>
            <div><strong>{reliability.data.failedPayments}</strong><span>Thất bại</span></div>
            <div><strong>{reliability.data.ledgerEntries}</strong><span>Bút toán</span></div>
          </div>
        </article>
      </section>

      <section className="panel platform-fee-panel">
        <div className="section-title-row">
          <div><h2><Percent size={18} /> Phí nền tảng</h2><p className="muted">Mức phí này được áp dụng cho các đơn mới ngay sau khi lưu.</p></div>
          <strong className="fee-preview">{Number(platformForm.commissionPercent || 0).toFixed(2)}%</strong>
        </div>
        <form className="settings-form" onSubmit={(event) => { event.preventDefault(); saveSettings.mutate(); }}>
          <label><span>Tên nền tảng</span><input value={platformForm.platformName} onChange={(e) => setPlatformForm({ ...platformForm, platformName: e.target.value })} required /></label>
          <label><span>Phí nền tảng (%)</span><input type="number" min="0" max="50" step="0.01" value={platformForm.commissionPercent} onChange={(e) => setPlatformForm({ ...platformForm, commissionPercent: e.target.value })} required /></label>
          <label><span>Phí giao hàng mặc định</span><input type="number" min="0" value={platformForm.defaultShippingFee} onChange={(e) => setPlatformForm({ ...platformForm, defaultShippingFee: e.target.value })} required /></label>
          <button className="btn primary" disabled={saveSettings.isPending} type="submit"><Settings size={17} /> Lưu cấu hình</button>
        </form>
        {saveSettings.isSuccess && <p className="success">Đã cập nhật phí nền tảng.</p>}
      </section>

      <section className="content-grid">
        <article className="panel">
          <h2>Báo cáo vi phạm</h2>
          <div className="stack-list">{(reports.data || []).map((report) => <div className="soft-row" key={report.id}><strong>{report.type} #{report.targetId} · {report.status}</strong><span>{report.reason}</span><div className="detail-actions"><button className="btn secondary" onClick={() => updateReport.mutate({ reportId: report.id, status: "RESOLVED" })} type="button">Đã xử lý</button><button className="btn secondary" onClick={() => updateReport.mutate({ reportId: report.id, status: "REJECTED" })} type="button">Từ chối</button></div></div>)}</div>
        </article>
        <article className="panel">
          <h2><MessageSquareReply size={18} /> Đánh giá</h2>
          <div className="stack-list">{(reviews.data || []).map((review) => <div className="soft-row" key={review.id}><strong>{review.productName} · {review.rating}/5</strong><span>{review.comment}</span><small className="muted">Phản hồi: {review.sellerReply || "Chưa có"}</small><button className="btn secondary" onClick={() => replyReview.mutate({ reviewId: review.id, reply: "Cảm ơn bạn đã đánh giá sản phẩm." })} type="button">Phản hồi mẫu</button></div>)}</div>
        </article>
      </section>
    </div>
  );
}
