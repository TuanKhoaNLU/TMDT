import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Clock3, Image as ImageIcon, Loader2, Palette, Send, Sparkles } from "lucide-react";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { marketplaceApi } from "../api/marketplaceApi";
import { useAuth } from "../context/AuthContext";
import { formatMoney } from "../utils/format";

const emptyRequest = { title: "", description: "", budgetMin: "", budgetMax: "", desiredTimeline: "", referenceImages: "" };
const emptyProposal = { message: "", proposedPrice: "", leadTimeDays: 7, sketchImageUrl: "" };

export default function CustomRequestsPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [selectedPostId, setSelectedPostId] = useState(null);
  const [requestForm, setRequestForm] = useState(emptyRequest);
  const [proposalForm, setProposalForm] = useState(emptyProposal);
  const [actionError, setActionError] = useState("");
  const isSeller = user?.role === "SELLER";

  const postsQuery = useQuery({ queryKey: ["commissions"], queryFn: marketplaceApi.commissions });
  const posts = postsQuery.data || [];
  const selectedPost = useMemo(
    () => posts.find((post) => post.id === selectedPostId) || posts[0],
    [posts, selectedPostId]
  );
  const proposalsQuery = useQuery({
    queryKey: ["commission-proposals", selectedPost?.id],
    queryFn: () => marketplaceApi.proposals(selectedPost.id),
    enabled: Boolean(selectedPost?.id)
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ["commissions"] });
    queryClient.invalidateQueries({ queryKey: ["commission-proposals", selectedPost?.id] });
    queryClient.invalidateQueries({ queryKey: ["notifications"] });
  };

  const createRequest = useMutation({
    mutationFn: (payload) => marketplaceApi.createCommission(payload),
    onSuccess: (post) => {
      setRequestForm(emptyRequest);
      setSelectedPostId(post.id);
      refresh();
    },
    onError: (err) => setActionError(err.message)
  });
  const createProposal = useMutation({
    mutationFn: (payload) => marketplaceApi.createProposal(selectedPost.id, payload),
    onSuccess: () => {
      setProposalForm(emptyProposal);
      refresh();
    },
    onError: (err) => setActionError(err.message)
  });
  const acceptProposal = useMutation({
    mutationFn: (proposalId) => marketplaceApi.acceptProposal(selectedPost.id, proposalId),
    onSuccess: refresh,
    onError: (err) => setActionError(err.message)
  });

  if (postsQuery.isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Đang tải yêu cầu custom...</section>;
  }

  const ownsSelectedPost = selectedPost?.customerId === user?.userId;

  return (
    <div className="dashboard-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><Sparkles size={16} /> Thiết kế theo yêu cầu</span>
          <h1>Yêu cầu sản phẩm custom</h1>
          <p className="muted">Khách hàng đăng nhu cầu, các shop gửi báo giá và bản thảo thiết kế.</p>
        </div>
        <Link className="btn secondary" to="/custom-orders">Xem đơn custom</Link>
      </section>
      {actionError && <section className="alert error">{actionError}</section>}

      {!isSeller && (
        <form
          className="panel catalog-form"
          onSubmit={(event) => {
            event.preventDefault();
            setActionError("");
            createRequest.mutate({
              ...requestForm,
              budgetMin: Number(requestForm.budgetMin || 0),
              budgetMax: Number(requestForm.budgetMax || 0)
            });
          }}
        >
          <h2><Palette size={18} /> Đăng yêu cầu mới</h2>
          <input placeholder="Tên sản phẩm cần làm" value={requestForm.title} onChange={(e) => setRequestForm({ ...requestForm, title: e.target.value })} required />
          <textarea placeholder="Mô tả kích thước, vật liệu, màu sắc và yêu cầu cá nhân hóa" value={requestForm.description} onChange={(e) => setRequestForm({ ...requestForm, description: e.target.value })} required />
          <input type="number" min="0" placeholder="Ngân sách tối thiểu" value={requestForm.budgetMin} onChange={(e) => setRequestForm({ ...requestForm, budgetMin: e.target.value })} />
          <input type="number" min="0" placeholder="Ngân sách tối đa" value={requestForm.budgetMax} onChange={(e) => setRequestForm({ ...requestForm, budgetMax: e.target.value })} required />
          <input placeholder="Thời hạn mong muốn, ví dụ 10 ngày" value={requestForm.desiredTimeline} onChange={(e) => setRequestForm({ ...requestForm, desiredTimeline: e.target.value })} />
          <input type="url" placeholder="URL ảnh tham khảo" value={requestForm.referenceImages} onChange={(e) => setRequestForm({ ...requestForm, referenceImages: e.target.value })} />
          <button className="btn primary" disabled={createRequest.isPending} type="submit"><Send size={17} /> Đăng yêu cầu</button>
        </form>
      )}

      <section className="commission-layout">
        <aside className="panel commission-list">
          <h2>Yêu cầu đang có</h2>
          {posts.map((post) => (
            <button className={selectedPost?.id === post.id ? "soft-row unread" : "soft-row"} key={post.id} onClick={() => setSelectedPostId(post.id)} type="button">
              <strong>{post.title}</strong>
              <span>{formatMoney(post.budgetMin)} - {formatMoney(post.budgetMax)}</span>
              <small>{post.status}</small>
            </button>
          ))}
          {!posts.length && <p className="muted">Chưa có yêu cầu custom.</p>}
        </aside>

        <main className="panel commission-detail">
          {!selectedPost ? <p className="muted">Chọn một yêu cầu để xem chi tiết.</p> : (
            <>
              <div className="section-title-row">
                <div>
                  <span className="section-kicker">#{selectedPost.id} · {selectedPost.status}</span>
                  <h2>{selectedPost.title}</h2>
                  <p>{selectedPost.description}</p>
                </div>
              </div>
              <div className="request-meta">
                <span><Clock3 size={16} /> {selectedPost.desiredTimeline || "Chưa đặt thời hạn"}</span>
                <strong>{formatMoney(selectedPost.budgetMin)} - {formatMoney(selectedPost.budgetMax)}</strong>
                {selectedPost.referenceImages && <a className="text-link" href={selectedPost.referenceImages} target="_blank" rel="noreferrer"><ImageIcon size={16} /> Xem ảnh tham khảo</a>}
              </div>

              {isSeller && selectedPost.status === "OPEN" && (
                <form
                  className="quote-form"
                  onSubmit={(event) => {
                    event.preventDefault();
                    setActionError("");
                    createProposal.mutate({
                      ...proposalForm,
                      proposedPrice: Number(proposalForm.proposedPrice || 0),
                      leadTimeDays: Number(proposalForm.leadTimeDays || 1)
                    });
                  }}
                >
                  <h3>Gửi báo giá và bản thảo</h3>
                  <textarea placeholder="Nội dung tư vấn và phương án thực hiện" value={proposalForm.message} onChange={(e) => setProposalForm({ ...proposalForm, message: e.target.value })} required />
                  <input type="number" min="0" placeholder="Giá đề xuất" value={proposalForm.proposedPrice} onChange={(e) => setProposalForm({ ...proposalForm, proposedPrice: e.target.value })} required />
                  <input type="number" min="1" placeholder="Số ngày thực hiện" value={proposalForm.leadTimeDays} onChange={(e) => setProposalForm({ ...proposalForm, leadTimeDays: e.target.value })} required />
                  <input type="url" placeholder="URL bản thảo thiết kế" value={proposalForm.sketchImageUrl} onChange={(e) => setProposalForm({ ...proposalForm, sketchImageUrl: e.target.value })} />
                  <button className="btn primary" disabled={createProposal.isPending} type="submit"><Send size={17} /> Gửi báo giá</button>
                </form>
              )}

              <div className="stack-list">
                <h3>Báo giá từ shop</h3>
                {(proposalsQuery.data || []).map((proposal) => (
                  <article className="proposal-row" key={proposal.id}>
                    <div>
                      <strong>{proposal.shopName} · {formatMoney(proposal.proposedPrice)}</strong>
                      <p>{proposal.message}</p>
                      <small className="muted">Thời gian: {proposal.leadTimeDays} ngày · {proposal.status}</small>
                    </div>
                    {proposal.sketchImageUrl && <img src={proposal.sketchImageUrl} alt={`Bản thảo của ${proposal.shopName}`} />}
                    {ownsSelectedPost && selectedPost.status === "OPEN" && proposal.status === "PENDING" && (
                      <button className="btn primary" disabled={acceptProposal.isPending} onClick={() => acceptProposal.mutate(proposal.id)} type="button">
                        <CheckCircle2 size={17} /> Chấp nhận
                      </button>
                    )}
                  </article>
                ))}
                {!proposalsQuery.isLoading && !(proposalsQuery.data || []).length && <p className="muted">Chưa có shop gửi báo giá.</p>}
              </div>
            </>
          )}
        </main>
      </section>
    </div>
  );
}
