import { Link } from "react-router-dom";
import { ShoppingBag } from "lucide-react";

export function EmptyState({ title = "Chưa có dữ liệu", action = true }) {
  return (
    <section className="empty-state">
      <ShoppingBag size={30} />
      <h2>{title}</h2>
      {action && (
        <Link className="btn primary" to="/">
          Chọn sản phẩm
        </Link>
      )}
    </section>
  );
}
