import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, ShieldCheck, Truck, XCircle } from "lucide-react";
import { useState } from "react";
import { marketplaceApi } from "../api/marketplaceApi";
import { EmptyState } from "../components/EmptyState";
import { StatusBadge } from "../components/StatusBadge";
import { formatDate, formatMoney } from "../utils/format";

export default function AdminOrdersPage() {
  const queryClient = useQueryClient();
  const [actionError, setActionError] = useState("");
  const { data: orders = [], isLoading, error } = useQuery({
    queryKey: ["admin-orders"],
    queryFn: marketplaceApi.adminOrders
  });
  const cancelOrder = useMutation({
    mutationFn: marketplaceApi.cancelAdminOrder,
    onMutate: () => setActionError(""),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-orders"] }),
    onError: (err) => setActionError(err.message)
  });

  if (isLoading) {
    return (
      <section className="loading-panel">
        <Loader2 className="spin" size={22} /> Dang tai admin orders...
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
          <h1>Admin orders</h1>
          <p className="muted">Order tong, shop order, payment va shipment.</p>
        </div>
        <ShieldCheck size={24} />
      </section>
      {actionError && <section className="alert error">{actionError}</section>}

      {!orders.length ? (
        <EmptyState title="Chua co order" action={false} />
      ) : (
        orders.map((order) => {
          const canCancel =
            order.paymentStatus !== "PAID" &&
            ["PENDING_PAYMENT", "PROCESSING"].includes(order.status) &&
            order.shopOrders.every((shopOrder) => ["PENDING_PAYMENT", "NEW", "CONFIRMED"].includes(shopOrder.status));
          return (
            <article className="admin-card" key={order.id}>
              <div className="page-head">
                <div>
                  <h2>Order #{order.id}</h2>
                  <p className="muted">
                    {order.receiverName} - {formatDate(order.createdAt)}
                  </p>
                </div>
                <div className="badge-row">
                  <StatusBadge status={order.status} />
                  <StatusBadge status={order.paymentStatus} />
                </div>
              </div>

              <div className="metric-grid">
                <div>
                  <span>Total</span>
                  <strong>{formatMoney(order.total)}</strong>
                </div>
                <div>
                  <span>Shop orders</span>
                  <strong>{order.shopOrders.length}</strong>
                </div>
                <div>
                  <span>Payments</span>
                  <strong>{order.payments.length}</strong>
                </div>
              </div>

              {canCancel && (
                <button className="btn secondary self-start" disabled={cancelOrder.isPending} onClick={() => cancelOrder.mutate(order.id)}>
                  <XCircle size={17} /> Huy don
                </button>
              )}

              {order.shopOrders.map((shopOrder) => (
                <div className="mini-card" key={shopOrder.id}>
                  <div className="summary-row">
                    <span>
                      #{shopOrder.id} - {shopOrder.shopName}
                    </span>
                    <StatusBadge status={shopOrder.status} />
                  </div>
                  <div className="summary-row muted">
                    <span>Commission</span>
                    <span>{formatMoney(shopOrder.commissionAmount)}</span>
                  </div>
                  <div className="summary-row muted">
                    <span>Payout</span>
                    <span>{formatMoney(shopOrder.payoutAmount)}</span>
                  </div>
                  {shopOrder.shipments.map((shipment) => (
                    <div className="shipment-row" key={shipment.id}>
                      <Truck size={18} />
                      <span>{shipment.trackingCode}</span>
                      <StatusBadge status={shipment.status} />
                    </div>
                  ))}
                </div>
              ))}

              {order.payments.map((payment) => (
                <div className="payment-row" key={payment.id}>
                  <span>
                    {payment.method} - {payment.transactionRef}
                  </span>
                  <strong>{formatMoney(payment.amount)}</strong>
                  <StatusBadge status={payment.status} />
                </div>
              ))}
            </article>
          );
        })
      )}
    </div>
  );
}
