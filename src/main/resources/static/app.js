const USERS = [
  { id: "00000000-0000-0000-0000-000000000001", name: "Ana Cliente", role: "CLIENT" },
  { id: "00000000-0000-0000-0000-000000000002", name: "Luis Vendedor", role: "SELLER" },
  { id: "00000000-0000-0000-0000-000000000003", name: "Marta Admin", role: "ADMIN" }
];

const state = {
  products: [],
  cart: JSON.parse(localStorage.getItem("cart") || "[]"),
  favorites: JSON.parse(localStorage.getItem("favorites") || "[]"),
  userId: localStorage.getItem("userId") || USERS[0].id
};

const money = new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 });

const userSelect = document.getElementById("userSelect");
const queryInput = document.getElementById("queryInput");
const clearSearchBtn = document.getElementById("clearSearchBtn");
const sessionMeta = document.getElementById("sessionMeta");
const roleExplainer = document.getElementById("roleExplainer");
const roleQuick = document.getElementById("roleQuick");
const catalogEl = document.getElementById("catalog");
const featuredEl = document.getElementById("featured");
const favoritesEl = document.getElementById("favorites");
const cartEl = document.getElementById("cart");
const cartSummary = document.getElementById("cartSummary");
const inventoryEl = document.getElementById("inventory");
const checkoutBtn = document.getElementById("checkoutBtn");
const checkoutResult = document.getElementById("checkoutResult");

function currentRole() {
  return USERS.find(u => u.id === state.userId)?.role || "CLIENT";
}

function headers(extra = {}) {
  return {
    "Content-Type": "application/json",
    "X-User-Id": state.userId,
    "X-Roles": currentRole(),
    ...extra
  };
}

function persist() {
  localStorage.setItem("cart", JSON.stringify(state.cart));
  localStorage.setItem("favorites", JSON.stringify(state.favorites));
  localStorage.setItem("userId", state.userId);
}

function renderSession() {
  userSelect.innerHTML = USERS.map(u => `<option value="${u.id}">${u.name} (${u.role})</option>`).join("");
  userSelect.value = state.userId;
  const user = USERS.find(u => u.id === state.userId);
  sessionMeta.textContent = `Usuario demo: ${user.name}. Rol actual: ${user.role}.`;
  roleQuick.innerHTML = user.role === "CLIENT"
    ? `<strong>Cliente</strong><span>Comprar, guardar favoritos y pagar.</span>`
    : user.role === "SELLER"
      ? `<strong>Vendedor</strong><span>Editar stock y ver inventario.</span>`
      : `<strong>Administrador</strong><span>Supervisar catálogo y validar stock.</span>`;
  roleExplainer.innerHTML = user.role === "CLIENT"
    ? "<strong>Cliente:</strong> puede comprar, guardar favoritos y ver su carrito. Si no hay stock, la compra se bloquea."
    : user.role === "SELLER"
      ? "<strong>Vendedor:</strong> puede ver el catálogo, subir o bajar stock y preparar productos para venta."
      : "<strong>Admin:</strong> puede supervisar el catálogo, ajustar stock y validar el comportamiento general de la demo.";
}

function productIllustration(product) {
  const palette = {
    tecnologia: ["#fff8d6", "#ffd84d"],
    gaming: ["#fff1b8", "#ffb703"],
    telefonia: ["#fff7c7", "#ffca3a"],
    default: ["#fff8d6", "#ffd84d"]
  };
  const key = (product.category || "default").toLowerCase();
  const [light, strong] = palette[key] || palette.default;
  const initials = product.title.split(" ").slice(0, 2).map(word => word[0]).join("").toUpperCase();
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="640" height="420" viewBox="0 0 640 420">
      <defs>
        <linearGradient id="g" x1="0" x2="1" y1="0" y2="1">
          <stop offset="0%" stop-color="${light}" />
          <stop offset="100%" stop-color="${strong}" />
        </linearGradient>
      </defs>
      <rect width="640" height="420" rx="36" fill="url(#g)" />
      <circle cx="520" cy="82" r="88" fill="rgba(255,255,255,0.24)" />
      <rect x="82" y="86" width="238" height="238" rx="40" fill="rgba(255,255,255,0.58)" />
      <text x="201" y="228" text-anchor="middle" font-size="84" font-family="Arial, sans-serif" fill="#1f2937" font-weight="700">${initials}</text>
      <text x="82" y="356" font-size="28" font-family="Arial, sans-serif" fill="#1f2937" font-weight="700">${product.category.toUpperCase()}</text>
      <text x="82" y="390" font-size="22" font-family="Arial, sans-serif" fill="#1f2937">Stock: ${product.stockAvailable}</text>
    </svg>`;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

function renderCatalog() {
  const query = queryInput.value.trim().toLowerCase();
  const filtered = state.products.filter(p => !query || `${p.title} ${p.category}`.toLowerCase().includes(query));
  const sorted = [...filtered].sort((a, b) => b.stockAvailable - a.stockAvailable);
  const featured = sorted.slice(0, 3);
  const role = currentRole();
  featuredEl.innerHTML = featured.map(product => `
    <article class="featured-card">
      <img class="product-image" src="${productIllustration(product)}" alt="${product.title}" />
      <div class="tag">${product.category}</div>
      <strong>${product.title}</strong>
      <div class="mini">${product.description}</div>
    </article>
  `).join("");
  catalogEl.innerHTML = filtered.map(product => `
    <div class="card">
      <img class="product-image" src="${productIllustration(product)}" alt="${product.title}" />
      <div class="tag">${product.category}</div>
      <h3>${product.title}</h3>
      <p class="short-desc">${product.description || "Producto disponible para demo."}</p>
      <strong>${money.format(product.price)}</strong>
      <div class="muted">Stock disponible: ${product.stockAvailable}</div>
      <div class="role-badge">${role === "CLIENT" ? "Vista de compra" : role === "SELLER" ? "Vista de vendedor" : "Vista de administrador"}</div>
      <div class="actions">
        ${role === "CLIENT" ? `
          <button class="btn-primary" data-cart="${product.id}">Agregar al carrito</button>
          <button class="btn-secondary" data-fav="${product.id}">${state.favorites.includes(product.id) ? "Quitar favorito" : "Favorito"}</button>
        ` : `
          <button class="btn-secondary" data-stock-add="${product.id}">+1</button>
          <button class="btn-secondary" data-stock-dec="${product.id}">-</button>
          <input class="mini-input" data-stock-input="${product.id}" type="number" min="0" value="${product.stockAvailable}" />
          <button class="btn-primary" data-stock="${product.id}">Guardar stock</button>
        `}
      </div>
    </div>
  `).join("");
  applyRoleRules();
}

function renderSidebars() {
  const cartDetailed = state.cart.map(item => {
    const product = state.products.find(p => p.id === item.productId);
    return { ...item, product };
  });

  favoritesEl.innerHTML = state.favorites.length
    ? state.favorites.map(id => {
        const product = state.products.find(p => p.id === id);
        return `<div class="item"><strong>${product?.title || id}</strong><div class="muted">${product?.category || ""}</div></div>`;
      }).join("")
    : `<div class="muted">Sin favoritos.</div>`;

  cartEl.innerHTML = cartDetailed.length
    ? cartDetailed.map(item => `
      <div class="item">
        <strong>${item.product?.title || item.productId}</strong>
        <div class="muted">${item.quantity} x ${money.format(item.product?.price || 0)}</div>
      </div>
    `).join("")
    : `<div class="muted">Carrito vacío.</div>`;

  const total = cartDetailed.reduce((sum, item) => sum + (item.product?.price || 0) * item.quantity, 0);
  cartSummary.textContent = `Items: ${cartDetailed.length} | Total estimado: ${money.format(total)}`;

  inventoryEl.innerHTML = state.products.map(p => `
    <div class="item">
      <strong>${p.title}</strong>
      <div class="muted">Stock: ${p.stockAvailable}</div>
    </div>
  `).join("");
}

async function loadProducts() {
  const query = queryInput.value.trim();
  const url = `/api/products?size=100${query ? `&query=${encodeURIComponent(query)}` : ""}`;
  const response = await fetch(url, { headers: headers() });
  const page = await response.json();
  state.products = page.content || [];
  renderCatalog();
  renderSidebars();
}

function addToCart(productId) {
  const existing = state.cart.find(item => item.productId === productId);
  if (existing) {
    existing.quantity += 1;
  } else {
    state.cart.push({ productId, quantity: 1 });
  }
  persist();
  renderSidebars();
}

function toggleFavorite(productId) {
  if (state.favorites.includes(productId)) {
    state.favorites = state.favorites.filter(id => id !== productId);
  } else {
    state.favorites.push(productId);
  }
  persist();
  renderCatalog();
  renderSidebars();
}

async function updateStock(productId) {
  const input = catalogEl.querySelector(`[data-stock-input="${productId}"]`);
  const stock = Number(input?.value ?? 0);
  const response = await fetch(`/api/products/${productId}/stock`, {
    method: "PATCH",
    headers: headers(),
    body: JSON.stringify({ stock })
  });
  const data = await response.json();
  checkoutResult.textContent = response.ok
    ? `Stock actualizado: ${data.title} ahora tiene ${data.stockAvailable}`
    : `No se pudo actualizar el stock: ${data.message || "error"}`;
  await loadProducts();
}

function deltaStock(productId, delta) {
  const input = catalogEl.querySelector(`[data-stock-input="${productId}"]`);
  const current = Number(input?.value ?? 0);
  if (input) {
    input.value = String(Math.max(0, current + delta));
  }
}

function applyRoleRules() {
  const role = currentRole();
  checkoutBtn.disabled = role !== "CLIENT";
  document.getElementById("clientPanel").style.display = role === "CLIENT" ? "block" : "none";
  document.getElementById("cartPanel").style.display = role === "CLIENT" ? "block" : "none";
  document.getElementById("favoritesPanel").style.display = role === "CLIENT" ? "block" : "none";
  document.getElementById("sellerPanel").style.display = role === "CLIENT" ? "none" : "block";
  catalogEl.querySelectorAll("[data-cart]").forEach(button => button.disabled = role !== "CLIENT");
  catalogEl.querySelectorAll("[data-fav]").forEach(button => button.disabled = role !== "CLIENT");
  catalogEl.querySelectorAll("[data-stock]").forEach(button => button.disabled = role === "CLIENT");
  catalogEl.querySelectorAll("[data-stock-dec]").forEach(button => button.disabled = role === "CLIENT");
  catalogEl.querySelectorAll("[data-stock-add]").forEach(button => button.disabled = role === "CLIENT");
  catalogEl.querySelectorAll("[data-stock-input]").forEach(input => input.disabled = role === "CLIENT");
}

userSelect.addEventListener("change", async () => {
  state.userId = userSelect.value;
  persist();
  renderSession();
  applyRoleRules();
  await loadProducts();
});

queryInput.addEventListener("input", renderCatalog);
clearSearchBtn.addEventListener("click", () => {
  queryInput.value = "";
  renderCatalog();
});

catalogEl.addEventListener("click", (event) => {
  const cartId = event.target.dataset.cart;
  const favId = event.target.dataset.fav;
  const stockId = event.target.dataset.stock;
  const stockDecId = event.target.dataset.stockDec;
  const stockAddId = event.target.dataset.stockAdd;
  if (cartId) addToCart(cartId);
  if (favId) toggleFavorite(favId);
  if (stockId) updateStock(stockId);
  if (stockDecId) deltaStock(stockDecId, -1);
  if (stockAddId) deltaStock(stockAddId, 1);
});

checkoutBtn.addEventListener("click", async () => {
  if (!state.cart.length) {
    checkoutResult.textContent = "Agrega productos al carrito antes de simular el pago.";
    return;
  }
  const body = {
    items: state.cart.map(item => ({ productId: item.productId, quantity: item.quantity }))
  };
  const response = await fetch("/api/checkout", {
    method: "POST",
    headers: headers({ "Idempotency-Key": `demo-${Date.now()}` }),
    body: JSON.stringify(body)
  });
  const data = await response.json();
  checkoutResult.textContent = JSON.stringify(data, null, 2);
  if (response.ok && data.paymentStatus === "APPROVED") {
    state.cart = [];
    persist();
    renderSidebars();
  }
});

(async function init() {
  renderSession();
  applyRoleRules();
  await loadProducts();
})();
