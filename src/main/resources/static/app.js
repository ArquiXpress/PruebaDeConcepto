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
const ROLE_LABELS = {
  CLIENT: "Cliente",
  SELLER: "Vendedor",
  ADMIN: "Administrador"
};
const DEFAULT_PRICE_LABEL = "Precio por confirmar";

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
const headerFavoritesCount = document.getElementById("headerFavoritesCount");
const headerCartCount = document.getElementById("headerCartCount");
const accountCaption = document.getElementById("accountCaption");

function currentRole() {
  return USERS.find(u => u.id === state.userId)?.role || "CLIENT";
}

function roleLabel(role) {
  return ROLE_LABELS[role] || ROLE_LABELS.CLIENT;
}

function cleanText(value, fallback = "") {
  const text = typeof value === "string" ? value.trim() : "";
  return text || fallback;
}

function truncateText(value, maxLength) {
  if (value.length <= maxLength) {
    return value;
  }
  return `${value.slice(0, maxLength - 1).trimEnd()}…`;
}

function humanizeCategory(category) {
  return cleanText(category, "General")
    .replace(/[_-]+/g, " ")
    .replace(/\b\w/g, letter => letter.toUpperCase());
}

function productDescription(product) {
  const description = cleanText(product?.description);
  if (!description || /^producto de marketplace/i.test(description)) {
    return `Disponible en ${humanizeCategory(product?.category)} con stock actualizado.`;
  }
  return description;
}

function productTitle(product) {
  return cleanText(product?.title, "Producto disponible");
}

function productPrice(product) {
  return Number.isFinite(product?.price) ? money.format(product.price) : DEFAULT_PRICE_LABEL;
}

function totalCartItems() {
  return state.cart.reduce((sum, item) => sum + item.quantity, 0);
}

function cartLineTotal(item) {
  return money.format((item.product?.price || 0) * item.quantity);
}

function updateHeaderStatus() {
  headerFavoritesCount.textContent = String(state.favorites.length);
  headerCartCount.textContent = String(totalCartItems());
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
  userSelect.innerHTML = USERS.map(u => `<option value="${u.id}">${u.name} · ${roleLabel(u.role)}</option>`).join("");
  userSelect.value = state.userId;
  const user = USERS.find(u => u.id === state.userId);
  sessionMeta.textContent = `${user.name} · Perfil activo: ${roleLabel(user.role)}.`;
  accountCaption.textContent = roleLabel(user.role);
  roleQuick.innerHTML = user.role === "CLIENT"
    ? `<strong>Cliente</strong><span>Compra productos, guarda favoritos y revisa tu carrito en segundos.</span>`
    : user.role === "SELLER"
      ? `<strong>Vendedor</strong><span>Actualiza stock y mantén el inventario visible para tus clientes.</span>`
      : `<strong>Administrador</strong><span>Supervisa catálogo, stock y operación general desde la misma vista.</span>`;
  roleExplainer.innerHTML = user.role === "CLIENT"
    ? "<strong>Cliente:</strong> explora el catálogo, guarda favoritos y compra solo productos con stock disponible."
    : user.role === "SELLER"
      ? "<strong>Vendedor:</strong> revisa el catálogo y ajusta inventario rápidamente desde cada tarjeta."
      : "<strong>Administrador:</strong> supervisa el catálogo, ajusta stock y revisa el estado general de la operación.";
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
  const title = productTitle(product);
  const initials = title.split(" ").slice(0, 2).map(word => word[0]).join("").toUpperCase();
  const category = humanizeCategory(product?.category).toUpperCase();
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
      <text x="82" y="356" font-size="28" font-family="Arial, sans-serif" fill="#1f2937" font-weight="700">${category}</text>
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
      <img class="product-image" src="${productIllustration(product)}" alt="${productTitle(product)}" />
      <div class="tag">${humanizeCategory(product.category)}</div>
      <strong>${productTitle(product)}</strong>
      <div class="mini">${truncateText(productDescription(product), 74)}</div>
    </article>
  `).join("");
  catalogEl.innerHTML = filtered.map(product => `
    <div class="card">
      <img class="product-image" src="${productIllustration(product)}" alt="${productTitle(product)}" />
      <div class="card-body">
        <div class="card-meta">
          <div class="tag">${humanizeCategory(product.category)}</div>
          <div class="role-badge">${role === "CLIENT" ? "Compra" : role === "SELLER" ? "Inventario" : "Admin"}</div>
        </div>
        <h3>${productTitle(product)}</h3>
        <p class="short-desc">${truncateText(productDescription(product), 96)}</p>
        <div class="price-row">
          <strong class="price">${productPrice(product)}</strong>
          <div class="muted stock-copy">Stock: ${product.stockAvailable}</div>
        </div>
        <div class="actions">
        ${role === "CLIENT" ? `
          <button class="btn-primary" data-cart="${product.id}">Agregar</button>
          <button class="btn-secondary" data-fav="${product.id}">${state.favorites.includes(product.id) ? "Quitar" : "Guardar"}</button>
        ` : `
          <button class="btn-secondary" data-stock-dec="${product.id}">-1</button>
          <button class="btn-secondary" data-stock-add="${product.id}">+1</button>
          <input class="mini-input" data-stock-input="${product.id}" type="number" min="0" value="${product.stockAvailable}" />
          <button class="btn-primary" data-stock="${product.id}">Guardar stock</button>
        `}
        </div>
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
        return `
          <div class="item list-item">
            <div class="item-copy">
              <strong>${productTitle(product) || id}</strong>
              <div class="muted">${humanizeCategory(product?.category)}</div>
            </div>
            <button class="btn-secondary item-action" data-remove-fav="${id}">Quitar</button>
          </div>`;
      }).join("")
    : `<div class="empty-state">Aun no tienes favoritos guardados.</div>`;

  cartEl.innerHTML = cartDetailed.length
    ? cartDetailed.map(item => `
      <div class="item list-item">
        <div class="item-copy">
          <strong>${productTitle(item.product) || item.productId}</strong>
          <div class="muted">${item.quantity} x ${productPrice(item.product)}</div>
        </div>
        <div class="item-total">${cartLineTotal(item)}</div>
      </div>
    `).join("")
    : `<div class="empty-state">Tu carrito esta vacio por ahora.</div>`;

  const total = cartDetailed.reduce((sum, item) => sum + (item.product?.price || 0) * item.quantity, 0);
  cartSummary.textContent = `Productos: ${totalCartItems()} · Total estimado: ${money.format(total)}`;
  updateHeaderStatus();

  inventoryEl.innerHTML = state.products.map(p => `
    <div class="item list-item">
      <div class="item-copy">
        <strong>${productTitle(p)}</strong>
        <div class="muted">${humanizeCategory(p.category)}</div>
      </div>
      <div class="item-total">Stock: ${p.stockAvailable}</div>
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

function removeFavorite(productId) {
  if (!state.favorites.includes(productId)) {
    return;
  }
  state.favorites = state.favorites.filter(id => id !== productId);
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

favoritesEl.addEventListener("click", (event) => {
  const removeFavId = event.target.dataset.removeFav;
  if (removeFavId) removeFavorite(removeFavId);
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
