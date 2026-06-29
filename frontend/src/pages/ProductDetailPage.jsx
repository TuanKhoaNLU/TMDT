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
  const [actionError, setActionError] = useState("");

  const detailQuery = useQuery({
    queryKey: ["product-detail", productId],
    queryFn: () => marketplaceApi.productDetail(productId)
  });
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
    onSuccess: () => {
      setQuestion("");
      refreshDetail();
    }
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

  async function handleAddToCart(product) {
    setActionError("");
    try {
      const extras = product.requiresPersonalization
        ? { note: "Khach se bo sung noi dung ca nhan hoa sau" }
        : {};
      await addItem(product.id, 1, extras);
    } catch (err) {
      setActionError(err.message || "Khong the them san pham vao gio hang.");
    }
  }

  if (detailQuery.isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Dang tai chi tiet...</section>;
  }
  if (detailQuery.error) {
    return <section className="alert error"><AlertCircle size={18} /> {detailQuery.error.message}</section>;
  }

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
          <p className="muted">San pham {product.customizable ? "ho tro ca nhan hoa" : "co san"}, ton kho {product.stock}.</p>
          <div className="detail-actions">
            <button className="btn primary" disabled={!available} onClick={() => handleAddToCart(product)} type="button">
              <Plus size={17} /> Them vao gio
            </button>
            <button className="btn secondary" onClick={() => wishlistMutation.mutate()} type="button">
              <Heart size={17} fill={wished ? "currentColor" : "none"} /> {wished ? "Da luu" : "Yeu thich"}
            </button>
            <button className="btn secondary" onClick={() => followMutation.mutate()} type="button">
              <Store size={17} /> {followedShop ? "Dang theo doi" : "Theo doi shop"}
            </button>
            <button className="btn secondary" onClick={() => chatMutation.mutate()} type="button">
              <MessageCircle size={17} /> Chat voi shop
            </button>
            {product.customizable && <Link className="btn secondary" to="/custom-requests"><Sparkles size={17} /> Dang yeu cau custom</Link>}
          </div>
          {actionError && <section className="alert error"><AlertCircle size={18} /> {actionError}</section>}
        </div>
      </section>

      <section className="content-grid">
        <article className="panel">
          <h2>Hoi dap san pham</h2>
          <form className="inline-form" onSubmit={(event) => { event.preventDefault(); if (question.trim()) questionMutation.mutate(); }}>
            <input value={question} onChange={(event) => setQuestion(event.target.value)} placeholder="Dat cau hoi cho shop" />
            <button className="btn primary" type="submit"><Send size={16} /> Gui</button>
          </form>
          <div className="stack-list">
            {questions.map((item) => <div className="soft-row" key={item.id}><strong>{item.question}</strong><span className="muted">{item.answer || "Shop chua tra loi"}</span></div>)}
            {!questions.length && <p className="muted">Chua co cau hoi nao.</p>}
          </div>
        </article>

        <article className="panel">
          <h2>Danh gia</h2>
          {!user && <p className="muted">Dang nhap va hoan tat don hang de danh gia san pham.</p>}
          {user?.role === "BUYER" && eligibilityQuery.isLoading && <p className="muted">Dang kiem tra dieu kien danh gia...</p>}
          {reviewEligibility?.eligible ? (
            <form className="review-form" onSubmit={(event) => { event.preventDefault(); reviewMutation.mutate(); }}>
              <select value={rating} onChange={(event) => setRating(Number(event.target.value))}>{[5, 4, 3, 2, 1].map((value) => <option key={value} value={value}>{value} sao</option>)}</select>
              <input value={review} onChange={(event) => setReview(event.target.value)} placeholder="Cam nhan cua ban" required />
              <button className="btn primary" disabled={reviewMutation.isPending} type="submit"><Star size={16} /> Danh gia</button>
            </form>
          ) : reviewEligibility && <p className="muted">{reviewEligibility.reason}</p>}
          {reviewMutation.error && <p className="danger">{reviewMutation.error.message}</p>}
          <div className="stack-list">
            {reviews.map((item) => (
              <div className="soft-row" key={item.id}>
                <strong><Star size={15} fill="currentColor" /> {item.rating}/5 · {item.customerName || "Khach hang"}</strong>
                <span>{item.comment}</span>
                {item.sellerReply && <small className="muted"><MessageCircle size={14} /> Shop: {item.sellerReply}</small>}
              </div>
            ))}
          </div>
        </article>
      </section>

      {!!relatedProducts.length && (
        <section className="panel">
          <h2>San pham lien quan</h2>
          <div className="product-grid mini-grid">
            {relatedProducts.map((item) => (
              <Link className="mini-product" to={`/products/${item.id}`} key={item.id}>
                <img src={item.image} alt={item.name} />
                <strong>{item.name}</strong>
                <span>{formatMoney(item.price)}</span>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
