import { useQuery } from "@tanstack/react-query";
import {
  BarChart3,
  Bell,
  Flag,
  Gift,
  Image,
  Loader2,
  MessageSquare,
  Percent,
  ReceiptText,
  Settings,
  ShoppingBag,
  Sparkles,
  Ticket
} from "lucide-react";
import { marketplaceApi } from "../api/marketplaceApi";
import { formatMoney } from "../utils/format";

export default function MarketplaceModulesPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["marketplace-modules"],
    queryFn: marketplaceApi.modules
  });

  if (isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Đang tải module plan...</section>;
  }

  if (error) {
    return <section className="alert error">{error.message}</section>;
  }

  const cards = [
    { title: "Voucher", icon: Ticket, count: data.vouchers.length, note: "Platform/seller voucher, min order, max discount, usage limit" },
    { title: "Flash sale", icon: Percent, count: data.flashSales.length, note: "Campaign, state, per-user limit, reserve/sold counters" },
    { title: "Gift wrap", icon: Gift, count: data.giftWrapTiers.length, note: "Tier goi qua, thiep, phi them tai checkout" },
    { title: "Commission board", icon: Sparkles, count: data.commissions.length, note: "Customer post va seller proposal" },
    { title: "Custom order", icon: ShoppingBag, count: data.customOrders.length, note: "Quote, review, crafting status, payment status" },
    { title: "Reports", icon: Flag, count: data.reports.length, note: "Moderation workflow cho shop/product/order/customer" },
    { title: "Reliability", icon: BarChart3, count: data.reliability.pendingTransactions, note: "Payment/ledger anomaly counters" },
    { title: "Settings", icon: Settings, count: Object.keys(data.settings || {}).length, note: "Platform name, commission bps, default shipping fee" }
  ];

  return (
    <div className="modules-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><Sparkles size={16} /> Full plan coverage</span>
          <h1>Marketplace Handmade Modules</h1>
          <p className="muted">Cac module trong file plan da co schema, seed data va REST endpoint de demo.</p>
        </div>
      </section>

      <section className="module-grid">
        {cards.map(({ title, icon: Icon, count, note }) => (
          <article className="module-card" key={title}>
            <Icon size={20} />
            <strong>{title}</strong>
            <span>{count}</span>
            <p>{note}</p>
          </article>
        ))}
      </section>

      <section className="content-grid">
        <article className="panel">
          <h2><Ticket size={18} /> Voucher public</h2>
          <div className="stack-list">
            {data.vouchers.map((voucher) => (
              <div className="soft-row" key={voucher.id}>
                <strong>{voucher.code} - {voucher.title}</strong>
                <span>{voucher.scope} - giam {voucher.discountPercent}% toi da {formatMoney(voucher.maxDiscountAmount)}</span>
                <small className="muted">Min order {formatMoney(voucher.minOrderAmount)} - used {voucher.usedCount}/{voucher.usageLimit}</small>
              </div>
            ))}
          </div>
        </article>

        <article className="panel">
          <h2><Percent size={18} /> Flash sale</h2>
          <div className="stack-list">
            {data.flashSales.map((sale) => (
              <div className="soft-row" key={sale.id}>
                <strong>{sale.name} - {sale.state}</strong>
                <span>{sale.description}</span>
                <small className="muted">Giam {sale.discountPercent}% - sold {sale.soldUnits}/{sale.maxUnits}, reserved {sale.reservedUnits}</small>
              </div>
            ))}
          </div>
        </article>
      </section>

      <section className="content-grid">
        <article className="panel">
          <h2><MessageSquare size={18} /> Custom order & commission</h2>
          <div className="stack-list">
            {data.customOrders.map((order) => (
              <div className="soft-row" key={`custom-${order.id}`}>
                <strong>{order.title} - {order.status}</strong>
                <span>{formatMoney(order.price)} - payment {order.paymentStatus}</span>
              </div>
            ))}
            {data.commissions.map((post) => (
              <div className="soft-row" key={`commission-${post.id}`}>
                <strong>{post.title} - {post.status}</strong>
                <span>{formatMoney(post.budgetMin)} - {formatMoney(post.budgetMax)} / {post.desiredTimeline}</span>
              </div>
            ))}
          </div>
        </article>

        <article className="panel">
          <h2><Flag size={18} /> Reports & reliability</h2>
          <div className="stack-list">
            {data.reports.map((report) => (
              <div className="soft-row" key={report.id}>
                <strong>{report.type} #{report.targetId} - {report.status}</strong>
                <span>{report.reason}</span>
              </div>
            ))}
            <div className="soft-row">
              <strong><ReceiptText size={15} /> Payment reliability</strong>
              <span>Pending tx: {data.reliability.pendingTransactions}, failed: {data.reliability.failedPayments}</span>
              <span>Paid missing ledger: {data.reliability.paidMissingLedger}, ledger entries: {data.reliability.ledgerEntries}</span>
            </div>
          </div>
        </article>
      </section>

      <section className="content-grid">
        <article className="panel">
          <h2><Gift size={18} /> Gift wrap tiers</h2>
          <div className="stack-list">
            {data.giftWrapTiers.map((tier) => (
              <div className="soft-row" key={tier.id}>
                <strong>{tier.name} - {formatMoney(tier.price)}</strong>
                <span>{tier.description}</span>
              </div>
            ))}
          </div>
        </article>
        <article className="panel">
          <h2><Settings size={18} /> Platform settings</h2>
          <div className="stack-list">
            {Object.entries(data.settings || {}).map(([key, value]) => (
              <div className="soft-row" key={key}>
                <strong>{key}</strong>
                <span>{value}</span>
              </div>
            ))}
          </div>
        </article>
      </section>

      <section className="module-strip">
        <span><Bell size={16} /> Notification DB + unread API</span>
        <span><Image size={16} /> Media library endpoints</span>
        <span><MessageSquare size={16} /> Chat conversations/messages</span>
        <span><BarChart3 size={16} /> Seller analytics endpoint</span>
      </section>
    </div>
  );
}
