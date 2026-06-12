import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, CreditCard, Loader2, PackageCheck } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { marketplaceApi } from "../api/marketplaceApi";
import { EmptyState } from "../components/EmptyState";
import { useCart } from "../state/CartContext";
import { formatMoney } from "../utils/format";

export default function CheckoutPage() {
  const navigate = useNavigate();
  const { cart, loading: cartLoading, refreshCart } = useCart();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const [selectedProvinceId, setSelectedProvinceId] = useState("");
  const [selectedDistrictId, setSelectedDistrictId] = useState("");
  const [selectedWardCode, setSelectedWardCode] = useState("");

  const summaryQuery = useQuery({
    queryKey: ["checkout-summary", cart.totalQuantity, selectedDistrictId, selectedWardCode],
    queryFn: () => marketplaceApi.checkoutSummary(selectedDistrictId, selectedWardCode),
    enabled: !cartLoading && cart.shops.length > 0
  });
  const provinces = useQuery({ queryKey: ["shipping-provinces"], queryFn: marketplaceApi.provinces });
  const districts = useQuery({ 
    queryKey: ["shipping-districts", selectedProvinceId], 
    queryFn: () => marketplaceApi.districts(selectedProvinceId),
    enabled: !!selectedProvinceId
  });
  const wards = useQuery({ 
    queryKey: ["shipping-wards", selectedDistrictId], 
    queryFn: () => marketplaceApi.wards(selectedDistrictId),
    enabled: !!selectedDistrictId
  });

  useEffect(() => {
    if (!cartLoading && !cart.shops.length) {
      navigate("/cart", { replace: true });
    }
  }, [cartLoading, cart.shops.length, navigate]);

  if (cartLoading || (summaryQuery.isLoading && !summaryQuery.data)) {
    return (
      <section className="loading-panel">
        <Loader2 className="spin" size={22} /> Dang tinh checkout...
      </section>
    );
  }

  if (!cart.shops.length) {
    return <EmptyState title="Can co san pham trong gio hang" />;
  }

  const summary = summaryQuery.data;
  const provinceOptions = provinces.data || [];
  const districtOptions = districts.data || [];
  const wardOptions = wards.data || [];
  const selectedProvince = provinceOptions.find((item) => String(item.code) === String(selectedProvinceId));
  const selectedDistrict = districtOptions.find((item) => String(item.code) === String(selectedDistrictId));
  const selectedWard = wardOptions.find((item) => String(item.code) === String(selectedWardCode));
  const locationError = provinces.error || districts.error || wards.error || summaryQuery.error;

  async function submitCheckout(event) {
    event.preventDefault();
    setError("");
    if (!cart.shops.length || !cart.canCheckout || !summary?.canCheckout) {
      setError("Gio hang chua san sang de dat hang.");
      return;
    }

    setSubmitting(true);
    try {
      const formData = new FormData(event.currentTarget);
      const payload = Object.fromEntries(formData.entries());
      payload.provinceName = selectedProvince?.name || payload.province;
      payload.districtName = selectedDistrict?.name || payload.district;
      payload.wardName = selectedWard?.name || payload.ward;
      const order = await marketplaceApi.checkout(payload);
      await refreshCart();

      if (order.nextAction === "PAYMENT_REQUIRED") {
        localStorage.setItem("pendingPaymentOrderId", order.orderId);
        const payment = await marketplaceApi.createVnpayPayment(order.orderId);
        window.location.href = payment.paymentUrl;
        return;
      }

      navigate(`/orders/${order.orderId}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="two-col">
      <form className="checkout-form" onSubmit={submitCheckout}>
        <div className="page-head">
          <div>
            <h1>Checkout</h1>
            <p className="muted">Don co the giao thanh nhieu kien tu nhieu shop.</p>
          </div>
          <Link className="btn secondary" to="/cart">
            <ArrowLeft size={17} /> Gio hang
          </Link>
        </div>

        {error && <div className="alert error">{error}</div>}

        <section className="form-section">
          <h2>Nguoi nhan</h2>
          {locationError && <div className="alert error">{locationError.message}</div>}
          <label>
            Ho ten
            <input name="receiverName" required placeholder="Nguyen Van A" />
          </label>
          <label>
            Dien thoai
            <input name="phone" required placeholder="0900000000" />
          </label>
          <label>
            Tinh/Thanh
            <select name="province" required value={selectedProvinceId} onChange={e => {
                setSelectedProvinceId(e.target.value);
                setSelectedDistrictId("");
                setSelectedWardCode("");
            }}>
              <option value="">Chon Tinh/Thanh</option>
              {provinces.isLoading && <option value="">Dang tai tu GHN...</option>}
              {provinceOptions.map((item) => (
                <option key={item.code} value={item.code}>
                  {item.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Quan/Huyen
            <select name="district" required value={selectedDistrictId} onChange={e => {
                setSelectedDistrictId(e.target.value);
                setSelectedWardCode("");
            }} disabled={!selectedProvinceId}>
              <option value="">Chon Quan/Huyen</option>
              {districts.isLoading && <option value="">Dang tai tu GHN...</option>}
              {districtOptions.map((item) => (
                <option key={item.code} value={item.code}>
                  {item.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Phuong/Xa
            <select name="ward" required value={selectedWardCode} onChange={e => setSelectedWardCode(e.target.value)} disabled={!selectedDistrictId}>
              <option value="">Chon Phuong/Xa</option>
              {wards.isLoading && <option value="">Dang tai tu GHN...</option>}
              {wardOptions.map((item) => (
                <option key={item.code} value={item.code}>
                  {item.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Dia chi
            <input name="address" required placeholder="So nha, ten duong" />
          </label>
        </section>

        <section className="form-section">
          <h2>Thanh toan</h2>
          <div className="payment-grid">
            <label>
              <input type="radio" name="paymentMethod" value="VNPAY" defaultChecked />
              <CreditCard size={18} /> VNPay
            </label>
            <label>
              <input type="radio" name="paymentMethod" value="COD" />
              <PackageCheck size={18} /> COD
            </label>
          </div>
        </section>

        <button className="btn primary full" disabled={submitting || summaryQuery.isFetching || !summary?.canCheckout} type="submit">
          <PackageCheck size={18} /> {submitting ? "Dang tao order..." : "Dat hang"}
        </button>
      </form>

      <aside className="summary-panel">
        <h2>Goi hang</h2>
        {summary?.shopSummaries?.map((shop) => (
          <div className="mini-card" key={shop.shopId}>
            <div className="summary-row">
              <span>{shop.shopName}</span>
              <strong>{formatMoney(shop.total)}</strong>
            </div>
            <div className="summary-row muted">
              <span>Hang</span>
              <span>{formatMoney(shop.itemSubtotal)}</span>
            </div>
            <div className="summary-row muted">
              <span>GHN</span>
              <span>{formatMoney(shop.shippingFee)}</span>
            </div>
          </div>
        ))}
        <div className="summary-row">
          <span>Tam tinh</span>
          <strong>{formatMoney(summary?.subtotal)}</strong>
        </div>
        <div className="summary-row">
          <span>Phi ship</span>
          <strong>{formatMoney(summary?.shippingTotal)}</strong>
        </div>
        <div className="summary-row total">
          <span>Tong</span>
          <strong>{formatMoney(summary?.grandTotal)}</strong>
        </div>
      </aside>
    </div>
  );
}
