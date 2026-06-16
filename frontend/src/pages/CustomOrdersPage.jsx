import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, ShoppingBag } from "lucide-react";
import { marketplaceApi } from "../api/marketplaceApi";
import { formatMoney } from "../utils/format";

export default function CustomOrdersPage() {
  const queryClient = useQueryClient();
  const { data = [], isLoading } = useQuery({ queryKey: ["custom-orders"], queryFn: marketplaceApi.customOrders });
  const updateStatus = useMutation({
    mutationFn: ({ id, status }) => marketplaceApi.updateCustomOrderStatus(id, { status }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["custom-orders"] })
  });

  if (isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Dang tai custom orders...</section>;
  }

  return (
    <div className="dashboard-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><ShoppingBag size={16} /> Custom Orders</span>
          <h1>Don dat lam rieng</h1>
          <p className="muted">Theo doi quote, crafting, finishing, shipped va delivered.</p>
        </div>
      </section>
      <section className="panel">
        <div className="stack-list">
          {data.map((order) => (
            <div className="soft-row" key={order.id}>
              <strong>#{order.id} {order.title} - {order.status}</strong>
              <span>{order.description}</span>
              <span>{formatMoney(order.price)} - payment {order.paymentStatus}</span>
              <div className="detail-actions">
                {["AWAITING_PAYMENT", "CRAFTING", "FINISHING", "SHIPPED", "DELIVERED", "CANCELLED"].map((status) => (
                  <button className="btn secondary" key={status} onClick={() => updateStatus.mutate({ id: order.id, status })} type="button">
                    {status}
                  </button>
                ))}
              </div>
            </div>
          ))}
          {!data.length && <p className="muted">Chua co custom order.</p>}
        </div>
      </section>
    </div>
  );
}
