import { useQuery } from "@tanstack/react-query";
import { AlertCircle, Loader2, Plus, RotateCcw, ShieldCheck, ShoppingCart, SlidersHorizontal, Sparkles, Store, Ticket } from "lucide-react";
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
  const { data: homepage } = useQuery({
    queryKey: ["homepage"],
    queryFn: marketplaceApi.homepage
  });

  const keyword = (searchParams.get("q") || "").trim().toLowerCase();
  const activeCategory = searchParams.get("category") || "ALL";
  const minPrice = searchParams.get("minPrice") || "";
  const maxPrice = searchParams.get("maxPrice") || "";
  const sort = searchParams.get("sort") || "newest";

  const categories = useMemo(
    () => ["ALL", ...new Set(products.map((product) => product.category).filter(Boolean))],
    [products]
  );

  const visibleProducts = useMemo(() => {
    const min = minPrice ? Number(minPrice) : null;
    const max = maxPrice ? Number(maxPrice) : null;
    const filtered = products.filter((product) => {
      const price = Number(product.price || 0);
      const matchesCategory = activeCategory === "ALL" || product.category === activeCategory;
      const haystack = `${product.name} ${product.category} ${product.shopName}`.toLowerCase();
      const matchesPrice = (min === null || price >= min) && (max === null || price <= max);
      return matchesCategory && matchesPrice && (!keyword || haystack.includes(keyword));
    });

    return [...filtered].sort((a, b) => {
      if (sort === "priceAsc") return Number(a.price || 0) - Number(b.price || 0);
      if (sort === "priceDesc") return Number(b.price || 0) - Number(a.price || 0);
      if (sort === "name") return String(a.name || "").localeCompare(String(b.name || ""), "vi");
      return Number(b.id || 0) - Number(a.id || 0);
    });
  }, [activeCategory, keyword, maxPrice, minPrice, products, sort]);

  const activeProducts = products.filter((product) => product.status === "active" && Number(product.stock) > 0);
  const shopCount = new Set(products.map((product) => product.shopName)).size;

  async function handleAdd(productId) {
    await addItem(productId, 1);
  }

  function updateParam(name, value) {
    const next = new URLSearchParams(searchParams);
    if (!value || value === "ALL" || value === "newest") {
      next.delete(name);
    } else {
      next.set(name, value);
    }
    setSearchParams(next);
  }

  function updateCategory(category) {
    updateParam("category", category);
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

  const hasFilter = keyword || activeCategory !== "ALL" || minPrice || maxPrice || sort !== "newest";

  return (
    <div className="market-page">
      {!!homepage?.banners?.length && (
        <section className="home-hero" style={{ backgroundImage: `url(${homepage.banners[0].imageUrl})` }}>
          <div>
            <span className="section-kicker"><Sparkles size={16} /> Handmade marketplace</span>
            <h1>{homepage.banners[0].title}</h1>
            <p>{homepage.banners[0].subtitle}</p>
            <Link className="btn primary" to={homepage.banners[0].linkUrl || "/"}>
              Khám phá ngay
            </Link>
          </div>
        </section>
      )}

      <section className="market-overview">
        <div className="overview-copy">
          <span className="section-kicker">TMDT Marketplace</span>
          <h1>Sàn handmade từ các shop đang hoạt động</h1>
          <p className="muted">Thiệp, quà tặng custom và decor thủ công có sẵn trong kho.</p>
        </div>
        <div className="market-stats" aria-label="Thống kê marketplace">
          <div>
            <strong>{products.length}</strong>
            <span>Sản phẩm</span>
          </div>
          <div>
            <strong>{shopCount}</strong>
            <span>Shop đang bán</span>
          </div>
          <div>
            <strong>{activeProducts.length}</strong>
            <span>Còn hàng</span>
          </div>
        </div>
      </section>

      {!!homepage?.promotions?.length && (
        <section className="promo-row">
          {homepage.promotions.map((promo) => (
            <article className="promo-tile" key={promo.code}>
              <Ticket size={18} />
              <strong>{promo.title}</strong>
              <span>{promo.description}</span>
            </article>
          ))}
        </section>
      )}

      <div className="catalog-shell">
        <aside className="catalog-sidebar">
          <div className="sidebar-block">
            <h2><SlidersHorizontal size={18} /> Bộ lọc</h2>
            <div className="category-list">
              {categories.map((category) => (
                <button
                  className={activeCategory === category || (activeCategory === "ALL" && category === "ALL") ? "active" : ""}
                  key={category}
                  onClick={() => updateCategory(category)}
                  type="button"
                >
                  {category === "ALL" ? "Tất cả danh mục" : category}
                </button>
              ))}
            </div>

            <div className="filter-field">
              <label>Khoảng giá</label>
              <div className="price-filter-row">
                <input type="number" min="0" placeholder="Từ" value={minPrice} onChange={(event) => updateParam("minPrice", event.target.value)} />
                <input type="number" min="0" placeholder="Đến" value={maxPrice} onChange={(event) => updateParam("maxPrice", event.target.value)} />
              </div>
            </div>

            <div className="filter-field">
              <label>Sắp xếp</label>
              <select value={sort} onChange={(event) => updateParam("sort", event.target.value)}>
                <option value="newest">Mới nhất</option>
                <option value="priceAsc">Giá tăng dần</option>
                <option value="priceDesc">Giá giảm dần</option>
                <option value="name">Tên A-Z</option>
              </select>
            </div>

            {hasFilter && (
              <button className="btn secondary full" onClick={clearFilters} type="button">
                <RotateCcw size={16} /> Xóa lọc
              </button>
            )}
          </div>
          <div className="sidebar-block trust-block">
            <p><ShieldCheck size={17} /> COD/VNPay</p>
            <p><Store size={17} /> Shop handmade</p>
            <p><ShoppingCart size={17} /> Giỏ hàng đa shop</p>
          </div>
        </aside>

        <section className="catalog-main">
          <div className="catalog-head">
            <div>
              <span className="section-kicker">Kết quả</span>
              <h2>{visibleProducts.length} sản phẩm</h2>
              {keyword && <p className="muted">Đang tìm: "{keyword}"</p>}
            </div>
            <Link className="btn secondary" to="/cart">
              <ShoppingCart size={18} /> Xem giỏ hàng
            </Link>
          </div>

          {!visibleProducts.length ? (
            <section className="empty-state compact-empty">
              <AlertCircle size={32} />
              <h2>Không có sản phẩm phù hợp</h2>
              <button className="btn secondary" onClick={clearFilters} type="button">
                Xóa bộ lọc
              </button>
            </section>
          ) : (
            <section className="product-grid">
              {visibleProducts.map((product) => {
                const available = product.status === "active" && Number(product.stock) > 0;
                return (
                  <article className="product-card" key={product.id}>
                    <div className="img-wrapper">
                      <Link to={`/products/${product.id}`}>
                        <img src={product.image} alt={product.name} />
                      </Link>
                      <span className="mall-tag">{product.customizable ? "Đặt riêng" : "Có sẵn"}</span>
                    </div>
                    <div className="product-body">
                      <div className="product-info">
                        <Link className="shop-name" to={`/shops/${product.shopId}`}><Store size={14} /> {product.shopName}</Link>
                        <Link to={`/products/${product.id}`}><h2>{product.name}</h2></Link>
                        <p className="muted">{product.category}</p>
                      </div>
                      <div className="product-meta">
                        <strong>{formatMoney(product.price)}</strong>
                        <StatusBadge status={product.status} />
                      </div>
                      <div className="product-actions">
                        <span className={available ? "stock-text" : "danger"}>Tồn kho: {product.stock}</span>
                        <button className="btn primary" disabled={!available} onClick={() => handleAdd(product.id)}>
                          <Plus size={17} /> Thêm vào giỏ
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
      <Loader2 className="spin" size={22} /> Đang tải sản phẩm...
    </section>
  );
}
