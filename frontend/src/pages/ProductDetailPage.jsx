import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertCircle, Heart, Loader2, MessageCircle, Plus, Send, Sparkles, Star, Store } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { marketplaceApi } from "../api/marketplaceApi";
import { StatusBadge } from "../components/StatusBadge";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../state/CartContext";
import { formatMoney } from "../utils/format";

export default function ProductDetailPage() {
  const { productId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { addItem } = useCart();
  const [question, setQuestion] = useState("");
  const [review, setReview] = useState("");
  const [rating, setRating] = useState(5);

  const detailQuery = useQuery({ queryKey: ["product-detail", productId], queryFn: () => marketplaceApi.productDetail(productId) });
  const eligibilityQuery = useQuery({
    queryKey: ["review-eligibility", productId],
    queryFn: () => marketplaceApi.reviewEligibility(productId),
    enabled: Boolean(user?.userId && user?.role === "BUYER")
  });
  const refreshDetail = () => queryClient.invalidateQueries({ queryKey: ["product-detail", productId] });
  const wishlistMutation = useMutation({ mutationFn: () => marketplaceApi.toggleWishlist(productId), onSuccess: refreshDetail });
  const followMutation = useMutation({ mutationFn: () => marketplaceApi.toggleFollowShop(detailQuery.data.product.shopId), onSuccess: refreshDetail });
  const questionMutation = useMutation({
    mutationFn: () => marketplaceApi.askQuestion(productId, { question }),
    onSuccess: () => { setQuestion(""); refreshDetail(); }
  });
  const chatMutation = useMutation({
    mutationFn: () => marketplaceApi.startConversation({ shopId: detailQuery.data.product.shopId, productId: detailQuery.data.product.id }),
    onSuccess: () => navigate("/chat")
  });
  const reviewMutation = useMutation({
    mutationFn: () => marketplaceApi.addReview(productId, { rating, comment: review }),
    onSuccess: () => {
      setReview("");
      setRating(5);
      refreshDetail();
      queryClient.invalidateQueries({ queryKey: ["review-eligibility", productId] });
    }
  });

  if (detailQuery.isLoading) return <section className="loading-panel"><Loader2 className="spin" size={22} /> Đang tải chi tiết...</section>;
  if (detailQuery.error) return <section className="alert error"><AlertCircle size={18} /> {detailQuery.error.message}</section>;

  const { product, reviews, questions, wished, followedShop, relatedProducts } = detailQuery.data;
  const available = product.status === "active" && Number(product.stock) > 0;
  const reviewEligibility = eligibilityQuery.data;

  return (
    <div className="detail-page">
      <section className="product-detail-shell">
        <div className="detail-media"><img src={product.image} alt={product.name} /></div>
        <div className="detail-copy">
          <Link className="shop-name" to={`/shops/${product.shopId}`}><Store size={15} /> {product.shopName}</Link>
          <h1>{product.name}</h1>
          <p className="muted">{product.category}</p>
          <div className="detail-price-row"><strong>{formatMoney(product.price)}</strong><StatusBadge status={product.status} /></div>
          <p className="muted">Sản phẩm {product.customizable ? "hỗ trợ cá nhân hóa" : "có sẵn"}, tồn kho {product.stock}.</p>
          <div className="detail-actions">
            <button className="btn primary" disabled={!available} onClick={() => addItem(product.id, 1)}><Plus size={17} /> Thêm vào giỏ</button>
            <button className="btn secondary" onClick={() => wishlistMutation.mutate()} type="button"><Heart size={17} fill={wished ? "currentColor" : "none"} /> {wished ? "Đã lưu" : "Yêu thích"}</button>
            <button className="btn secondary" onClick={() => followMutation.mutate()} type="button"><Store size={17} /> {followedShop ? "Đang theo dõi" : "Theo dõi shop"}</button>
            <button className="btn secondary" onClick={() => chatMutation.mutate()} type="button"><MessageCircle size={17} /> Chat với shop</button>
            {product.customizable && <Link className="btn secondary" to="/custom-requests"><Sparkles size={17} /> Đăng yêu cầu custom</Link>}
          </div>
        </div>
      </section>

      <section className="content-grid">
        <article className="panel">
          <h2>Hỏi đáp sản phẩm</h2>
          <form className="inline-form" onSubmit={(event) => { event.preventDefault(); if (question.trim()) questionMutation.mutate(); }}>
            <input value={question} onChange={(event) => setQuestion(event.target.value)} placeholder="Đặt câu hỏi cho shop" />
            <button className="btn primary" type="submit"><Send size={16} /> Gửi</button>
          </form>
          <div className="stack-list">
            {questions.map((item) => <div className="soft-row" key={item.id}><strong>{item.question}</strong><span className="muted">{item.answer || "Shop chưa trả lời"}</span></div>)}
            {!questions.length && <p className="muted">Chưa có câu hỏi nào.</p>}
          </div>
        </article>

        <article className="panel">
          <h2>Đánh giá</h2>
          {!user && <p className="muted">Đăng nhập và hoàn tất đơn hàng để đánh giá sản phẩm.</p>}
          {user?.role === "BUYER" && eligibilityQuery.isLoading && <p className="muted">Đang kiểm tra điều kiện đánh giá...</p>}
          {reviewEligibility?.eligible ? (
            <form className="review-form" onSubmit={(event) => { event.preventDefault(); reviewMutation.mutate(); }}>
              <select value={rating} onChange={(event) => setRating(Number(event.target.value))}>{[5, 4, 3, 2, 1].map((value) => <option key={value} value={value}>{value} sao</option>)}</select>
              <input value={review} onChange={(event) => setReview(event.target.value)} placeholder="Cảm nhận của bạn" required />
              <button className="btn primary" disabled={reviewMutation.isPending} type="submit"><Star size={16} /> Đánh giá</button>
            </form>
          ) : reviewEligibility && <p className="muted">{reviewEligibility.reason}</p>}
          {reviewMutation.error && <p className="danger">{reviewMutation.error.message}</p>}
          <div className="stack-list">
            {reviews.map((item) => (
              <div className="soft-row" key={item.id}>
                <strong><Star size={15} fill="currentColor" /> {item.rating}/5 · {item.customerName || "Khách hàng"}</strong>
                <span>{item.comment}</span>
                {item.sellerReply && <small className="muted"><MessageCircle size={14} /> Shop: {item.sellerReply}</small>}
              </div>
            ))}
          </div>
        </article>
      </section>

      {!!relatedProducts.length && (
        <section className="panel">
          <h2>Sản phẩm liên quan</h2>
          <div className="product-grid mini-grid">{relatedProducts.map((item) => <Link className="mini-product" to={`/products/${item.id}`} key={item.id}><img src={item.image} alt={item.name} /><strong>{item.name}</strong><span>{formatMoney(item.price)}</span></Link>)}</div>
        </section>
      )}
    </div>
  );
}
