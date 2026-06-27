import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Loader2, PackagePlus, Pencil, RotateCcw, ShieldCheck, Tags, XCircle } from "lucide-react";
import { useState } from "react";
import { marketplaceApi } from "../api/marketplaceApi";
import { useAuth } from "../context/AuthContext";
import { formatMoney } from "../utils/format";

const emptyProduct = {
  name: "", sku: "", categoryId: 1, price: "", stock: 5, lowStockAlert: 2,
  tags: "", imageUrl: "", description: "", optionsJson: "", requiresPersonalization: false,
  customizable: false, processingDays: 3
};

export default function CatalogManagementPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  const queryClient = useQueryClient();
  const [productForm, setProductForm] = useState(emptyProduct);
  const [editingId, setEditingId] = useState(null);
  const [categoryName, setCategoryName] = useState("");
  const [inventoryDrafts, setInventoryDrafts] = useState({});
  const [actionError, setActionError] = useState("");

  const productsQuery = useQuery({
    queryKey: [isAdmin ? "admin-products" : "seller-products"],
    queryFn: isAdmin ? marketplaceApi.adminProducts : marketplaceApi.sellerProducts
  });
  const categoriesQuery = useQuery({ queryKey: ["categories"], queryFn: marketplaceApi.categories });

  const resetForm = () => {
    setProductForm(emptyProduct);
    setEditingId(null);
  };
  const refreshProducts = () => queryClient.invalidateQueries({ queryKey: [isAdmin ? "admin-products" : "seller-products"] });
  const createProduct = useMutation({ mutationFn: marketplaceApi.createSellerProduct, onSuccess: () => { refreshProducts(); resetForm(); }, onError: (err) => setActionError(err.message) });
  const updateProduct = useMutation({
    mutationFn: ({ productId, payload }) => marketplaceApi.updateSellerProduct(productId, payload),
    onSuccess: () => { refreshProducts(); resetForm(); },
    onError: (err) => setActionError(err.message)
  });
  const updateInventory = useMutation({
    mutationFn: ({ productId, payload }) => marketplaceApi.updateSellerInventory(productId, payload),
    onSuccess: refreshProducts,
    onError: (err) => setActionError(err.message)
  });
  const moderate = useMutation({
    mutationFn: ({ productId, status }) => marketplaceApi.moderateProduct(productId, status),
    onSuccess: refreshProducts,
    onError: (err) => setActionError(err.message)
  });
  const createCategory = useMutation({
    mutationFn: marketplaceApi.createCategory,
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["categories"] }); setCategoryName(""); },
    onError: (err) => setActionError(err.message)
  });

  if (productsQuery.isLoading || categoriesQuery.isLoading) {
    return <section className="loading-panel"><Loader2 className="spin" size={22} /> Đang tải catalog...</section>;
  }

  const products = productsQuery.data || [];
  const categories = categoriesQuery.data || [];
  const updateForm = (field, value) => setProductForm((current) => ({ ...current, [field]: value }));

  function productPayload() {
    return {
      ...productForm,
      categoryId: Number(productForm.categoryId),
      price: Number(productForm.price || 0),
      stock: Number(productForm.stock || 0),
      lowStockAlert: Number(productForm.lowStockAlert || 0),
      processingDays: Number(productForm.processingDays || 3)
    };
  }

  function editProduct(product) {
    setEditingId(product.id);
    setProductForm({
      name: product.name || "",
      sku: product.sku || "",
      categoryId: product.categoryId || 1,
      price: product.price ?? "",
      stock: product.stock ?? 0,
      lowStockAlert: product.lowStockAlert ?? 0,
      tags: product.tags || "",
      imageUrl: product.imageUrl || "",
      description: product.description || "",
      optionsJson: product.optionsJson || "",
      requiresPersonalization: Boolean(product.requiresPersonalization),
      customizable: Boolean(product.customizable),
      processingDays: product.processingDays || 3
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function inventoryValue(product, field) {
    return inventoryDrafts[product.id]?.[field] ?? product[field];
  }

  function setInventoryValue(product, field, value) {
    setInventoryDrafts((current) => ({
      ...current,
      [product.id]: { stock: product.stock, lowStockAlert: product.lowStockAlert, ...current[product.id], [field]: value }
    }));
  }

  return (
    <div className="dashboard-page">
      <section className="page-head">
        <div>
          <span className="section-kicker"><Tags size={16} /> Catalog</span>
          <h1>{isAdmin ? "Duyệt sản phẩm" : "Sản phẩm và tồn kho"}</h1>
          <p className="muted">Seller tạo hoặc sửa listing sẽ chuyển về chờ duyệt; mọi thay đổi tồn kho đều được ghi lịch sử.</p>
        </div>
      </section>
      {actionError && <section className="alert error">{actionError}</section>}

      {!isAdmin && (
        <form
          className="panel catalog-form"
          onSubmit={(event) => {
            event.preventDefault();
            setActionError("");
            if (editingId) updateProduct.mutate({ productId: editingId, payload: productPayload() });
            else createProduct.mutate(productPayload());
          }}
        >
          <h2>{editingId ? <><Pencil size={18} /> Sửa sản phẩm #{editingId}</> : <><PackagePlus size={18} /> Tạo sản phẩm mới</>}</h2>
          <input placeholder="Tên sản phẩm" value={productForm.name} onChange={(e) => updateForm("name", e.target.value)} required />
          <input placeholder="SKU" value={productForm.sku} onChange={(e) => updateForm("sku", e.target.value)} />
          <select value={productForm.categoryId} onChange={(e) => updateForm("categoryId", e.target.value)}>
            {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
          </select>
          <input placeholder="Giá" min="0" type="number" value={productForm.price} onChange={(e) => updateForm("price", e.target.value)} required />
          <input placeholder="Tồn kho" min="0" type="number" value={productForm.stock} onChange={(e) => updateForm("stock", e.target.value)} />
          <input placeholder="Ngưỡng cảnh báo sắp hết" min="0" type="number" value={productForm.lowStockAlert} onChange={(e) => updateForm("lowStockAlert", e.target.value)} />
          <input placeholder="Số ngày xử lý" min="1" type="number" value={productForm.processingDays} onChange={(e) => updateForm("processingDays", e.target.value)} />
          <input placeholder="Tag" value={productForm.tags} onChange={(e) => updateForm("tags", e.target.value)} />
          <input placeholder="URL ảnh" value={productForm.imageUrl} onChange={(e) => updateForm("imageUrl", e.target.value)} />
          <textarea placeholder="Mô tả" value={productForm.description} onChange={(e) => updateForm("description", e.target.value)} />
          <label className="check-row"><input type="checkbox" checked={productForm.customizable} onChange={(e) => updateForm("customizable", e.target.checked)} /> Sản phẩm custom</label>
          <label className="check-row"><input type="checkbox" checked={productForm.requiresPersonalization} onChange={(e) => updateForm("requiresPersonalization", e.target.checked)} /> Bắt buộc nhập nội dung cá nhân hóa</label>
          <div className="detail-actions">
            <button className="btn primary" disabled={createProduct.isPending || updateProduct.isPending} type="submit">{editingId ? "Lưu và gửi duyệt lại" : "Tạo và gửi duyệt"}</button>
            {editingId && <button className="btn secondary" onClick={resetForm} type="button"><RotateCcw size={16} /> Hủy sửa</button>}
          </div>
        </form>
      )}

      {isAdmin && (
        <form className="panel inline-form" onSubmit={(event) => { event.preventDefault(); if (categoryName.trim()) createCategory.mutate({ name: categoryName, status: "ACTIVE" }); }}>
          <input placeholder="Tên danh mục mới" value={categoryName} onChange={(e) => setCategoryName(e.target.value)} />
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
                <span className="muted">{product.categoryName} · {product.sku || "Chưa có SKU"}</span>
                <span>{formatMoney(product.price)} · Tồn {product.stock} · Cảnh báo {product.lowStockAlert}</span>
              </div>
              <div className="catalog-status"><span>{product.approvalStatus}</span><small>{product.saleStatus}</small></div>
              {!isAdmin && (
                <div className="inventory-actions">
                  <label><span>Tồn</span><input min="0" type="number" value={inventoryValue(product, "stock")} onChange={(e) => setInventoryValue(product, "stock", e.target.value)} /></label>
                  <label><span>Cảnh báo</span><input min="0" type="number" value={inventoryValue(product, "lowStockAlert")} onChange={(e) => setInventoryValue(product, "lowStockAlert", e.target.value)} /></label>
                  <button className="btn secondary" disabled={updateInventory.isPending} onClick={() => updateInventory.mutate({ productId: product.id, payload: { stock: Number(inventoryValue(product, "stock")), lowStockAlert: Number(inventoryValue(product, "lowStockAlert")) } })} type="button">Lưu kho</button>
                  <button className="btn secondary" onClick={() => editProduct(product)} type="button"><Pencil size={16} /> Sửa listing</button>
                </div>
              )}
              {isAdmin && (
                <div className="detail-actions">
                  <button className="btn secondary" onClick={() => moderate.mutate({ productId: product.id, status: "APPROVED" })} type="button"><CheckCircle2 size={16} /> Duyệt</button>
                  <button className="btn secondary danger-btn" onClick={() => moderate.mutate({ productId: product.id, status: "REJECTED" })} type="button"><XCircle size={16} /> Từ chối</button>
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
          <div className="stack-list">{categories.map((category) => <div className="soft-row" key={category.id}><strong>{category.name} · {category.status}</strong><span>{category.productCount} sản phẩm</span></div>)}</div>
        </section>
      )}
    </div>
  );
}
