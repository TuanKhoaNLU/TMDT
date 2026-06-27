import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, Loader2, RotateCcw, ShoppingBag, XCircle } from "lucide-react";
import { useState } from "react";
import { marketplaceApi } from "../api/marketplaceApi";
import { StatusBadge } from "../components/StatusBadge";
import { useAuth } from "../context/AuthContext";
import { formatMoney } from "../utils/format";

const sellerTransitions = {
  AWAITING_PAYMENT: ["CRAFTING", "CANCELLED"],
  CRAFTING: ["FINISHING", "CANCELLED"],
  FINISHING: ["SHIPPED", "CANCELLED"],
  REVISION_REJECTED: ["FINISHING", "CANCELLED"],
  SHIPPED: ["DELIVERED"]
};

export default function CustomOrdersPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [revisionText, setRevisionText] = useState({});
  const [actionError, setActionError] = useState("");
  const isSeller = user?.role === "SELLER";
  const ordersQuery = useQuery({ queryKey: ["custom-orders"], queryFn: marketplaceApi.customOrders });
  const revisionsQuery = useQuery({ queryKey: ["custom-order-revisions"], queryFn: marketplaceApi.customOrderRevisions });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ["custom-orders"] });
    queryClient.invalidateQueries({ queryKey: ["custom-order-revisions"] });
    queryClient.invalidateQueries({ queryKey: ["notifications"] });
  };
  const updateStatus = useMutation({
    mutationFn: ({ id, status }) => marketplaceApi.updateCustomOrderStatus(id, { status }),
    onSuccess: refresh,
    onError: (err) => setActionError(err.message)
  });
  const requestRevision = useMutation({
    mutationFn: ({ orderId, reason }) => marketplaceApi.requestCustomOrderRevision(orderId, { reason }),
    onSuccess: (_, variables) => {
      setRevisionText((current) => ({ ...current, [variables.orderId]: "" }));
      refresh();
    },
    onError: (err) => setActionError(err.message)
  });
  const resolveRevision = useMutation({
    mutationFn: ({ revisionId, status }) => marketplaceApi.resolveCustomOrderRevision(revisionId, { status, sellerResponse: status === "ACCEPTED" ? "Shop đồng ý chỉnh sửa theo yêu cầu." : "Shop chưa thể thực hiện thay đổi này." }),
    onSuccess: refresh,
    onError: (err) => setActionError(err.message)
  });

  if (ordersQuery.isLoading || revisionsQuery.isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Đang tải đơn custom...</section>;
  }

  const orders = ordersQuery.data || [];
  const revisions = revisionsQuery.data || [];

  return (
    <div className="dashboard-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><ShoppingBag size={16} /> Đơn custom</span>
          <h1>Đơn đặt làm riêng</h1>
          <p className="muted">Theo dõi báo giá, chế tác, chỉnh sửa, hoàn thiện và giao hàng.</p>
        </div>
      </section>
      {actionError && <section className="alert error">{actionError}</section>}

      <section className="stack-list">
        {orders.map((order) => {
          const orderRevisions = revisions.filter((revision) => revision.customOrderId === order.id);
          const canRequestRevision = !isSeller && ["CRAFTING", "FINISHING", "REVISION_REJECTED"].includes(order.status);
          return (
            <article className="panel custom-order-panel" key={order.id}>
              <div className="section-title-row">
                <div>
                  <span className="section-kicker">Đơn #{order.id}</span>
                  <h2>{order.title}</h2>
                  <p>{order.description}</p>
                </div>
                <div className="badge-row"><StatusBadge status={order.status} /><StatusBadge status={order.paymentStatus} /></div>
              </div>
              <strong>{formatMoney(order.price)}</strong>

              <div className="detail-actions wrap">
                {isSeller ? (sellerTransitions[order.status] || []).map((status) => (
                  <button className="btn secondary" disabled={updateStatus.isPending} key={status} onClick={() => updateStatus.mutate({ id: order.id, status })} type="button">
                    {status === "CANCELLED" ? <XCircle size={16} /> : <Check size={16} />} {statusLabel(status)}
                  </button>
                )) : (
                  <>
                    {order.status === "PENDING_REVIEW" && <button className="btn primary" onClick={() => updateStatus.mutate({ id: order.id, status: "AWAITING_PAYMENT" })} type="button"><Check size={16} /> Chấp nhận báo giá</button>}
                    {["PENDING_REVIEW", "AWAITING_PAYMENT"].includes(order.status) && <button className="btn secondary" onClick={() => updateStatus.mutate({ id: order.id, status: "CANCELLED" })} type="button"><XCircle size={16} /> Hủy yêu cầu</button>}
                  </>
                )}
              </div>

              {canRequestRevision && (
                <form
                  className="inline-form revision-form"
                  onSubmit={(event) => {
                    event.preventDefault();
                    const reason = revisionText[order.id]?.trim();
                    if (reason) requestRevision.mutate({ orderId: order.id, reason });
                  }}
                >
                  <input placeholder="Mô tả nội dung cần chỉnh sửa" value={revisionText[order.id] || ""} onChange={(event) => setRevisionText({ ...revisionText, [order.id]: event.target.value })} />
                  <button className="btn secondary" disabled={requestRevision.isPending} type="submit"><RotateCcw size={16} /> Yêu cầu chỉnh sửa</button>
                </form>
              )}

              {!!orderRevisions.length && (
                <div className="revision-list">
                  <h3>Lịch sử chỉnh sửa</h3>
                  {orderRevisions.map((revision) => (
                    <div className="soft-row" key={revision.id}>
                      <strong>Yêu cầu #{revision.id} · {revision.status}</strong>
                      <span>{revision.reason}</span>
                      {revision.sellerResponse && <small className="muted">Phản hồi: {revision.sellerResponse}</small>}
                      {isSeller && revision.status === "REQUESTED" && (
                        <div className="detail-actions">
                          <button className="btn primary" onClick={() => resolveRevision.mutate({ revisionId: revision.id, status: "ACCEPTED" })} type="button"><Check size={16} /> Đồng ý</button>
                          <button className="btn secondary" onClick={() => resolveRevision.mutate({ revisionId: revision.id, status: "REJECTED" })} type="button"><XCircle size={16} /> Từ chối</button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </article>
          );
        })}
        {!orders.length && <section className="panel"><p className="muted">Chưa có đơn custom.</p></section>}
      </section>
    </div>
  );
}

function statusLabel(status) {
  return ({ CRAFTING: "Bắt đầu chế tác", FINISHING: "Chuyển hoàn thiện", SHIPPED: "Đã gửi hàng", DELIVERED: "Đã giao", CANCELLED: "Hủy đơn" })[status] || status;
}
