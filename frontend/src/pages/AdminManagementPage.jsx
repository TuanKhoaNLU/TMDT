import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BarChart3, CheckCircle2, Loader2, MessageSquareReply, Settings, ShieldCheck, UsersRound } from "lucide-react";
import { useState } from "react";
import { marketplaceApi } from "../api/marketplaceApi";
import { formatMoney } from "../utils/format";

export default function AdminManagementPage() {
  const queryClient = useQueryClient();
  const [userForm, setUserForm] = useState({ username: "", fullName: "", email: "", phone: "", role: "BUYER", status: "ACTIVE" });
  const [settingsText, setSettingsText] = useState("");

  const users = useQuery({ queryKey: ["admin-users"], queryFn: marketplaceApi.adminUsers });
  const reports = useQuery({ queryKey: ["admin-reports"], queryFn: marketplaceApi.adminReports });
  const reviews = useQuery({ queryKey: ["admin-reviews"], queryFn: marketplaceApi.adminReviews });
  const settings = useQuery({ queryKey: ["platform-settings"], queryFn: marketplaceApi.platformSettings });
  const reliability = useQuery({ queryKey: ["payment-reliability"], queryFn: marketplaceApi.paymentReliability });

  const createUser = useMutation({
    mutationFn: marketplaceApi.createAdminUser,
    onSuccess: () => {
      setUserForm({ username: "", fullName: "", email: "", phone: "", role: "BUYER", status: "ACTIVE" });
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
    }
  });
  const verifySeller = useMutation({
    mutationFn: (userId) => marketplaceApi.verifySeller(userId, true),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-users"] })
  });
  const replyReview = useMutation({
    mutationFn: ({ reviewId, reply }) => marketplaceApi.adminReplyReview(reviewId, { reply }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-reviews"] })
  });
  const updateReport = useMutation({
    mutationFn: ({ reportId, status }) => marketplaceApi.updateReport(reportId, { status, adminNote: "Xu ly tu admin dashboard" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-reports"] })
  });
  const saveSettings = useMutation({
    mutationFn: () => marketplaceApi.updatePlatformSettings(parseSettings(settingsText)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["platform-settings"] })
  });

  if (users.isLoading || reports.isLoading || reviews.isLoading || settings.isLoading || reliability.isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Dang tai admin management...</section>;
  }

  return (
    <div className="dashboard-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><ShieldCheck size={16} /> Admin Management</span>
          <h1>Quan ly marketplace</h1>
          <p className="muted">Users, reports, reviews, settings va payment reliability.</p>
        </div>
      </section>

      <section className="content-grid">
        <article className="panel">
          <h2><UsersRound size={18} /> Users</h2>
          <form
            className="catalog-form compact-admin-form"
            onSubmit={(event) => {
              event.preventDefault();
              createUser.mutate(userForm);
            }}
          >
            <input placeholder="Username" value={userForm.username} onChange={(e) => setUserForm({ ...userForm, username: e.target.value })} />
            <input placeholder="Full name" value={userForm.fullName} onChange={(e) => setUserForm({ ...userForm, fullName: e.target.value })} />
            <input placeholder="Email" value={userForm.email} onChange={(e) => setUserForm({ ...userForm, email: e.target.value })} />
            <select value={userForm.role} onChange={(e) => setUserForm({ ...userForm, role: e.target.value })}>
              <option value="BUYER">BUYER</option>
              <option value="SELLER">SELLER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
            <button className="btn primary" type="submit">Tao user</button>
          </form>
          <div className="stack-list">
            {(users.data || []).map((user) => (
              <div className="soft-row" key={user.id}>
                <strong>{user.username} - {user.role} - {user.status}</strong>
                <span>{user.fullName} - spent {formatMoney(user.totalSpent)} - sales {formatMoney(user.sales)}</span>
                {user.role === "SELLER" && (
                  <button className="btn secondary" onClick={() => verifySeller.mutate(user.id)} type="button">
                    <CheckCircle2 size={16} /> Verify artisan
                  </button>
                )}
              </div>
            ))}
          </div>
        </article>

        <article className="panel">
          <h2><BarChart3 size={18} /> Payment reliability</h2>
          <div className="metric-grid compact">
            <div><strong>{reliability.data.paidMissingLedger}</strong><span>Paid missing ledger</span></div>
            <div><strong>{reliability.data.pendingTransactions}</strong><span>Pending tx</span></div>
            <div><strong>{reliability.data.failedPayments}</strong><span>Failed payments</span></div>
            <div><strong>{reliability.data.ledgerEntries}</strong><span>Ledger entries</span></div>
          </div>
        </article>
      </section>

      <section className="content-grid">
        <article className="panel">
          <h2>Reports</h2>
          <div className="stack-list">
            {(reports.data || []).map((report) => (
              <div className="soft-row" key={report.id}>
                <strong>{report.type} #{report.targetId} - {report.status}</strong>
                <span>{report.reason}</span>
                <div className="detail-actions">
                  <button className="btn secondary" onClick={() => updateReport.mutate({ reportId: report.id, status: "RESOLVED" })} type="button">Resolve</button>
                  <button className="btn secondary" onClick={() => updateReport.mutate({ reportId: report.id, status: "REJECTED" })} type="button">Reject</button>
                </div>
              </div>
            ))}
          </div>
        </article>

        <article className="panel">
          <h2><MessageSquareReply size={18} /> Reviews</h2>
          <div className="stack-list">
            {(reviews.data || []).map((review) => (
              <div className="soft-row" key={review.id}>
                <strong>{review.productName} - {review.rating}/5</strong>
                <span>{review.comment}</span>
                <small className="muted">Shop reply: {review.sellerReply || "Chua co"}</small>
                <button className="btn secondary" onClick={() => replyReview.mutate({ reviewId: review.id, reply: "Cam on ban da danh gia san pham." })} type="button">
                  Reply mau
                </button>
              </div>
            ))}
          </div>
        </article>
      </section>

      <section className="panel">
        <h2><Settings size={18} /> Platform settings</h2>
        <textarea
          className="settings-editor"
          value={settingsText || stringifySettings(settings.data)}
          onChange={(event) => setSettingsText(event.target.value)}
        />
        <button className="btn primary" onClick={() => saveSettings.mutate()} type="button">Luu settings</button>
      </section>
    </div>
  );
}

function stringifySettings(settings = {}) {
  return Object.entries(settings).map(([key, value]) => `${key}=${value}`).join("\n");
}

function parseSettings(raw) {
  return raw.split("\n").reduce((acc, line) => {
    const [key, ...rest] = line.split("=");
    if (key?.trim()) acc[key.trim()] = rest.join("=").trim();
    return acc;
  }, {});
}
