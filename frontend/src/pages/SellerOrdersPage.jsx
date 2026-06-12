import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, Loader2, PackageCheck, Truck, XCircle } from "lucide-react";
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

  const updateStatus = useMutation({
    mutationFn: ({ shopOrderId, status }) => marketplaceApi.updateSellerOrderStatus(shopOrderId, status),
    onMutate: () => setActionError(""),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["seller-orders"] }),
    onError: (err) => setActionError(err.message)
  });

  const createShipment = useMutation({
    mutationFn: (shopOrderId) => marketplaceApi.createGhnShipment(shopOrderId),
    onMutate: () => setActionError(""),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["seller-orders"] }),
    onError: (err) => setActionError(err.message)
  });

  if (isLoading) {
    return (
      <section className="loading-panel">
        <Loader2 className="spin" size={22} /> Dang tai don shop...
      </section>
    );
  }

  if (error) {
    return <section className="alert error">{error.message}</section>;
  }

  return (
    <div className="stack">
      <section className="page-head">
        <div>
          <h1>Seller orders</h1>
          <p className="muted">Shop chi thay don cua shop hien tai.</p>
        </div>
      </section>
      {actionError && <section className="alert error">{actionError}</section>}

      {!orders.length ? (
        <EmptyState title="Shop chua co don" action={false} />
      ) : (
        orders.map((order) => (
          <article className="order-card seller-card" key={order.shopOrderId}>
            <div>
              <h2>Shop order #{order.shopOrderId}</h2>
              <p className="muted">
                Order #{order.orderId} - {formatDate(order.createdAt)}
              </p>
              <p className="muted">
                {order.receiverName} - {order.shippingAddress}
              </p>
              <div className="badge-row">
                <StatusBadge status={order.status} />
                <StatusBadge status={order.paymentStatus} />
              </div>
            </div>

            <div className="stack compact-stack">
              {order.items.map((item) => (
                <div className="summary-row" key={item.id}>
                  <span>
                    {item.productName} x {item.quantity}
                  </span>
                  <strong>{formatMoney(item.lineTotal)}</strong>
                </div>
              ))}
              <div className="summary-row">
                <span>Phi ship</span>
                <strong>{formatMoney(order.shippingFee)}</strong>
              </div>
              <div className="summary-row">
                <span>COD</span>
                <strong>{formatMoney(order.codAmount)}</strong>
              </div>
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
                <button
                  className="btn primary"
                  disabled={updateStatus.isPending || createShipment.isPending}
                  onClick={() => createShipment.mutate(order.shopOrderId)}
                >
                  <Truck size={17} /> Tao GHN
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
        { status: "CONFIRMED", label: "Xac nhan", icon: Check },
        ...(canCancel ? [{ status: "CANCELLED", label: "Huy", icon: XCircle }] : [])
      ];
    case "CONFIRMED":
      return [
        { status: "PACKING", label: "Dong goi", icon: PackageCheck },
        ...(canCancel ? [{ status: "CANCELLED", label: "Huy", icon: XCircle }] : [])
      ];
    case "PACKING":
      return canCancel ? [{ status: "CANCELLED", label: "Huy", icon: XCircle }] : [];
    case "SHIPPING":
      return [
        { status: "COMPLETED", label: "Hoan tat", icon: Check },
        { status: "DELIVERY_FAILED", label: "Giao that bai", icon: XCircle }
      ];
    default:
      return [];
  }
}
