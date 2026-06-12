import { useQuery } from "@tanstack/react-query";
import { AlertCircle, Loader2, Plus, RotateCcw, ShieldCheck, ShoppingCart, SlidersHorizontal, Store } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { useMemo } from "react";
import { marketplaceApi } from "../api/marketplaceApi";
import { StatusBadge } from "../components/StatusBadge";
import { useCart } from "../state/CartContext";
import { formatMoney } from "../utils/format";

export default function ProductsPage() {
  const { addItem } = useCart();
  const [searchParams, setSearchParams] = useSearchParams();
  const { data: products = [], isLoading, error } = useQuery({
    queryKey: ["products"],
    queryFn: marketplaceApi.products
  });

  const keyword = (searchParams.get("q") || "").trim().toLowerCase();
  const activeCategory = searchParams.get("category") || "ALL";

  const categories = useMemo(
    () => ["ALL", ...new Set(products.map((product) => product.category).filter(Boolean))],
    [products]
  );
  const visibleProducts = useMemo(() => {
    return products.filter((product) => {
      const matchesCategory = activeCategory === "ALL" || product.category === activeCategory;
      const haystack = `${product.name} ${product.category} ${product.shopName}`.toLowerCase();
      return matchesCategory && (!keyword || haystack.includes(keyword));
    });
  }, [activeCategory, keyword, products]);
  const activeProducts = products.filter((product) => product.status === "active" && Number(product.stock) > 0);
  const shopCount = new Set(products.map((product) => product.shopName)).size;

  async function handleAdd(productId) {
    await addItem(productId, 1);
  }

  function updateCategory(category) {
    const next = new URLSearchParams(searchParams);
    if (category === "ALL") {
      next.delete("category");
    } else {
      next.set("category", category);
    }
    setSearchParams(next);
  }

  function clearFilters() {
    setSearchParams({});
  }

  if (isLoading) {
    return <LoaderState />;
  }

  if (error) {
    return (
      <section className="alert error">
        <AlertCircle size={18} /> {error.message}
      </section>
    );
  }

  return (
    <div className="market-page">
      <section className="market-overview">
        <div className="overview-copy">
          <span className="section-kicker">TMDT Marketplace</span>
          <h1>San handmade tu cac shop dang hoat dong</h1>
          <p className="muted">Thiep, qua tang custom va decor thu cong co san trong kho.</p>
        </div>
        <div className="market-stats" aria-label="Thong ke marketplace">
          <div>
            <strong>{products.length}</strong>
            <span>San pham</span>
          </div>
          <div>
            <strong>{shopCount}</strong>
            <span>Shop dang ban</span>
          </div>
          <div>
            <strong>{activeProducts.length}</strong>
            <span>Con hang</span>
          </div>
        </div>
      </section>

      <div className="catalog-shell">
        <aside className="catalog-sidebar">
          <div className="sidebar-block">
            <h2><SlidersHorizontal size={18} /> Bo loc</h2>
            <div className="category-list">
              {categories.map((category) => (
                <button
                  className={activeCategory === category || (activeCategory === "ALL" && category === "ALL") ? "active" : ""}
                  key={category}
                  onClick={() => updateCategory(category)}
                  type="button"
                >
                  {category === "ALL" ? "Tat ca danh muc" : category}
                </button>
              ))}
            </div>
            {(keyword || activeCategory !== "ALL") && (
              <button className="btn secondary full" onClick={clearFilters} type="button">
                <RotateCcw size={16} /> Xoa loc
              </button>
            )}
          </div>
          <div className="sidebar-block trust-block">
            <p><ShieldCheck size={17} /> COD/VNPay</p>
            <p><Store size={17} /> Shop handmade</p>
            <p><ShoppingCart size={17} /> Gio hang da shop</p>
          </div>
        </aside>

        <section className="catalog-main">
          <div className="catalog-head">
            <div>
              <span className="section-kicker">Ket qua</span>
              <h2>{visibleProducts.length} san pham</h2>
              {keyword && <p className="muted">Dang tim: "{keyword}"</p>}
            </div>
            <Link className="btn secondary" to="/cart">
              <ShoppingCart size={18} /> Xem gio hang
            </Link>
          </div>

          {!visibleProducts.length ? (
            <section className="empty-state compact-empty">
              <AlertCircle size={32} />
              <h2>Khong co san pham phu hop</h2>
              <button className="btn secondary" onClick={clearFilters} type="button">
                Xoa bo loc
              </button>
            </section>
          ) : (
            <section className="product-grid">
              {visibleProducts.map((product) => {
                const available = product.status === "active" && Number(product.stock) > 0;
                return (
                  <article className="product-card" key={product.id}>
                    <div className="img-wrapper">
                      <img src={product.image} alt={product.name} />
                      <span className="mall-tag">{product.customizable ? "Dat rieng" : "Co san"}</span>
                    </div>
                    <div className="product-body">
                      <div className="product-info">
                        <p className="shop-name"><Store size={14} /> {product.shopName}</p>
                        <h2>{product.name}</h2>
                        <p className="muted">{product.category}</p>
                      </div>
                      <div className="product-meta">
                        <strong>{formatMoney(product.price)}</strong>
                        <StatusBadge status={product.status} />
                      </div>
                      <div className="product-actions">
                        <span className={available ? "stock-text" : "danger"}>Ton kho: {product.stock}</span>
                        <button className="btn primary" disabled={!available} onClick={() => handleAdd(product.id)}>
                          <Plus size={17} /> Them vao gio
                        </button>
                      </div>
                    </div>
                  </article>
                );
              })}
            </section>
          )}
        </section>
      </div>
    </div>
  );
}

function LoaderState() {
  return (
    <section className="loading-panel">
      <Loader2 className="spin" size={22} /> Dang tai san pham...
    </section>
  );
}
