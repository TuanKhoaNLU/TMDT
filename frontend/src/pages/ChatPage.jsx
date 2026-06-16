import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, MessageSquare, Send, ShoppingBag } from "lucide-react";
import { useMemo, useState } from "react";
import { marketplaceApi } from "../api/marketplaceApi";
import { useAuth } from "../context/AuthContext";
import { formatMoney } from "../utils/format";

export default function ChatPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [activeId, setActiveId] = useState(null);
  const [body, setBody] = useState("");
  const [quote, setQuote] = useState({ title: "", description: "", price: "" });

  const conversationsQuery = useQuery({ queryKey: ["chat-conversations"], queryFn: marketplaceApi.conversations });
  const conversations = conversationsQuery.data || [];
  const activeConversation = useMemo(
    () => conversations.find((item) => item.id === activeId) || conversations[0],
    [activeId, conversations]
  );
  const messagesQuery = useQuery({
    queryKey: ["chat-messages", activeConversation?.id],
    queryFn: () => marketplaceApi.messages(activeConversation.id),
    enabled: !!activeConversation?.id
  });

  const sendMessage = useMutation({
    mutationFn: () => marketplaceApi.sendMessage(activeConversation.id, { messageType: "TEXT", body }),
    onSuccess: () => {
      setBody("");
      queryClient.invalidateQueries({ queryKey: ["chat-messages", activeConversation.id] });
      queryClient.invalidateQueries({ queryKey: ["chat-conversations"] });
    }
  });

  const sendQuote = useMutation({
    mutationFn: () => marketplaceApi.sendCustomQuote(activeConversation.id, { ...quote, price: Number(quote.price || 0) }),
    onSuccess: () => {
      setQuote({ title: "", description: "", price: "" });
      queryClient.invalidateQueries({ queryKey: ["chat-messages", activeConversation.id] });
      queryClient.invalidateQueries({ queryKey: ["custom-orders"] });
    }
  });

  if (conversationsQuery.isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Dang tai chat...</section>;
  }

  return (
    <div className="chat-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><MessageSquare size={16} /> Chat</span>
          <h1>Chat va custom quote</h1>
          <p className="muted">Mot customer-seller co mot conversation, seller co the gui offer custom order.</p>
        </div>
      </section>

      <section className="chat-shell">
        <aside className="chat-list panel">
          {conversations.map((conversation) => (
            <button
              className={activeConversation?.id === conversation.id ? "soft-row unread" : "soft-row"}
              key={conversation.id}
              onClick={() => setActiveId(conversation.id)}
              type="button"
            >
              <strong>{conversation.shopName}</strong>
              <span>{conversation.lastMessage || "Chua co tin nhan"}</span>
            </button>
          ))}
          {!conversations.length && <p className="muted">Chua co conversation.</p>}
        </aside>

        <main className="chat-thread panel">
          {!activeConversation ? (
            <p className="muted">Chon conversation de bat dau.</p>
          ) : (
            <>
              <div className="message-list">
                {(messagesQuery.data || []).map((message) => (
                  <div className={message.senderId === user?.userId ? "message-bubble mine" : "message-bubble"} key={message.id}>
                    <small>{message.senderRole} - {message.messageType}</small>
                    <span>{message.body}</span>
                    {message.customOrderId && <strong><ShoppingBag size={14} /> Custom order #{message.customOrderId}</strong>}
                  </div>
                ))}
              </div>

              <form
                className="inline-form"
                onSubmit={(event) => {
                  event.preventDefault();
                  if (body.trim()) sendMessage.mutate();
                }}
              >
                <input value={body} onChange={(event) => setBody(event.target.value)} placeholder="Nhap tin nhan" />
                <button className="btn primary" type="submit"><Send size={16} /> Gui</button>
              </form>

              {user?.role === "SELLER" && (
                <form
                  className="quote-form"
                  onSubmit={(event) => {
                    event.preventDefault();
                    if (quote.title.trim()) sendQuote.mutate();
                  }}
                >
                  <h2><ShoppingBag size={18} /> Gui custom quote</h2>
                  <input placeholder="Tieu de bao gia" value={quote.title} onChange={(event) => setQuote({ ...quote, title: event.target.value })} />
                  <input placeholder="Mo ta" value={quote.description} onChange={(event) => setQuote({ ...quote, description: event.target.value })} />
                  <input placeholder="Gia" type="number" value={quote.price} onChange={(event) => setQuote({ ...quote, price: event.target.value })} />
                  <button className="btn secondary" type="submit">Gui offer {quote.price ? formatMoney(Number(quote.price)) : ""}</button>
                </form>
              )}
            </>
          )}
        </main>
      </section>
    </div>
  );
}
