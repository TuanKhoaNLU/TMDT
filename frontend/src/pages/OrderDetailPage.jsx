import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ArrowLeft,
  CalendarDays,
  CreditCard,
  Loader2,
  MapPin,
  PackageCheck,
  ReceiptText,
  Store,
  Truck,
  XCircle
} from "lucide-react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { marketplaceApi } from "../api/marketplaceApi";
import { StatusBadge } from "../components/StatusBadge";
import { formatDate, formatMoney } from "../utils/format";

const orderSteps = [
  { status: "PENDING_PAYMENT", label: "Cho thanh toan" },
  { status: "PROCESSING", label: "Shop xu ly" },
  { status: "SHIPPING", label: "Dang giao" },
  { status: "COMPLETED", label: "Hoan tat" }
];

function stepClass(currentStatus, stepStatus) {
  if (currentStatus === "CANCELLED") return "";
  const currentIndex = orderSteps.findIndex((step) => step.status === currentStatus);
  const stepIndex = orderSteps.findIndex((step) => step.status === stepStatus);
  return currentIndex >= stepIndex ? "active" : "";
}

export default function OrderDetailPage() {
  const { orderId } = useParams();
  const queryClient = useQueryClient();
  const [actionError, setActionError] = useState("");
  const { data: order, isLoading, error } = useQuery({
    queryKey: ["order-detail", orderId],
    queryFn: () => marketplaceApi.orderDetail(orderId),
    enabled: Boolean(orderId)
  });
  const cancelOrder = useMutation({
    mutationFn: () => marketplaceApi.cancelOrder(order.id),
    onMutate: () => setActionError(""),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["order-detail", orderId] });
      queryClient.invalidateQueries({ queryKey: ["buyer-orders"] });
    },
    onError: (err) => setActionError(err.message)
  });
  const cancelShopOrder = useMutation({
    mutationFn: (shopOrderId) => marketplaceApi.cancelShopOrder(order.id, shopOrderId),
    onMutate: () => setActionError(""),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["order-detail", orderId] });
      queryClient.invalidateQueries({ queryKey: ["buyer-orders"] });
    },
    onError: (err) => setActionError(err.message)
  });

  async function payAgain() {
    localStorage.setItem("pendingPaymentOrderId", order.id);
    const payment = await marketplaceApi.createVnpayPayment(order.id);
    window.location.href = payment.paymentUrl;
  }

  if (isLoading) {
    return (
      <section className="loading-panel">
        <Loader2 className="spin" size={22} /> Dang tai chi tiet don...
      </section>
    );
  }

  if (error) {
    return <section className="alert error">{error.message}</section>;
  }

  const canPayAgain =
    order.paymentMethod === "VNPAY" &&
    order.status === "PENDING_PAYMENT" &&
    order.paymentStatus === "PENDING";
  const canCancel =
    order.paymentStatus !== "PAID" &&
    ["PENDING_PAYMENT", "PROCESSING"].includes(order.status) &&
    order.shopOrders.every((shopOrder) => ["PENDING_PAYMENT", "NEW", "CONFIRMED"].includes(shopOrder.status));
  const multiShop = order.shopOrders.length > 1;

  return (
    <div className="stack">
      {actionError && <section className="alert error">{actionError}</section>}
      <section className="detail-panel order-detail-hero">
        <div className="order-title-row">
          <div>
            <Link className="text-link" to="/orders">
              <ArrowLeft size={16} /> Ve lich su don
            </Link>
            <h1>Order #{order.id}</h1>
            <p className="muted">
              <CalendarDays size={15} /> {formatDate(order.createdAt)}
            </p>
          </div>
          <div className="badge-row">
            <StatusBadge status={order.status} />
            <StatusBadge status={order.paymentStatus} />
          </div>
        </div>

        <div className="order-timeline">
          {orderSteps.map((step) => (
            <div className={`timeline-step ${stepClass(order.status, step.status)}`} key={step.status}>
              <span />
              <strong>{step.label}</strong>
            </div>
          ))}
          {order.status === "CANCELLED" && (
            <div className="timeline-step cancelled active">
              <span />
              <strong>Da huy</strong>
            </div>
          )}
        </div>

        <div className="detail-grid">
          <div className="detail-card">
            <MapPin size={20} />
            <span>Nguoi nhan</span>
            <strong>{order.receiverName}</strong>
            <p>{order.receiverPhone}</p>
            <p>{order.shippingAddress}</p>
          </div>
          <div className="detail-card">
            <CreditCard size={20} />
            <span>Thanh toan</span>
            <strong>{order.paymentMethod}</strong>
            <p>{order.paymentMethod === "COD" ? "Thu ho khi giao hang" : "Cong thanh toan VNPay"}</p>
            <StatusBadge status={order.paymentStatus} />
          </div>
          <div className="detail-card total-detail">
            <ReceiptText size={20} />
            <span>Tong don</span>
            <strong>{formatMoney(order.total)}</strong>
            <p>Hang {formatMoney(order.subtotal)} · Ship {formatMoney(order.shippingFee)}</p>
          </div>
        </div>
        <div className="card-actions">
          {canPayAgain && (
            <button className="btn primary" onClick={payAgain}>
              <CreditCard size={18} /> Thanh toan lai
            </button>
          )}
          {canCancel && (
            <button className="btn secondary" disabled={cancelOrder.isPending} onClick={() => cancelOrder.mutate()}>
              <XCircle size={18} /> {multiShop ? "Huy toan bo don" : "Huy don"}
            </button>
          )}
        </div>
      </section>

      {order.shopOrders.map((shopOrder) => {
        const canCancelPackage =
          multiShop &&
          order.paymentStatus !== "PAID" &&
          !["CANCELLED", "COMPLETED"].includes(order.status) &&
          ["PENDING_PAYMENT", "NEW", "CONFIRMED"].includes(shopOrder.status);
        return (
          <section className="shop-panel package-card" key={shopOrder.id}>
            <div className="package-head">
              <div>
                <span className="shop-name">
                  <Store size={16} /> {shopOrder.shopName}
                </span>
                <h2>Goi hang #{shopOrder.id}</h2>
              </div>
              <div className="package-head-actions">
                <StatusBadge status={shopOrder.status} />
                {canCancelPackage && (
                  <button
                    className="btn secondary"
                    disabled={cancelShopOrder.isPending}
                    onClick={() => cancelShopOrder.mutate(shopOrder.id)}
                  >
                    <XCircle size={17} /> Huy goi nay
                  </button>
                )}
              </div>
            </div>

            <div className="package-items">
              {shopOrder.items.map((item) => (
                <div className="package-item" key={item.id}>
                  <div>
                    <h3>{item.productName}</h3>
                    <p className="muted">
                      {item.quantity} x {formatMoney(item.unitPrice)}
                    </p>
                    {item.note && <p className="muted">{item.note}</p>}
                  </div>
                  <strong>{formatMoney(item.lineTotal)}</strong>
                </div>
              ))}
            </div>

            <div className="package-summary">
              <div className="summary-row">
                <span>Tien hang</span>
                <strong>{formatMoney(shopOrder.itemSubtotal)}</strong>
              </div>
              <div className="summary-row">
                <span>Phi ship GHN</span>
                <strong>{formatMoney(shopOrder.shippingFee)}</strong>
              </div>
              <div className="summary-row">
                <span>COD shop</span>
                <strong>{formatMoney(shopOrder.codAmount)}</strong>
              </div>
            </div>

            {shopOrder.shipments.length ? (
              shopOrder.shipments.map((shipment) => (
                <div className="shipment-row shipment-track" key={shipment.id}>
                  <Truck size={18} />
                  <span>{shipment.serviceName || "GHN Standard"}</span>
                  <strong>{shipment.trackingCode || "Dang tao ma van don"}</strong>
                  <StatusBadge status={shipment.status} />
                </div>
              ))
            ) : (
              <div className="shipment-row shipment-track muted">
                <Truck size={18} />
                <span>Shop chua tao van don GHN</span>
              </div>
            )}
          </section>
        );
      })}
    </div>
  );
}
