async function loadProducts() {
  const wrap = document.getElementById("product-list");
  if (!wrap) return;
  const res = await fetch("/api/products");
  const data = await res.json();
  wrap.innerHTML = data.map((p) => `
    <article class="card">
      <div class="ph"></div>
      <div class="card-body">
        <h3>${p.name}</h3>
        <p class="muted">${p.category} • ${p.artisan}</p>
        <strong>$${p.price.toFixed(2)}</strong>
      </div>
    </article>
  `).join("");
}

async function loadOrders() {
  const wraps = document.querySelectorAll("[data-orders]");
  if (!wraps.length) return;
  const res = await fetch("/api/orders");
  const orders = await res.json();
  const html = orders.map((o) => `
    <article class="panel" style="margin-bottom:16px">
      <h3>Order #${o.id}</h3>
      <p class="muted">Status: ${o.status}</p>
      <p>${o.receiverName} - ${o.shippingAddress}</p>
      <strong>Total: $${o.total.toFixed(2)}</strong>
    </article>
  `).join("");
  wraps.forEach((el) => { el.innerHTML = html; });
}

loadProducts();
loadOrders();
