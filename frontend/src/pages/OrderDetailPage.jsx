import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, CalendarDays, CreditCard, Loader2, MapPin, PackageCheck, ReceiptText, Store, Truck, Undo2, XCircle } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { marketplaceApi } from "../api/marketplaceApi";
import { StatusBadge } from "../components/StatusBadge";
import { formatDate, formatMoney } from "../utils/format";

const orderSteps = [
  { status: "PENDING_PAYMENT", label: "Chờ thanh toán" },
  { status: "PROCESSING", label: "Shop xử lý" },
  { status: "SHIPPING", label: "Đang giao" },
  { status: "COMPLETED", label: "Hoàn tất" }
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
  const { data: order, isLoading, error } = useQuery({
    queryKey: ["order-detail", orderId],
    queryFn: () => marketplaceApi.orderDetail(orderId),
    enabled: Boolean(orderId)
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ["order-detail", orderId] });
    queryClient.invalidateQueries({ queryKey: ["buyer-orders"] });
    queryClient.invalidateQueries({ queryKey: ["buyer-returns"] });
  };

  const cancelOrder = useMutation({ mutationFn: () => marketplaceApi.cancelOrder(order.id), onSuccess: refresh });
  const cancelShopOrder = useMutation({ mutationFn: (shopOrderId) => marketplaceApi.cancelShopOrder(order.id, shopOrderId), onSuccess: refresh });
  const requestReturn = useMutation({
    mutationFn: (shopOrderId) => marketplaceApi.requestReturn(order.id, shopOrderId, { reason: "Khách hàng yêu cầu hoàn trả/hoàn tiền" }),
    onSuccess: refresh
  });

  async function payAgain() {
    localStorage.setItem("pendingPaymentOrderId", order.id);
    const payment = await marketplaceApi.createVnpayPayment(order.id);
    window.location.href = payment.paymentUrl;
  }

  if (isLoading) {
    return (
      <section className="loading-panel">
        <Loader2 className="spin" size={22} /> Đang tải chi tiết đơn...
      </section>
    );
  }

  if (error) return <section className="alert error">{error.message}</section>;

  const actionError = cancelOrder.error?.message || cancelShopOrder.error?.message || requestReturn.error?.message;
  const canPayAgain = order.paymentMethod === "VNPAY" && order.status === "PENDING_PAYMENT" && order.paymentStatus === "PENDING";
  const canCancel =
    order.paymentStatus !== "PAID" &&
    ["PENDING_PAYMENT", "PROCESSING"].includes(order.status) &&
    order.shopOrders.every((shopOrder) => ["PENDING_PAYMENT", "NEW", "CONFIRMED"].includes(shopOrder.status));
  const multiShop = order.shopOrders.length > 1;

  return (
    <div className="stack">
      {actionError && <section className="alert error">{actionError}</section>}
      {requestReturn.isSuccess && <section className="alert success">Đã gửi yêu cầu hoàn trả/hoàn tiền.</section>}

      <section className="detail-panel order-detail-hero">
        <div className="order-title-row">
          <div>
            <Link className="text-link" to="/orders">
              <ArrowLeft size={16} /> Về lịch sử đơn
            </Link>
            <h1>Đơn hàng #{order.id}</h1>
            <p className="muted"><CalendarDays size={15} /> {formatDate(order.createdAt)}</p>
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
          {order.status === "CANCELLED" && <div className="timeline-step cancelled active"><span /><strong>Đã hủy</strong></div>}
        </div>

        <div className="detail-grid">
          <div className="detail-card">
            <MapPin size={20} />
            <span>Người nhận</span>
            <strong>{order.receiverName}</strong>
            <p>{order.receiverPhone}</p>
            <p>{order.shippingAddress}</p>
          </div>
          <div className="detail-card">
            <CreditCard size={20} />
            <span>Thanh toán</span>
            <strong>{order.paymentMethod}</strong>
            <p>{order.paymentMethod === "COD" ? "Thu hộ khi giao hàng" : "Cổng thanh toán VNPay"}</p>
            <StatusBadge status={order.paymentStatus} />
          </div>
          <div className="detail-card total-detail">
            <ReceiptText size={20} />
            <span>Tổng đơn</span>
            <strong>{formatMoney(order.total)}</strong>
            <p>Hàng {formatMoney(order.subtotal)} · Ship {formatMoney(order.shippingFee)}</p>
          </div>
        </div>

        <div className="card-actions">
          {canPayAgain && <button className="btn primary" onClick={payAgain}><CreditCard size={18} /> Thanh toán lại</button>}
          {canCancel && (
            <button className="btn secondary" disabled={cancelOrder.isPending} onClick={() => cancelOrder.mutate()}>
              <XCircle size={18} /> {multiShop ? "Hủy toàn bộ đơn" : "Hủy đơn"}
            </button>
          )}
        </div>
      </section>

      {order.shopOrders.map((shopOrder) => {
        const shopOrderId = shopOrder.id || shopOrder.shopOrderId;
        const canCancelPackage =
          multiShop &&
          order.paymentStatus !== "PAID" &&
          !["CANCELLED", "COMPLETED"].includes(order.status) &&
          ["PENDING_PAYMENT", "NEW", "CONFIRMED"].includes(shopOrder.status);
        const canReturn = ["COMPLETED", "DELIVERED"].includes(shopOrder.status);

        return (
          <section className="shop-panel package-card" key={shopOrderId}>
            <div className="package-head">
              <div>
                <span className="shop-name"><Store size={16} /> {shopOrder.shopName}</span>
                <h2>Kiện hàng #{shopOrderId}</h2>
              </div>
              <div className="package-head-actions">
                <StatusBadge status={shopOrder.status} />
                {canCancelPackage && (
                  <button className="btn secondary" disabled={cancelShopOrder.isPending} onClick={() => cancelShopOrder.mutate(shopOrderId)}>
                    <XCircle size={17} /> Hủy kiện này
                  </button>
                )}
                {canReturn && (
                  <button className="btn secondary" disabled={requestReturn.isPending} onClick={() => requestReturn.mutate(shopOrderId)}>
                    <Undo2 size={17} /> Yêu cầu hoàn trả
                  </button>
                )}
              </div>
            </div>

            <div className="package-items">
              {shopOrder.items.map((item) => (
                <div className="package-item" key={item.id}>
                  <div>
                    <h3>{item.productName}</h3>
                    <p className="muted">{item.quantity} x {formatMoney(item.unitPrice)}</p>
                    {item.note && <p className="muted">{item.note}</p>}
                  </div>
                  <strong>{formatMoney(item.lineTotal)}</strong>
                </div>
              ))}
            </div>

            <div className="package-summary">
              <div className="summary-row"><span>Tiền hàng</span><strong>{formatMoney(shopOrder.itemSubtotal)}</strong></div>
              <div className="summary-row"><span>Phí ship GHN</span><strong>{formatMoney(shopOrder.shippingFee)}</strong></div>
              <div className="summary-row"><span>COD shop</span><strong>{formatMoney(shopOrder.codAmount)}</strong></div>
            </div>

            {shopOrder.shipments.length ? (
              shopOrder.shipments.map((shipment) => (
                <div className="shipment-row shipment-track" key={shipment.id}>
                  <Truck size={18} />
                  <span>{shipment.serviceName || "GHN Standard"}</span>
                  <strong>{shipment.trackingCode || "Đang tạo mã vận đơn"}</strong>
                  <StatusBadge status={shipment.status} />
                </div>
              ))
            ) : (
              <div className="shipment-row shipment-track muted">
                <Truck size={18} />
                <span>Shop chưa tạo vận đơn GHN</span>
              </div>
            )}
          </section>
        );
      })}
    </div>
  );
}
