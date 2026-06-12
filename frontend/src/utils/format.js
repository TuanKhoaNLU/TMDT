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
    PROCESSING: "Dang xu ly",
    NEW: "Moi",
    CONFIRMED: "Da xac nhan",
    PACKING: "Dang dong goi",
    SHIPPING: "Dang giao",
    COMPLETED: "Hoan tat",
    CANCELLED: "Da huy",
    DELIVERY_FAILED: "Giao that bai",
    PAID: "Da thanh toan",
    PENDING: "Dang cho",
    COD_PENDING: "COD",
    FAILED: "That bai",
    SUCCESS: "Thanh cong",
    CREATED: "Da tao"
  };
  return labels[status] || status || "Khong ro";
}
