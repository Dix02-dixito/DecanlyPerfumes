"use strict";
// Interacciones visuales. Los formularios de perfumes usan el POST nativo de Spring MVC.

function feedback(form, message, error = false) {
  let node = form.querySelector(".form-feedback");
  if (!node) {
    node = document.createElement("p");
    node.className = "form-feedback";
    node.setAttribute("role", "status");
    form.append(node);
  }
  node.classList.toggle("error", error);
  node.textContent = message;
}
function showError(error) {
  toast(error.message || "No se pudo completar la acción.");
}
function queryFilter() {
  const q = new URLSearchParams(location.search);
  filter = {
    q: q.get("q") || "",
    brand: q.get("marca") || "",
    category: q.get("estilo") || "",
    occasion: q.get("ocasion") || "",
    sort: q.get("orden") || "featured",
  };
}
function rememberFilters() {
  const q = new URLSearchParams();
  for (const [key, value] of [
    ["q", filter.q],
    ["marca", filter.brand],
    ["estilo", filter.category],
    ["ocasion", filter.occasion],
    ["orden", filter.sort === "featured" ? "" : filter.sort],
  ])
    if (value) q.set(key, value);
  history.replaceState(null, "", location.pathname + (q.size ? "?" + q : ""));
}
function paintCatalog(initial = false) {
  if (initial && $(".filters")) $(".filters").outerHTML = filters();
  if ($("#search")) $("#search").value = filter.q;
  if ($("#brand-filter")) $("#brand-filter").value = filter.brand;
  if ($("#category-filter")) $("#category-filter").value = filter.category;
  if ($("#sort")) $("#sort").value = filter.sort;
  document
    .querySelectorAll('[data-action="occasion"]')
    .forEach((b) =>
      b.classList.toggle("selected", b.dataset.value === filter.occasion),
    );
  put($("#product-grid"), gridContent(route === "packs" ? "pack" : "cart"));
  if ($("#result-count"))
    $("#result-count").textContent =
      filtered().length + " fragancias para descubrir";
  if (route === "catalogo" && $(".page-head h1"))
    $(".page-head h1").textContent =
      filter.brand ||
      (filter.category
        ? "Aromas " + filter.category.toLowerCase()
        : "Tu próximo favorito.");
}
function paintPack() {
  put($("#pack-summary"), packSummary());
  if ($(".pack-mobile-bar")) $(".pack-mobile-bar").outerHTML = packMobileBar();
}
function paintCart() {
  const saved = DecanlyPedido.snapshot($("#checkout"));
  const holder = document.createElement("div");
  holder.innerHTML = cartPage();
  const section = holder.querySelector(".section"),
    current = app.querySelector(".section");
  if (current) current.replaceWith(section);
  else app.append(section);
  normalize(app);
  DecanlyPedido.restore($("#checkout"), saved);
}

function paintPromos() {
  const active = promos
    .filter((p) => p.active)
    .sort(
      (a, b) =>
        Number(!promoAvailable({ items: promoLines(a) })) -
        Number(!promoAvailable({ items: promoLines(b) })),
    );
  const shown = route === "inicio" ? active.slice(0, 3) : active;
  put(
    $(".promo-grid"),
    shown.map(promoCard).join("") ||
      '<div class="empty"><h2>Pronto tendremos más packs.</h2><a class="btn" href="#packs">Arma tu pack</a></div>',
  );
  if ($(".promotion-note b"))
    $(".promotion-note b").textContent = packDiscount + "% de ahorro";
  if (route === "promociones") {
    const grid = $(".promotions-section > .grid");
    if (grid)
      put(
        grid,
        stockFirst(products.filter((p) => p.discount > 0))
          .map((p) => card(p))
          .join(""),
      );
  }
}
function openDetail(id, view = null, inline = false) {
  if (!get(id)) {
    toast("Este perfume ya no está disponible.");
    return;
  }
  // El mismo componente sirve para el modal y la pantalla de detalle.
  detail(id, view);
  normalize(modal);
  if (inline) {
    put($("#standalone-detail"), modal.innerHTML);
    $("#standalone-detail .close")?.remove();
    modal.close();
    modal.innerHTML = "";
  }
  const host = inline ? $("#standalone-detail") : modal;
  DecanlyGallery.mountCarousel(
    host.querySelector(".detail-gallery"),
    (view) => {
      detailView = view;
    },
  );
}
function paintAfterSelection() {
  saveBasket();
  if (route === "carrito") paintCart();
  if (route === "packs") {
    paintPack();
    paintCatalog();
  }
}
function addProduct(id, target = "cart") {
  const p = get(id),
    s = size(id);
  if (!p) return false;
  const dest = target === "pack" ? pack : cart;
  const pending = target === "pack" ? reserved(id, s, pack) : 0;
  if (available(p, s) <= pending) {
    toast("No hay más unidades de este tamaño disponibles.");
    return false;
  }
  let line = dest.find((l) => l.id === id && l.size === s && !l.group);
  if (line) line.qty++;
  else dest.push({ id, size: s, qty: 1 });
  paintAfterSelection();
  return true;
}
function addPromo(id, buy = false) {
  const pr = promos.find((p) => p.id === id && p.active);
  if (!pr) return;
  const items = promoLines(pr);
  if (!promoAvailable({ items })) {
    toast("Este tamaño de pack está agotado.");
    return;
  }
  const group = "pack-" + crypto.randomUUID();
  cart.push(...items.map((l) => ({ ...l, group })));
  saveBasket();
  if (buy) navigate("/carrito");
  else {
    paintPromos();
    toast("Pack añadido al carrito.");
  }
}
document.addEventListener("click", async (e) => {
  if (e.target.closest("#main-nav a")) closeMenu();
  const b = e.target.closest("[data-action]");
  if (!b || b.disabled) return;
  const action = b.dataset.action,
    id = Number(b.dataset.id);
  if (action === "reload-data") {
    location.reload();
    return;
  }
  if (!ready && action !== "reset-local") {
    toast("Espera a que carguen los datos.");
    return;
  }
  try {
    if (action === "menu") {
      const open = $(".site-header").classList.toggle("menu-open");
      b.setAttribute("aria-expanded", open);
      b.setAttribute("aria-label", open ? "Cerrar menú" : "Abrir menú");
      if (open) closeSearch();
    } else if (action === "toggle-search") {
      closeMenu();
      const open = $(".site-header").classList.toggle("search-open");
      b.setAttribute("aria-expanded", String(open));
      if (open) $("#header-search").focus();
    } else if (action === "brand" || action === "category") {
      e.preventDefault();
      navigate(
        "/catalogo?" +
          (action === "brand"
            ? "marca=" + encodeURIComponent(b.dataset.brand)
            : "estilo=" + encodeURIComponent(b.dataset.category)),
      );
    } else if (action === "clear-filters") {
      filter = {
        q: "",
        brand: "",
        category: "",
        occasion: "",
        sort: "featured",
      };
      paintCatalog();
      rememberFilters();
    } else if (action === "occasion") {
      filter.occasion = b.dataset.value;
      paintCatalog();
      rememberFilters();
    } else if (action === "size") {
      selected[id] = b.dataset.size;
      b.closest(".card").outerHTML = card(
        get(id),
        route === "packs" ? "pack" : "cart",
      );
      normalize(app);
      saveBasket();
    } else if (action === "detail") openDetail(id);
    else if (action === "detail-view")
      DecanlyGallery.select(b.closest(".detail-gallery"), b.dataset.view);
    else if (action === "detail-size") {
      selected[id] = b.dataset.size;
      saveBasket();
      openDetail(id, detailView, Boolean(b.closest("#standalone-detail")));
    } else if (action === "close") modal.close();
    else if (
      action === "add" ||
      action === "pack-add" ||
      action === "buy-now"
    ) {
      if (addProduct(id, action === "pack-add" ? "pack" : "cart")) {
        if (action === "buy-now") {
          modal.close();
          navigate("/carrito");
          return;
        }
        const cardNode = b.closest(".card");
        if (cardNode?.isConnected) {
          cardNode.outerHTML = card(
            get(id),
            route === "packs" ? "pack" : "cart",
          );
          normalize(app);
        }
        if (modal.open) {
          openDetail(id, detailView);
          $("#detail-feedback").textContent =
            "Añadido " + (action === "pack-add" ? "al pack." : "al carrito.");
        } else
          toast(
            action === "pack-add"
              ? "Añadido a tu pack."
              : "Añadido al carrito.",
          );
      }
    } else if (action === "promo-size") {
      promoSizes[id] = b.dataset.size;
      b.closest(".promo").outerHTML = promoCard(
        promos.find((p) => p.id === id),
      );
      normalize(app);
      saveBasket();
    } else if (action === "promo-cart" || action === "promo-buy")
      addPromo(id, action === "promo-buy");
    else if (action === "choose-promo") {
      const pr = promos.find((p) => p.id === id && p.active);
      if (!pr || !promoAvailable({ items: promoLines(pr) }))
        throw new Error("Revisa la disponibilidad de este pack.");
      pack = promoLines(pr);
      saveBasket();
      navigate("/packs");
    } else if (action === "pack-remove") {
      pack.splice(id, 1);
      paintAfterSelection();
    } else if (action === "pack-cart") {
      if (
        !pack.length ||
        pack.some((l) => available(get(l.id), l.size) < l.qty)
      )
        throw new Error("Revisa la disponibilidad de tu pack.");
      const group = "pack-" + crypto.randomUUID();
      cart.push(...pack.map((l) => ({ ...l, group })));
      pack = [];
      saveBasket();
      navigate("/carrito");
    } else if (action === "pack-review")
      $("#pack-summary").scrollIntoView({
        behavior: matchMedia("(prefers-reduced-motion: reduce)").matches
          ? "auto"
          : "smooth",
        block: "start",
      });
    else if (action === "remove") {
      cart.splice(id, 1);
      paintAfterSelection();
    } else if (action === "quantity") {
      const l = cart[id],
        delta = Number(b.dataset.delta);
      if (!l) return;
      if (delta > 0 && available(get(l.id), l.size) < 1)
        throw new Error("No hay más unidades disponibles.");
      l.qty += delta;
      if (l.qty <= 0) cart.splice(id, 1);
      paintAfterSelection();
    } else if (action === "logout") {
      await DecanlyAPI.logout();
      navigate("/cuenta");
    }
  } catch (error) {
    showError(error);
  }
});
document.addEventListener("input", (event) => {
  if (ready && event.target.id === "search") {
    filter.q = event.target.value;
    paintCatalog();
    rememberFilters();
  }
});
document.addEventListener("change", (event) => {
  const el = event.target,
    keys = {
      "brand-filter": "brand",
      "category-filter": "category",
      sort: "sort",
    };
  if (ready && keys[el.id]) {
    filter[keys[el.id]] = el.value;
    paintCatalog();
    rememberFilters();
  }
  if (el.name === "foto" && el.files[0]) {
    if (el.dataset.previewUrl) URL.revokeObjectURL(el.dataset.previewUrl);
    const url = URL.createObjectURL(el.files[0]);
    el.dataset.previewUrl = url;
    put(
      $("#photo-preview"),
      `<img src="${url}" alt="Vista previa de la nueva fotografía">`,
    );
  }
});
document.addEventListener("submit", async (event) => {
  const form = event.target;
  if (!["login", "checkout"].includes(form.id)) return;
  event.preventDefault();
  if (!form.reportValidity() || form.dataset.busy) return;
  const button = form.querySelector('button[type="submit"]');
  form.dataset.busy = "true";
  button.disabled = true;
  try {
    if (form.id === "login") {
      const session = await DecanlyAPI.login(
        form.elements.email.value.trim(),
        form.elements.password.value,
      );
      navigate(session.user?.role === "ADMIN" ? "/producto" : "/cuenta");
      return;
    }
    if (
      !ready ||
      !cart.length ||
      cart.some(
        (l) => !get(l.id) || reserved(l.id, l.size) > get(l.id).stock[l.size],
      )
    )
      throw new Error("Revisa tu selección y la disponibilidad.");
    const customer = DecanlyPedido.read(form),
      total = cartTotals().total,
      lines = cart.map((l) => ({
        ...l,
        brand: get(l.id).brand,
        name: get(l.id).name,
      }));
    const url = DecanlyWhatsApp.preparar({
      customer,
      lines,
      total,
      shipping: shippingLabel(total),
    });
    form.querySelector(".whatsapp-ready")?.remove();
    form.insertAdjacentHTML(
      "beforeend",
      `<a class="btn whatsapp-ready" href="${esc(url)}" target="_blank" rel="noopener noreferrer">Abrir WhatsApp ↗</a>`,
    );
    feedback(
      form,
      "Mensaje preparado. El vendedor confirma stock, total y entrega. Esta consulta no reserva productos.",
    );
    window.open(url, "_blank", "noopener,noreferrer");
  } catch (error) {
    feedback(form, error.message, true);
  } finally {
    delete form.dataset.busy;
    button.disabled = false;
  }
});
modal.addEventListener("click", (e) => {
  if (e.target === modal) modal.close();
});
modal.addEventListener("close", () => modal.classList.remove("perfume-dialog"));
document.addEventListener("click", (e) => {
  if (!e.target.closest(".site-header")) {
    closeMenu();
    closeSearch();
  }
});
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") {
    const header = $(".site-header");
    const focusTarget = header.classList.contains("search-open")
      ? $('[data-action="toggle-search"]')
      : header.classList.contains("menu-open")
        ? $('[data-action="menu"]')
        : null;
    closeMenu();
    closeSearch();
    focusTarget?.focus();
  }
});

async function startFrontend() {
  app.inert = true;
  try {
    await refreshData({ syncBasket: false });
    loadBasket();
    queryFilter();
    if (route === "catalogo" && currentPage === "catalogo/index")
      paintCatalog(true);
    if (route === "packs") {
      paintCatalog(true);
      paintPack();
    }
    if (route === "carrito") paintCart();
    if (currentPage === "inicio/index") {
      put(
        $("#best-sellers"),
        stockFirst(products)
          .slice(0, 8)
          .map((p) => card(p))
          .join(""),
      );
      paintPromos();
    }
    if (route === "promociones") paintPromos();
    if (currentPage === "perfume/detalle")
      openDetail(
        Number(new URLSearchParams(location.search).get("id") || 1),
        "perfume",
        true,
      );
    if (currentPage === "legal/terminos") put(app, termsPage());
    if (currentPage === "legal/envios") put(app, shippingPage());
    if (currentPage === "legal/privacidad") put(app, privacyPage());
    document.querySelectorAll("#main-nav a").forEach((a) => {
      a.classList.toggle(
        "active",
        a.getAttribute("href") === pathTo(routePaths[route] || "/"),
      );
      if (a.classList.contains("active"))
        a.setAttribute("aria-current", "page");
      else a.removeAttribute("aria-current");
    });
    normalize(document);
    updateCount();
    DecanlyCarousel.init();
    DecanlyInfoCarousel.init();
    DecanlyUX.init();
    ready = true;
    app.inert = false;
    document.dispatchEvent(new CustomEvent("decanly:ready"));
  } catch (error) {
    app.inert = false;
    const status = $("#connection-status");
    status.hidden = false;
    put(
      status,
      `<span>${esc(error.message)}</span><button class="btn light" data-action="reload-data">Reintentar</button>`,
    );
  }
}
startFrontend();
