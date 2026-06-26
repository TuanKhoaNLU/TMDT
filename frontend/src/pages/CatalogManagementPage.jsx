import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Loader2, PackagePlus, ShieldCheck, Tags, XCircle } from "lucide-react";
import { useState } from "react";
import { marketplaceApi } from "../api/marketplaceApi";
import { useAuth } from "../context/AuthContext";
import { formatMoney } from "../utils/format";

export default function CatalogManagementPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  const queryClient = useQueryClient();
  const [productForm, setProductForm] = useState({
    name: "",
    sku: "",
    categoryId: 1,
    price: "",
    stock: 5,
    lowStockAlert: 2,
    tags: "",
    imageUrl: "",
    description: "",
    requiresPersonalization: false,
    customizable: false,
    processingDays: 3
  });
  const [categoryName, setCategoryName] = useState("");

  const productsQuery = useQuery({
    queryKey: [isAdmin ? "admin-products" : "seller-products"],
    queryFn: isAdmin ? marketplaceApi.adminProducts : marketplaceApi.sellerProducts
  });
  const categoriesQuery = useQuery({ queryKey: ["categories"], queryFn: marketplaceApi.categories });

  const createProduct = useMutation({
    mutationFn: marketplaceApi.createSellerProduct,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-products"] });
      setProductForm((current) => ({ ...current, name: "", sku: "", price: "", description: "" }));
    }
  });

  const moderate = useMutation({
    mutationFn: ({ productId, status }) => marketplaceApi.moderateProduct(productId, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-products"] })
  });

  const createCategory = useMutation({
    mutationFn: marketplaceApi.createCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["categories"] });
      setCategoryName("");
    }
  });

  if (productsQuery.isLoading || categoriesQuery.isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Đang tải catalog...</section>;
  }

  const products = productsQuery.data || [];
  const categories = categoriesQuery.data || [];

  function updateForm(field, value) {
    setProductForm((current) => ({ ...current, [field]: value }));
  }

  function submitProduct(event) {
    event.preventDefault();
    createProduct.mutate({
      ...productForm,
      categoryId: Number(productForm.categoryId),
      price: Number(productForm.price || 0),
      stock: Number(productForm.stock || 0),
      lowStockAlert: Number(productForm.lowStockAlert || 0),
      processingDays: Number(productForm.processingDays || 3)
    });
  }

  return (
    <div className="dashboard-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><Tags size={16} /> Catalog</span>
          <h1>{isAdmin ? "Admin product approval" : "Seller product listings"}</h1>
          <p className="muted">Seller tao/sua listing se ve PENDING, admin duyet moi public.</p>
        </div>
      </section>

      {!isAdmin && (
        <form className="panel catalog-form" onSubmit={submitProduct}>
          <h2><PackagePlus size={18} /> Tạo listing mới</h2>
          <input placeholder="Tên sản phẩm" value={productForm.name} onChange={(e) => updateForm("name", e.target.value)} required />
          <input placeholder="SKU" value={productForm.sku} onChange={(e) => updateForm("sku", e.target.value)} />
          <select value={productForm.categoryId} onChange={(e) => updateForm("categoryId", e.target.value)}>
            {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
          </select>
          <input placeholder="Gia" type="number" value={productForm.price} onChange={(e) => updateForm("price", e.target.value)} required />
          <input placeholder="Tồn kho" type="number" value={productForm.stock} onChange={(e) => updateForm("stock", e.target.value)} />
          <input placeholder="Nguong canh bao ton" type="number" value={productForm.lowStockAlert} onChange={(e) => updateForm("lowStockAlert", e.target.value)} />
          <input placeholder="Tag" value={productForm.tags} onChange={(e) => updateForm("tags", e.target.value)} />
          <input placeholder="Image URL" value={productForm.imageUrl} onChange={(e) => updateForm("imageUrl", e.target.value)} />
          <textarea placeholder="Mo ta" value={productForm.description} onChange={(e) => updateForm("description", e.target.value)} />
          <label className="check-row">
            <input type="checkbox" checked={productForm.customizable} onChange={(e) => updateForm("customizable", e.target.checked)} />
            Sản phẩm custom
          </label>
          <label className="check-row">
            <input type="checkbox" checked={productForm.requiresPersonalization} onChange={(e) => updateForm("requiresPersonalization", e.target.checked)} />
            Bat buoc nhap ca nhan hoa khi mua
          </label>
          <button className="btn primary" type="submit">Gửi admin duyet</button>
        </form>
      )}

      {isAdmin && (
        <form
          className="panel inline-form"
          onSubmit={(event) => {
            event.preventDefault();
            if (categoryName.trim()) createCategory.mutate({ name: categoryName, status: "ACTIVE" });
          }}
        >
          <input placeholder="Thêm danh mục mới" value={categoryName} onChange={(e) => setCategoryName(e.target.value)} />
          <button className="btn primary" type="submit">Thêm danh mục</button>
        </form>
      )}

      <section className="panel">
        <h2>Danh sách sản phẩm</h2>
        <div className="stack-list">
          {products.map((product) => (
            <div className="catalog-row" key={product.id}>
              <img src={product.imageUrl || "https://picsum.photos/seed/product-empty/120/120"} alt={product.name} />
              <div>
                <strong>{product.name}</strong>
                <span className="muted">{product.categoryName} - {product.sku || "NO-SKU"}</span>
                <span>{formatMoney(product.price)} - stock {product.stock} - low {product.lowStockAlert}</span>
              </div>
              <div className="catalog-status">
                <span>{product.approvalStatus}</span>
                <small>{product.saleStatus}</small>
              </div>
              {isAdmin && (
                <div className="detail-actions">
                  <button className="btn secondary" onClick={() => moderate.mutate({ productId: product.id, status: "APPROVED" })} type="button">
                    <CheckCircle2 size={16} /> Duyet
                  </button>
                  <button className="btn secondary danger-btn" onClick={() => moderate.mutate({ productId: product.id, status: "REJECTED" })} type="button">
                    <XCircle size={16} /> Tu choi
                  </button>
                </div>
              )}
            </div>
          ))}
          {!products.length && <p className="muted">Chưa có sản phẩm.</p>}
        </div>
      </section>

      {isAdmin && (
        <section className="panel">
          <h2><ShieldCheck size={18} /> Danh mục</h2>
          <div className="stack-list">
            {categories.map((category) => (
              <div className="soft-row" key={category.id}>
                <strong>{category.name} - {category.status}</strong>
                <span>{category.slug} - {category.productCount} sản phẩm</span>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
