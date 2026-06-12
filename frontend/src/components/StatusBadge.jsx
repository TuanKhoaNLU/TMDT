import { statusLabel } from "../utils/format";

export function StatusBadge({ status }) {
  const tone = String(status || "unknown").toLowerCase().replaceAll("_", "-");
  return <span className={`status-badge ${tone}`}>{statusLabel(status)}</span>;
}
