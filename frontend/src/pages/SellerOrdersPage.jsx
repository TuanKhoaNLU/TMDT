import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, Loader2, PackageCheck, Truck, Undo2, XCircle } from "lucide-react";
import { useState } from "react";
import { marketplaceApi } from "../api/marketplaceApi";
import { EmptyState } from "../components/EmptyState";
import { StatusBadge } from "../components/StatusBadge";
import { formatDate, formatMoney } from "../utils/format";

export default function SellerOrdersPage() {
  const queryClient = useQueryClient();
  const [actionError, setActionError] = useState("");
  const { data: orders = [], isLoading, error } = useQuery({
    queryKey: ["seller-orders"],
    queryFn: marketplaceApi.sellerOrders
  });
  const { data: returns = [] } = useQuery({
    queryKey: ["seller-returns"],
    queryFn: marketplaceApi.sellerReturns
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ["seller-orders"] });
    queryClient.invalidateQueries({ queryKey: ["seller-returns"] });
  };

  const updateStatus = useMutation({
    mutationFn: ({ shopOrderId, status }) => marketplaceApi.updateSellerOrderStatus(shopOrderId, status),
    onMutate: () => setActionError(""),
    onSuccess: refresh,
    onError: (err) => setActionError(err.message)
  });
  const createShipment = useMutation({
    mutationFn: (shopOrderId) => marketplaceApi.createGhnShipment(shopOrderId),
    onMutate: () => setActionError(""),
    onSuccess: refresh,
    onError: (err) => setActionError(err.message)
  });
  const updateReturn = useMutation({
    mutationFn: ({ returnId, status }) => marketplaceApi.updateSellerReturn(returnId, { status }),
    onMutate: () => setActionError(""),
    onSuccess: refresh,
    onError: (err) => setActionError(err.message)
  });

  if (isLoading) {
    return (
      <section className="loading-panel">
        <Loader2 className="spin" size={22} /> Đang tải đơn shop...
      </section>
    );
  }

  if (error) return <section className="alert error">{error.message}</section>;

  return (
    <div className="stack">
      <section className="page-head">
        <div>
          <h1>Đơn hàng của shop</h1>
          <p className="muted">Shop chỉ thấy đơn, vận đơn và yêu cầu hoàn trả của shop hiện tại.</p>
        </div>
      </section>
      {actionError && <section className="alert error">{actionError}</section>}

      {!!returns.length && (
        <section className="detail-panel">
          <div className="section-title-row">
            <div>
              <span className="section-kicker"><Undo2 size={15} /> Hoàn trả</span>
              <h2>Yêu cầu hoàn trả/hoàn tiền</h2>
            </div>
          </div>
          <div className="stack compact-stack">
            {returns.map((item) => (
              <article className="order-card compact-order" key={item.id}>
                <div>
                  <h3>Yêu cầu #{item.id} · Đơn #{item.orderId}</h3>
                  <p className="muted">{item.reason}</p>
                  <p className="muted">Số tiền dự kiến: {formatMoney(item.refundAmount)}</p>
                  <StatusBadge status={item.status} />
                </div>
                <div className="card-actions wrap">
                  {item.status === "REQUESTED" && (
                    <>
                      <button className="btn secondary" disabled={updateReturn.isPending} onClick={() => updateReturn.mutate({ returnId: item.id, status: "APPROVED" })}>
                        <Check size={17} /> Duyệt
                      </button>
                      <button className="btn secondary" disabled={updateReturn.isPending} onClick={() => updateReturn.mutate({ returnId: item.id, status: "REJECTED" })}>
                        <XCircle size={17} /> Từ chối
                      </button>
                    </>
                  )}
                  {item.status === "APPROVED" && (
                    <button className="btn primary" disabled={updateReturn.isPending} onClick={() => updateReturn.mutate({ returnId: item.id, status: "REFUNDED" })}>
                      <Undo2 size={17} /> Đã hoàn tiền
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        </section>
      )}

      {!orders.length ? (
        <EmptyState title="Shop chưa có đơn" action={false} />
      ) : (
        orders.map((order) => (
          <article className="order-card seller-card" key={order.shopOrderId}>
            <div>
              <h2>Đơn shop #{order.shopOrderId}</h2>
              <p className="muted">Đơn #{order.orderId} - {formatDate(order.createdAt)}</p>
              <p className="muted">{order.receiverName} - {order.shippingAddress}</p>
              <div className="badge-row">
                <StatusBadge status={order.status} />
                <StatusBadge status={order.paymentStatus} />
              </div>
            </div>

            <div className="stack compact-stack">
              {order.items.map((item) => (
                <div className="summary-row" key={item.id}>
                  <span>{item.productName} x {item.quantity}</span>
                  <strong>{formatMoney(item.lineTotal)}</strong>
                </div>
              ))}
              <div className="summary-row"><span>Phí ship</span><strong>{formatMoney(order.shippingFee)}</strong></div>
              <div className="summary-row"><span>COD</span><strong>{formatMoney(order.codAmount)}</strong></div>
            </div>

            <div className="shipment-list">
              {order.shipments.map((shipment) => (
                <div className="shipment-row" key={shipment.id}>
                  <Truck size={18} />
                  <span>{shipment.trackingCode}</span>
                  <StatusBadge status={shipment.status} />
                </div>
              ))}
            </div>

            <div className="card-actions wrap">
              {sellerActionsFor(order).map((action) => {
                const Icon = action.icon;
                return (
                  <button
                    className="btn secondary"
                    disabled={updateStatus.isPending || createShipment.isPending}
                    key={action.status}
                    onClick={() => updateStatus.mutate({ shopOrderId: order.shopOrderId, status: action.status })}
                  >
                    <Icon size={17} /> {action.label}
                  </button>
                );
              })}
              {order.status === "PACKING" && (
                <button className="btn primary" disabled={updateStatus.isPending || createShipment.isPending} onClick={() => createShipment.mutate(order.shopOrderId)}>
                  <Truck size={17} /> Tạo GHN
                </button>
              )}
            </div>
          </article>
        ))
      )}
    </div>
  );
}

function sellerActionsFor(order) {
  const canCancel = order.paymentStatus !== "PAID";
  switch (order.status) {
    case "NEW":
      return [
        { status: "CONFIRMED", label: "Xác nhận", icon: Check },
        ...(canCancel ? [{ status: "CANCELLED", label: "Hủy", icon: XCircle }] : [])
      ];
    case "CONFIRMED":
      return [
        { status: "PACKING", label: "Đóng gói", icon: PackageCheck },
        ...(canCancel ? [{ status: "CANCELLED", label: "Hủy", icon: XCircle }] : [])
      ];
    case "PACKING":
      return canCancel ? [{ status: "CANCELLED", label: "Hủy", icon: XCircle }] : [];
    case "SHIPPING":
      return [
        { status: "COMPLETED", label: "Hoàn tất", icon: Check },
        { status: "DELIVERY_FAILED", label: "Giao thất bại", icon: XCircle }
      ];
    default:
      return [];
  }
}
