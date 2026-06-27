import { useQuery } from "@tanstack/react-query";
import { ArrowDownRight, BadgeDollarSign, Loader2, ReceiptText, WalletCards } from "lucide-react";
import { useMemo, useState } from "react";
import { marketplaceApi } from "../api/marketplaceApi";
import { StatusBadge } from "../components/StatusBadge";
import { formatDate, formatMoney } from "../utils/format";

export default function SellerTransactionsPage() {
  const [filter, setFilter] = useState("ALL");
  const { data = [], isLoading, error } = useQuery({
    queryKey: ["seller-transactions"],
    queryFn: marketplaceApi.sellerTransactions
  });

  const visible = useMemo(
    () => filter === "ALL" ? data : data.filter((item) => item.paymentStatus === filter),
    [data, filter]
  );
  const totals = useMemo(() => data.reduce((sum, item) => ({
    gross: sum.gross + Number(item.grossAmount || 0),
    fee: sum.fee + Number(item.commissionAmount || 0),
    payout: sum.payout + Number(item.payoutAmount || 0)
  }), { gross: 0, fee: 0, payout: 0 }), [data]);

  if (isLoading) return <section className="loading-panel"><Loader2 className="spin" size={22} /> Đang tải giao dịch...</section>;
  if (error) return <section className="alert error">{error.message}</section>;

  return (
    <div className="dashboard-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><WalletCards size={16} /> Tài chính shop</span>
          <h1>Quản lý giao dịch</h1>
          <p className="muted">Đối soát doanh thu, phí nền tảng và số tiền thực nhận theo từng đơn shop.</p>
        </div>
      </section>

      <section className="metric-grid">
        <div><ReceiptText size={18} /><strong>{formatMoney(totals.gross)}</strong><span>Doanh thu gộp</span></div>
        <div><ArrowDownRight size={18} /><strong>{formatMoney(totals.fee)}</strong><span>Phí nền tảng</span></div>
        <div><BadgeDollarSign size={18} /><strong>{formatMoney(totals.payout)}</strong><span>Thực nhận dự kiến</span></div>
      </section>

      <section className="panel">
        <div className="section-title-row">
          <h2>Lịch sử giao dịch</h2>
          <select value={filter} onChange={(event) => setFilter(event.target.value)}>
            <option value="ALL">Tất cả thanh toán</option>
            <option value="PAID">Đã thanh toán</option>
            <option value="COD_PENDING">Chờ COD</option>
            <option value="PENDING">Chờ VNPay</option>
            <option value="FAILED">Thất bại</option>
          </select>
        </div>
        <div className="transaction-table">
          <div className="transaction-row transaction-head">
            <span>Đơn</span><span>Thanh toán</span><span>Doanh thu</span><span>Phí sàn</span><span>Thực nhận</span><span>Ngày tạo</span>
          </div>
          {visible.map((item) => (
            <div className="transaction-row" key={item.shopOrderId}>
              <span><strong>#{item.orderId}</strong><small>Kiện #{item.shopOrderId}</small></span>
              <span><StatusBadge status={item.paymentStatus} /><small>{item.paymentMethod}</small></span>
              <strong>{formatMoney(item.grossAmount)}</strong>
              <span className="danger">-{formatMoney(item.commissionAmount)}</span>
              <strong>{formatMoney(item.payoutAmount)}</strong>
              <span>{formatDate(item.createdAt)}</span>
            </div>
          ))}
          {!visible.length && <p className="muted">Chưa có giao dịch phù hợp.</p>}
        </div>
      </section>
    </div>
  );
}
