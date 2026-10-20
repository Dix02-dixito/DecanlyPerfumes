"use strict";

// Carrusel visual de las imágenes guardadas en MySQL.
const DecanlyGallery = (() => {
  const escape = (text) =>
    String(text ?? "").replace(
      /[&<>"']/g,
      (c) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#39;",
        })[c],
    );
  const clone = (value) => structuredClone(value);
  const resolve = (src) =>
    typeof assetUrl === "function" ? assetUrl(src) : src;
  function entries(product) {
    if (Array.isArray(product.gallery)) {
      if (product.gallery.length) return clone(product.gallery);
      return product.image
        ? [{ id: "perfume", src: product.image, alt: product.name }]
        : [];
    }
    if (!product.image) return [];
    const first = {
      id: "perfume",
      src: product.image,
      alt: "Frasco original de " + (product.name || "tu perfume"),
    };
    return [first];
  }
  function photo(entry, decorative = false) {
    const alt = decorative ? "" : entry.alt || "Fotografía del perfume";
    if (entry.crop)
      return `<svg class="reference-photo" viewBox="${escape(entry.crop.viewBox)}" preserveAspectRatio="xMidYMid meet" ${decorative ? 'aria-hidden="true"' : `role="img" aria-label="${escape(alt)}"`}><image href="${escape(resolve(entry.src))}" width="${Number(entry.crop.width)}" height="${Number(entry.crop.height)}"/></svg>`;
    return `<img src="${escape(resolve(entry.src))}" alt="${escape(alt)}">`;
  }
  function markup(product, requested) {
    const list = entries(product);
    const index = Math.max(
      0,
      list.findIndex((entry) => String(entry.id) === String(requested)),
    );
    const active = list[index];
    if (!active)
      return {
        view: "",
        html: '<div class="detail-gallery"><p>Sin fotografía disponible.</p></div>',
      };
    const controls = (entry, i, thumbnail) =>
      `<button type="button" class="${i === index ? "selected" : ""}" data-action="detail-view" data-id="${product.id}" data-view="${escape(entry.id)}" aria-label="Ver imagen ${i + 1}: ${escape(entry.alt)}" aria-pressed="${i === index}">${thumbnail ? photo(entry, true) : "<span></span>"}</button>`;
    return {
      view: active.id,
      html: `<div class="detail-gallery" role="group" aria-roledescription="carrusel" aria-label="Fotografías de ${escape(product.name)}">
        <div class="detail-thumbnails" aria-label="Elegir fotografía">${list.map((entry, i) => controls(entry, i, true)).join("")}</div>
        <div class="detail-carousel">
          <div class="detail-art" data-gallery-viewport ${list.length > 1 ? 'tabindex="0" aria-label="Fotografías: desliza o usa las teclas izquierda y derecha"' : ""}>
            <div class="detail-photo-track" style="transform:translateX(-${index * 100}%)">
              ${list.map((entry, i) => `<div class="detail-photo-slide" data-photo-id="${escape(entry.id)}" data-photo-alt="${escape(entry.alt)}" role="group" aria-roledescription="diapositiva" aria-label="${i + 1} de ${list.length}" aria-hidden="${i !== index}" ${i !== index ? "inert" : ""}>${photo(entry)}</div>`).join("")}
            </div>
            ${list.length > 1 ? `<span class="detail-photo-counter" aria-hidden="true">${index + 1} / ${list.length}</span>` : ""}
          </div>
          <span class="detail-photo-label">${escape(active.alt)}</span>
          ${list.length > 1 ? `<div class="detail-photo-dots" aria-label="Páginas de fotografías">${list.map((entry, i) => controls(entry, i, false)).join("")}</div>` : ""}
          <span class="sr-only" data-gallery-status role="status" aria-live="polite" aria-atomic="true"></span>
        </div>
      </div>`,
    };
  }
  const carousels = new WeakMap();
  function mountCarousel(root, onChange = () => {}) {
    if (!root || carousels.has(root)) return;
    const viewport = root.querySelector("[data-gallery-viewport]");
    const track = root.querySelector(".detail-photo-track");
    const slides = [...root.querySelectorAll(".detail-photo-slide")];
    if (!viewport || !slides.length) return;
    const buttons = [...root.querySelectorAll('[data-action="detail-view"]')];
    const reduced = matchMedia("(prefers-reduced-motion: reduce)");
    let index = Math.max(
      0,
      slides.findIndex(
        (slide) => slide.getAttribute("aria-hidden") === "false",
      ),
    );
    let gesture = null;
    function show(next, announce = true) {
      index = (next + slides.length) % slides.length;
      viewport.classList.remove("is-dragging");
      track.style.transition = reduced.matches
        ? "none"
        : "transform 380ms ease";
      track.style.transform = `translateX(-${index * 100}%)`;
      const active = slides[index];
      slides.forEach((slide, i) => {
        slide.setAttribute("aria-hidden", String(i !== index));
        slide.toggleAttribute("inert", i !== index);
      });
      buttons.forEach((button) => {
        const selected = button.dataset.view === active.dataset.photoId;
        button.classList.toggle("selected", selected);
        button.setAttribute("aria-pressed", String(selected));
      });
      root.querySelector(".detail-photo-label").textContent =
        active.dataset.photoAlt;
      const counter = root.querySelector(".detail-photo-counter");
      if (counter) counter.textContent = `${index + 1} / ${slides.length}`;
      if (announce)
        root.querySelector("[data-gallery-status]").textContent =
          `Imagen ${index + 1} de ${slides.length}: ${active.dataset.photoAlt}`;
      onChange(active.dataset.photoId);
      // Mantener la miniatura seleccionada dentro de su tira sin mover la página.
      const thumb = root.querySelector(".detail-thumbnails .selected");
      const rail = thumb?.parentElement;
      if (rail && rail.scrollWidth > rail.clientWidth)
        rail.scrollTo({
          left: Math.max(
            0,
            thumb.offsetLeft -
              rail.offsetLeft -
              (rail.clientWidth - thumb.offsetWidth) / 2,
          ),
          behavior: reduced.matches ? "auto" : "smooth",
        });
      if (rail && rail.scrollHeight > rail.clientHeight)
        rail.scrollTo({
          top: Math.max(
            0,
            thumb.offsetTop -
              rail.offsetTop -
              (rail.clientHeight - thumb.offsetHeight) / 2,
          ),
          behavior: reduced.matches ? "auto" : "smooth",
        });
    }
    carousels.set(root, (id) => {
      const next = slides.findIndex(
        (slide) => slide.dataset.photoId === String(id),
      );
      if (next >= 0) show(next);
    });
    if (slides.length < 2) return;
    viewport.addEventListener("keydown", (event) => {
      if (event.altKey || event.ctrlKey || event.metaKey) return;
      const keys = {
        ArrowLeft: index - 1,
        ArrowRight: index + 1,
        Home: 0,
        End: slides.length - 1,
      };
      if (!(event.key in keys)) return;
      event.preventDefault();
      show(keys[event.key]);
    });
    viewport.addEventListener("pointerdown", (event) => {
      if (
        event.isPrimary === false ||
        (event.button !== undefined && event.button !== 0)
      )
        return;
      gesture = { id: event.pointerId, x: event.clientX, y: event.clientY };
      viewport.setPointerCapture?.(event.pointerId);
    });
    viewport.addEventListener("pointermove", (event) => {
      if (!gesture || event.pointerId !== gesture.id) return;
      const dx = event.clientX - gesture.x,
        dy = event.clientY - gesture.y;
      if (Math.abs(dx) > 6 && Math.abs(dx) > Math.abs(dy) * 1.3) {
        viewport.classList.add("is-dragging");
        track.style.transition = "none";
        track.style.transform = `translateX(calc(-${index * 100}% + ${dx}px))`;
      }
    });
    const release = (event, cancelled = false) => {
      if (!gesture || event.pointerId !== gesture.id) return;
      const dx = event.clientX - gesture.x,
        dy = event.clientY - gesture.y;
      gesture = null;
      if (viewport.hasPointerCapture?.(event.pointerId))
        viewport.releasePointerCapture(event.pointerId);
      if (!cancelled && Math.abs(dx) > 40 && Math.abs(dx) > Math.abs(dy) * 1.3)
        show(index + (dx < 0 ? 1 : -1));
      else show(index, false);
    };
    viewport.addEventListener("pointerup", (event) => release(event));
    viewport.addEventListener("pointercancel", (event) => release(event, true));
    viewport.addEventListener("lostpointercapture", (event) =>
      release(event, true),
    );
    viewport.addEventListener("dragstart", (event) => event.preventDefault());
  }
  function select(root, id) {
    carousels.get(root)?.(id);
  }
  return Object.freeze({ entries, photo, markup, mountCarousel, select });
})();
