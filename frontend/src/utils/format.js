export function formatMoney(value) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0
  }).format(Number(value || 0));
}

export function formatDate(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("vi-VN", {
    dateStyle: "medium",
    timeStyle: "short"
  });
}

export function statusLabel(status) {
  const labels = {
    PENDING_PAYMENT: "Cho thanh toan",
    PROCESSING: "Đang xử lý",
    NEW: "Moi",
    CONFIRMED: "Đã xác nhận",
    PACKING: "Đang đóng gói",
    SHIPPING: "Đang giao",
    COMPLETED: "Hoan tat",
    CANCELLED: "Đã hủy",
    DELIVERY_FAILED: "Giao that bai",
    PAID: "Đã thanh toán",
    PENDING: "Đang chờ",
    COD_PENDING: "COD",
    FAILED: "That bai",
    SUCCESS: "Thanh cong",
    CREATED: "Đã tạo"
  };
  return labels[status] || status || "Không rõ";
}
