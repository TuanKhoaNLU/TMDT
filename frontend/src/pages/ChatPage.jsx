import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FileText, Image as ImageIcon, Loader2, MessageSquare, Paperclip, Send, ShoppingBag, User, X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { marketplaceApi } from "../api/marketplaceApi";
import { useAuth } from "../context/AuthContext";
import { formatMoney } from "../utils/format";

const allowedAttachmentTypes = [
  "image/",
  "application/pdf",
  "text/plain",
  "application/msword",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "application/vnd.ms-excel",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "application/zip",
  "application/x-zip-compressed"
];

export default function ChatPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [activeId, setActiveId] = useState(null);
  const [body, setBody] = useState("");
  const [selectedFile, setSelectedFile] = useState(null);
  const [actionError, setActionError] = useState("");
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

  const refreshChat = () => {
    if (activeConversation?.id) {
      queryClient.invalidateQueries({ queryKey: ["chat-messages", activeConversation.id] });
    }
    queryClient.invalidateQueries({ queryKey: ["chat-conversations"] });
  };

  const sendMessage = useMutation({
    mutationFn: async () => {
      if (!activeConversation?.id) return null;
      let attachment = null;
      if (selectedFile) {
        attachment = await marketplaceApi.uploadChatAttachment(activeConversation.id, selectedFile);
      }
      const payload = {
        messageType: attachment ? (attachment.image ? "IMAGE" : "FILE") : "TEXT",
        body: body.trim(),
        imageUrl: attachment?.image ? attachment.url : "",
        attachmentUrl: attachment?.url || "",
        attachmentName: attachment?.fileName || "",
        attachmentContentType: attachment?.contentType || "",
        attachmentSize: attachment?.size || 0
      };
      return marketplaceApi.sendMessage(activeConversation.id, payload);
    },
    onSuccess: () => {
      setBody("");
      setSelectedFile(null);
      setActionError("");
      refreshChat();
    },
    onError: (err) => setActionError(err.message)
  });

  const sendQuote = useMutation({
    mutationFn: () => marketplaceApi.sendCustomQuote(activeConversation.id, { ...quote, price: Number(quote.price || 0) }),
    onSuccess: () => {
      setQuote({ title: "", description: "", price: "" });
      refreshChat();
      queryClient.invalidateQueries({ queryKey: ["custom-orders"] });
    },
    onError: (err) => setActionError(err.message)
  });

  const unsendMsg = useMutation({
    mutationFn: (messageId) => marketplaceApi.unsendMessage(messageId),
    onSuccess: () => {
      refreshChat();
    },
    onError: (err) => setActionError(err.message)
  });

  function handleFileChange(event) {
    const file = event.target.files?.[0];
    event.target.value = "";
    setActionError("");
    if (!file) return;
    const allowed = allowedAttachmentTypes.some((type) => file.type?.startsWith(type) || file.type === type);
    if (!allowed) {
      setActionError("File khong duoc ho tro. Hay chon anh, PDF, Word, Excel, text hoac zip.");
      return;
    }
    if (file.size > 8 * 1024 * 1024) {
      setActionError("File chat khong duoc vuot qua 8MB.");
      return;
    }
    setSelectedFile(file);
  }

  function submitMessage(event) {
    event.preventDefault();
    if (!body.trim() && !selectedFile) {
      setActionError("Nhap tin nhan hoac chon file truoc khi gui.");
      return;
    }
    sendMessage.mutate();
  }

  if (conversationsQuery.isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Dang tai chat...</section>;
  }

  return (
    <div className="chat-page">
      {actionError && <section className="alert error">{actionError}</section>}

      <section className="chat-shell">
        <aside className="chat-list">
          {conversations.map((conversation) => (
            <button
              className={activeConversation?.id === conversation.id ? "chat-list-item unread" : "chat-list-item"}
              key={conversation.id}
              onClick={() => setActiveId(conversation.id)}
              type="button"
            >
              <div className="chat-avatar"><User size={20} /></div>
              <div className="chat-list-copy">
                <strong>{conversation.shopName}</strong>
                <span className="truncate">{conversation.lastMessage || "Chua co tin nhan"}</span>
              </div>
            </button>
          ))}
          {!conversations.length && <p className="muted" style={{ padding: 16 }}>Chua co cuoc hoi thoai.</p>}
        </aside>

        <main className="chat-thread">
          {!activeConversation ? (
            <p className="muted">Chon conversation de bat dau.</p>
          ) : (
            <>
              <div className="message-list">
                {messagesQuery.isLoading ? <Loader2 className="spin" /> : (messagesQuery.data || []).map((msg) => (
                  <MessageBubble 
                    key={msg.id} 
                    message={msg} 
                    mine={msg.senderId === user?.userId} 
                    onUnsend={() => unsendMsg.mutate(msg.id)} 
                  />
                ))}
              </div>

              <form className="chat-compose" onSubmit={submitMessage}>
                {selectedFile && (
                  <div className="attachment-preview">
                    {selectedFile.type.startsWith("image/") ? <ImageIcon size={17} /> : <FileText size={17} />}
                    <span>{selectedFile.name}</span>
                    <small>{formatFileSize(selectedFile.size)}</small>
                    <button className="icon-btn" onClick={() => setSelectedFile(null)} type="button" title="Bo file">
                      <X size={15} />
                    </button>
                  </div>
                )}
                <div className="chat-compose-row">
                  <label className="icon-btn file-button" title="Dinh kem file">
                    <Paperclip size={20} />
                    <input
                      accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.txt,.zip"
                      onChange={handleFileChange}
                      type="file"
                    />
                  </label>
                  <input className="chat-input" value={body} onChange={(event) => setBody(event.target.value)} placeholder="Nhap tin nhan..." />
                  <button className="icon-btn primary-icon-btn send-button" disabled={sendMessage.isPending} type="submit" title="Gui">
                    {sendMessage.isPending ? <Loader2 className="spin" size={18} /> : <Send size={18} />}
                  </button>
                </div>
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
                  <button className="btn secondary" disabled={sendQuote.isPending} type="submit">Gui offer {quote.price ? formatMoney(Number(quote.price)) : ""}</button>
                </form>
              )}
            </>
          )}
        </main>
      </section>
    </div>
  );
}

function MessageBubble({ message, mine, onUnsend }) {
  const isImage = message.attachmentContentType?.startsWith("image/") || message.messageType === "IMAGE";
  
  if (message.messageType === "RECALLED") {
    return (
      <div className={mine ? "message-bubble mine recalled" : "message-bubble recalled"}>
        <small>{message.senderRole} - Đã thu hồi</small>
        <span className="recalled-text">Tin nhắn đã bị thu hồi</span>
      </div>
    );
  }

  return (
    <div className={mine ? "message-bubble mine" : "message-bubble"}>
      <small>
        {message.senderRole} - {message.messageType}
        {mine && (
          <button className="unsend-btn" onClick={onUnsend} title="Thu hồi tin nhắn" type="button">
            Thu hồi
          </button>
        )}
      </small>
      {message.body && <span>{message.body}</span>}
      {message.attachmentUrl && (
        <a className={isImage ? "chat-attachment image" : "chat-attachment"} href={message.attachmentUrl} target="_blank" rel="noreferrer">
          {isImage ? (
            <img src={message.attachmentUrl} alt={message.attachmentName || "Chat attachment"} />
          ) : (
            <>
              <FileText size={18} />
              <strong>{message.attachmentName || "File dinh kem"}</strong>
              <small>{formatFileSize(message.attachmentSize)}</small>
            </>
          )}
        </a>
      )}
      {message.customOrderId && <strong><ShoppingBag size={14} /> Custom order #{message.customOrderId}</strong>}
    </div>
  );
}

function formatFileSize(size = 0) {
  if (!size) return "";
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${Math.round(size / 1024)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}
