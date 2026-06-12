import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  CalendarDays,
  CreditCard,
  Eye,
  Loader2,
  MapPin,
  PackageCheck,
  ReceiptText,
  Search,
  Store,
  Truck,
  XCircle
} from "lucide-react";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { marketplaceApi } from "../api/marketplaceApi";
import { EmptyState } from "../components/EmptyState";
import { StatusBadge } from "../components/StatusBadge";
import { formatDate, formatMoney } from "../utils/format";

const filters = [
  { value: "ALL", label: "Tat ca" },
  { value: "PENDING_PAYMENT", label: "Cho thanh toan" },
  { value: "PROCESSING", label: "Dang xu ly" },
  { value: "SHIPPING", label: "Dang giao" },
  { value: "COMPLETED", label: "Hoan tat" },
  { value: "CANCELLED", label: "Da huy" }
];

export default function OrdersPage() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState("ALL");
  const [actionError, setActionError] = useState("");
  const { data: orders = [], isLoading, error } = useQuery({
    queryKey: ["buyer-orders"],
    queryFn: marketplaceApi.buyerOrders
  });
  const cancelOrder = useMutation({
    mutationFn: marketplaceApi.cancelOrder,
    onMutate: () => setActionError(""),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["buyer-orders"] }),
    onError: (err) => setActionError(err.message)
  });

  const visibleOrders = useMemo(() => {
    if (filter === "ALL") return orders;
    return orders.filter((order) => order.status === filter || order.paymentStatus === filter);
  }, [filter, orders]);

  const filterCounts = useMemo(
    () =>
      filters.reduce((result, item) => {
        result[item.value] =
          item.value === "ALL"
            ? orders.length
            : orders.filter((order) => order.status === item.value || order.paymentStatus === item.value).length;
        return result;
      }, {}),
    [orders]
  );

  const stats = useMemo(
    () => [
      { label: "Tong don", value: orders.length, icon: ReceiptText },
      {
        label: "Cho thanh toan",
        value: orders.filter((order) => order.status === "PENDING_PAYMENT" || order.paymentStatus === "PENDING").length,
        icon: CreditCard
      },
      { label: "Dang giao", value: orders.filter((order) => order.status === "SHIPPING").length, icon: Truck },
      { label: "Hoan tat", value: orders.filter((order) => order.status === "COMPLETED").length, icon: PackageCheck }
    ],
    [orders]
  );

  async function payAgain(orderId) {
    localStorage.setItem("pendingPaymentOrderId", orderId);
    const payment = await marketplaceApi.createVnpayPayment(orderId);
    window.location.href = payment.paymentUrl;
  }

  if (isLoading) {
    return (
      <section className="loading-panel">
        <Loader2 className="spin" size={22} /> Dang tai don hang...
      </section>
    );
  }

  if (error) {
    return <section className="alert error">{error.message}</section>;
  }

  return (
    <div className="stack">
      <section className="order-dashboard">
        <div className="order-dashboard-copy">
          <span className="section-kicker">Buyer center</span>
          <h1>Don mua</h1>
          <p className="muted">Theo doi thanh toan, goi hang theo shop va trang thai giao hang trong mot man hinh.</p>
        </div>
        <div className="order-stat-grid">
          {stats.map((stat) => {
            const Icon = stat.icon;
            return (
              <div className="order-stat" key={stat.label}>
                <Icon size={20} />
                <span>{stat.label}</span>
                <strong>{stat.value}</strong>
              </div>
            );
          })}
        </div>
      </section>

      <div className="filter-bar order-tabs">
        {filters.map((item) => (
          <button
            className={`btn ${filter === item.value ? "primary" : "secondary"}`}
            key={item.value}
            onClick={() => setFilter(item.value)}
          >
            {item.label} <span className="filter-count">{filterCounts[item.value] || 0}</span>
          </button>
        ))}
        <span className="order-search-note">
          <Search size={16} /> Lich su gan nhat
        </span>
      </div>
      {actionError && <section className="alert error">{actionError}</section>}

      {!visibleOrders.length ? (
        <EmptyState title="Chua co don phu hop" />
      ) : (
        visibleOrders.map((order) => {
          const canPayAgain =
            order.paymentMethod === "VNPAY" &&
            order.status === "PENDING_PAYMENT" &&
            order.paymentStatus === "PENDING";
          const canCancel =
            order.paymentStatus !== "PAID" &&
            ["PENDING_PAYMENT", "PROCESSING"].includes(order.status) &&
            order.shopCount === 1;
          const canManagePackageCancel =
            order.paymentStatus !== "PAID" &&
            ["PENDING_PAYMENT", "PROCESSING"].includes(order.status) &&
            order.shopCount > 1;
          return (
            <article className="order-card market-order" key={order.id}>
              <div className="order-main">
                <div className="order-title-row">
                  <div>
                    <h2>Order #{order.id}</h2>
                    <p className="muted">
                      <CalendarDays size={15} /> {formatDate(order.createdAt)}
                    </p>
                  </div>
                  <div className="badge-row">
                    <StatusBadge status={order.status} />
                    <StatusBadge status={order.paymentStatus} />
                  </div>
                </div>
                <div className="order-meta-grid">
                  <span>
                    <MapPin size={16} /> {order.receiverName}
                  </span>
                  <span>
                    <Store size={16} /> {order.shopCount} shop
                  </span>
                  <span>
                    <PackageCheck size={16} /> {order.itemCount} san pham
                  </span>
                  <span>
                    <CreditCard size={16} /> {order.paymentMethod}
                  </span>
                </div>
              </div>
              <div className="order-totals total-box">
                <span>Tong thanh toan</span>
                <strong>{formatMoney(order.total)}</strong>
                <small>{order.paymentMethod === "COD" ? "Thu ho khi giao" : "Cong thanh toan VNPay"}</small>
              </div>
              <div className="card-actions order-actions">
                {canPayAgain && (
                  <button className="btn primary" onClick={() => payAgain(order.id)}>
                    <CreditCard size={17} /> Thanh toan lai
                  </button>
                )}
                {canCancel && (
                  <button
                    className="btn secondary"
                    disabled={cancelOrder.isPending}
                    onClick={() => cancelOrder.mutate(order.id)}
                  >
                    <XCircle size={17} /> Huy don
                  </button>
                )}
                {canManagePackageCancel && (
                  <Link className="btn secondary" to={`/orders/${order.id}`}>
                    <XCircle size={17} /> Huy theo shop
                  </Link>
                )}
                <Link className="btn secondary" to={`/orders/${order.id}`}>
                  <Eye size={17} /> Chi tiet
                </Link>
              </div>
            </article>
          );
        })
      )}
    </div>
  );
}
