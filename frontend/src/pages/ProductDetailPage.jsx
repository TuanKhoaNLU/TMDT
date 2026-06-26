import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertCircle, Heart, Loader2, MessageCircle, Plus, Send, Star, Store } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useState } from "react";
import { marketplaceApi } from "../api/marketplaceApi";
import { StatusBadge } from "../components/StatusBadge";
import { useCart } from "../state/CartContext";
import { formatMoney } from "../utils/format";

export default function ProductDetailPage() {
  const { productId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { addItem } = useCart();
  const [question, setQuestion] = useState("");
  const [review, setReview] = useState("");
  const [rating, setRating] = useState(5);

  const { data, isLoading, error } = useQuery({
    queryKey: ["product-detail", productId],
    queryFn: () => marketplaceApi.productDetail(productId)
  });

  const wishlistMutation = useMutation({
    mutationFn: () => marketplaceApi.toggleWishlist(productId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["product-detail", productId] })
  });

  const followMutation = useMutation({
    mutationFn: () => marketplaceApi.toggleFollowShop(data.product.shopId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["product-detail", productId] })
  });

  const questionMutation = useMutation({
    mutationFn: () => marketplaceApi.askQuestion(productId, { question }),
    onSuccess: () => {
      setQuestion("");
      queryClient.invalidateQueries({ queryKey: ["product-detail", productId] });
    }
  });
  const chatMutation = useMutation({
    mutationFn: () => marketplaceApi.startConversation({ shopId: data.product.shopId, productId: data.product.id }),
    onSuccess: () => navigate("/chat")
  });

  const reviewMutation = useMutation({
    mutationFn: () => marketplaceApi.addReview(productId, { rating, comment: review }),
    onSuccess: () => {
      setReview("");
      setRating(5);
      queryClient.invalidateQueries({ queryKey: ["product-detail", productId] });
    }
  });

  if (isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Đang tải chi tiết...</section>;
  }

  if (error) {
    return <section className="alert error"><AlertCircle size={18} /> {error.message}</section>;
  }

  const { product, reviews, questions, wished, followedShop, relatedProducts } = data;
  const available = product.status === "active" && Number(product.stock) > 0;

  return (
    <div className="detail-page">
      <section className="product-detail-shell">
        <div className="detail-media">
          <img src={product.image} alt={product.name} />
        </div>
        <div className="detail-copy">
          <Link className="shop-name" to={`/shops/${product.shopId}`}>
            <Store size={15} /> {product.shopName}
          </Link>
          <h1>{product.name}</h1>
          <p className="muted">{product.category}</p>
          <div className="detail-price-row">
            <strong>{formatMoney(product.price)}</strong>
            <StatusBadge status={product.status} />
          </div>
          <p className="muted">Sản phẩm {product.customizable ? "hỗ trợ cá nhân hóa" : "co san"}, ton kho {product.stock}.</p>
          <div className="detail-actions">
            <button className="btn primary" disabled={!available} onClick={() => addItem(product.id, 1)}>
              <Plus size={17} /> Thêm vào giỏ
            </button>
            <button className="btn secondary" onClick={() => wishlistMutation.mutate()} type="button">
              <Heart size={17} fill={wished ? "currentColor" : "none"} /> {wished ? "Đã lưu" : "Wishlist"}
            </button>
            <button className="btn secondary" onClick={() => followMutation.mutate()} type="button">
              <Store size={17} /> {followedShop ? "Đang theo dõi shop" : "Theo dõi shop"}
            </button>
            <button className="btn secondary" onClick={() => chatMutation.mutate()} type="button">
              <MessageCircle size={17} /> Chat với shop
            </button>
          </div>
        </div>
      </section>

      <section className="content-grid">
        <article className="panel">
          <h2>Hoi dap sản phẩm</h2>
          <form
            className="inline-form"
            onSubmit={(event) => {
              event.preventDefault();
              if (question.trim()) questionMutation.mutate();
            }}
          >
            <input value={question} onChange={(event) => setQuestion(event.target.value)} placeholder="Đặt câu hỏi cho shop" />
            <button className="btn primary" type="submit"><Send size={16} /> Gửi</button>
          </form>
          <div className="stack-list">
            {questions.map((item) => (
              <div className="soft-row" key={item.id}>
                <strong>{item.question}</strong>
                <span className="muted">{item.answer || "Shop chưa trả lời"}</span>
              </div>
            ))}
            {!questions.length && <p className="muted">Chưa có câu hỏi nào.</p>}
          </div>
        </article>

        <article className="panel">
          <h2>Review</h2>
          <form
            className="review-form"
            onSubmit={(event) => {
              event.preventDefault();
              reviewMutation.mutate();
            }}
          >
            <select value={rating} onChange={(event) => setRating(Number(event.target.value))}>
              {[5, 4, 3, 2, 1].map((value) => <option key={value} value={value}>{value} sao</option>)}
            </select>
            <input value={review} onChange={(event) => setReview(event.target.value)} placeholder="Cảm nhận của bạn" />
            <button className="btn primary" type="submit"><Star size={16} /> Đánh giá</button>
          </form>
          <div className="stack-list">
            {reviews.map((item) => (
              <div className="soft-row" key={item.id}>
                <strong><Star size={15} fill="currentColor" /> {item.rating}/5 - {item.customerName || "Khách hàng"}</strong>
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
