import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, Loader2, XCircle } from "lucide-react";
import { Link, useLocation } from "react-router-dom";
import { marketplaceApi } from "../api/marketplaceApi";
import { StatusBadge } from "../components/StatusBadge";
import { formatMoney } from "../utils/format";

export default function PaymentResultPage() {
  const location = useLocation();
  const queryString = location.search || "";
  const pendingOrderId = localStorage.getItem("pendingPaymentOrderId");

  const resultQuery = useQuery({
    queryKey: ["vnpay-return", queryString],
    queryFn: () => marketplaceApi.verifyVnpayReturn(queryString),
    enabled: Boolean(queryString)
  });

  if (!queryString) {
    return (
      <section className="result-card">
        <XCircle size={40} />
        <h1>Chưa có giao dịch</h1>
        {pendingOrderId ? (
          <Link className="btn primary" to={`/orders/${pendingOrderId}`}>
            Xem đơn đang chờ
          </Link>
        ) : (
          <Link className="btn primary" to="/orders">
            Xem đơn hàng
          </Link>
        )}
      </section>
    );
  }

  if (resultQuery.isLoading) {
    return (
      <section className="loading-panel">
        <Loader2 className="spin" size={22} /> Đang xác minh thanh toán...
      </section>
    );
  }

  const result = resultQuery.data;
  if (result?.success || result?.paymentStatus === "FAILED" || result?.orderStatus === "CANCELLED") {
    localStorage.removeItem("pendingPaymentOrderId");
  }

  return (
    <section className="result-card">
      {result?.success ? <CheckCircle2 size={44} /> : <XCircle size={44} />}
      <h1>{result?.success ? "Thanh toán thành công" : "Thanh toán thất bại"}</h1>
      <p className="muted">{result?.message}</p>
      <div className="summary-row">
        <span>Mã đơn</span>
        <strong>#{result?.orderId}</strong>
      </div>
      <div className="summary-row">
        <span>So tien</span>
        <strong>{formatMoney(result?.amount)}</strong>
      </div>
      <div className="summary-row">
        <span>Trang thai</span>
        <StatusBadge status={result?.paymentStatus} />
      </div>
      <Link className="btn primary" to={`/orders/${result?.orderId}`}>
        Xem đơn
      </Link>
    </section>
  );
}
