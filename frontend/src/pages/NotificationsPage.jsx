import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, CheckCheck, Loader2 } from "lucide-react";
import { marketplaceApi } from "../api/marketplaceApi";

export default function NotificationsPage() {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ["notifications"],
    queryFn: marketplaceApi.notifications
  });

  const markOne = useMutation({
    mutationFn: marketplaceApi.markNotificationRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["notifications"] })
  });
  const markAll = useMutation({
    mutationFn: marketplaceApi.markAllNotificationsRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["notifications"] })
  });

  if (isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Đang tải thông báo...</section>;
  }

  return (
    <div className="notifications-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><Bell size={16} /> Notification</span>
          <h1>Thông báo</h1>
          <p className="muted">{data.unreadCount} thông báo chưa đọc</p>
        </div>
        <button className="btn secondary" onClick={() => markAll.mutate()} type="button">
          <CheckCheck size={17} /> Doc tat ca
        </button>
      </section>
      <section className="panel">
        <div className="stack-list">
          {data.notifications.map((item) => (
            <button className={`soft-row text-left ${item.read ? "" : "unread"}`} key={item.id} onClick={() => markOne.mutate(item.id)} type="button">
              <strong>{item.title}</strong>
              <span>{item.message}</span>
              <small className="muted">{item.type}</small>
            </button>
          ))}
          {!data.notifications.length && <p className="muted">Chưa có thông báo.</p>}
        </div>
      </section>
    </div>
  );
}
