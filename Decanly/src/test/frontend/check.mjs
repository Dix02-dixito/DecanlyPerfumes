// Pruebas de interacción sin servidor, sobre HTML renderizado por MockMvc.
// Ejecutar primero ./mvnw test; luego npm ci y npm test en esta carpeta.
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { fileURLToPath } from "node:url";
import { JSDOM, VirtualConsole } from "jsdom";
import * as css from "css-tree";

const root = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "../../..",
);
const rendered = path.join(root, "target/frontend-qa");
const resources = path.join(root, "src/main/resources/static");
const session = new Map();
let live = null,
  checks = 0;
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
async function until(test, message = "La interacción no terminó") {
  for (let i = 0; i < 250; i++) {
    if (test()) return;
    await sleep(4);
  }
  throw new Error(message);
}
function check(value, message) {
  assert.ok(value, message);
  checks++;
}
function finishPage() {
  if (!live) return;
  session.clear();
  for (let i = 0; i < live.w.sessionStorage.length; i++) {
    const key = live.w.sessionStorage.key(i);
    session.set(key, live.w.sessionStorage.getItem(key));
  }
  live.w.close();
  live = null;
}
async function page(file, url, options = {}) {
  finishPage();
  const errors = [],
    navigation = [],
    opened = [],
    calls = [];
  const console = new VirtualConsole();
  console.on("jsdomError", (error) => errors.push(error));
  const dom = new JSDOM(
    fs.readFileSync(path.join(rendered, file + ".html"), "utf8"),
    {
      url: "https://decanly.test" + url,
      runScripts: "outside-only",
      pretendToBeVisual: true,
      virtualConsole: console,
    },
  );
  const w = dom.window,
    ctx = dom.getInternalVMContext();
  for (const [key, value] of session) w.sessionStorage.setItem(key, value);
  w.structuredClone = structuredClone;
  w.TextEncoder = TextEncoder;
  Object.defineProperty(w.crypto, "subtle", {
    value: (await import("node:crypto")).webcrypto.subtle,
  });
  w.matchMedia = (query) => ({
    media: query,
    matches: true,
    addEventListener() {},
    removeEventListener() {},
  });
  w.scrollTo = () => {};
  w.HTMLElement.prototype.scrollIntoView = function () {
    this.dataset.scrolled = "yes";
  };
  w.HTMLElement.prototype.scrollTo = function (v) {
    this.scrollLeft = v.left || 0;
  };
  w.HTMLElement.prototype.scrollBy = function (v) {
    this.scrollLeft += v.left || 0;
  };
  w.HTMLDialogElement.prototype.showModal = function () {
    this.open = true;
  };
  w.HTMLDialogElement.prototype.close = function () {
    this.open = false;
    this.dispatchEvent(new w.Event("close"));
  };
  w.URL.createObjectURL = () => "blob:photo";
  w.URL.revokeObjectURL = () => {};
  w.open = (...args) => {
    opened.push(args);
    return null;
  };
  w.__navigate = (value) => navigation.push(value);
  w.fetch =
    options.fetch ||
    (async (url, init = {}) => {
      calls.push({ url, init });
      if (url.endsWith("api/packs"))
        return {
          ok: true,
          status: 200,
          json: async () =>
            JSON.parse(
              fs.readFileSync(path.join(rendered, "packs.json"), "utf8"),
            ),
        };
      if (url.endsWith("api/sesion") && init.method === "POST")
        return {
          ok: true,
          status: 200,
          json: async () => ({
            user: { email: "test@decanly.pe", role: "ADMIN" },
          }),
        };
      if (url.endsWith("api/sesion") && init.method === "DELETE")
        return { ok: true, status: 204 };
      throw new Error("Petición inesperada: " + url);
    });
  const evaluate = (code) => vm.runInContext(code, ctx);
  for (const script of w.document.querySelectorAll("script:not([src])"))
    evaluate(script.textContent);
  for (const script of w.document.querySelectorAll("script[src]")) {
    let code = fs.readFileSync(
      path.join(resources, "js", path.basename(script.src)),
      "utf8",
    );
    evaluate(code);
    if (script.src.endsWith("estado.js"))
      evaluate("navigate = path => window.__navigate(path)");
  }
  const result = {
    w,
    dom,
    evaluate,
    navigation,
    opened,
    calls,
    errors,
    $: (selector) => w.document.querySelector(selector),
    $$: (selector) => [...w.document.querySelectorAll(selector)],
    json: (code) => JSON.parse(JSON.stringify(evaluate(code))),
    async click(selector) {
      const button = this.$(selector);
      assert.ok(button, "No existe " + selector);
      button.click();
      await sleep(8);
    },
    input(selector, value, type = "input") {
      const field = this.$(selector);
      assert.ok(field, "No existe " + selector);
      field.value = value;
      field.dispatchEvent(new w.Event(type, { bubbles: true }));
    },
    async submit(selector) {
      const form = this.$(selector);
      assert.ok(form, selector);
      form.dispatchEvent(
        new w.Event("submit", { bubbles: true, cancelable: true }),
      );
      await until(() => !form.dataset.busy);
      await sleep(5);
    },
  };
  live = result;
  await until(
    () =>
      evaluate("ready") ||
      navigation.length ||
      !result.$("#connection-status").hidden,
    "No terminó el inicio: " + file,
  );
  if (!options.allowFailure)
    check(
      result.$("#connection-status").hidden,
      result.$("#connection-status").textContent,
    );
  check(errors.length === 0, errors.map((e) => e.message).join("\n"));
  return result;
}

try {
  let p = await page("catalogo", "/catalogo");
  check(
    p.calls.some((c) => c.url === "/api/packs"),
    "Packs obtenidos por API",
  );
  check(p.$$(".card").length === 1, "Catálogo real del modelo Spring");
  check(p.$(".card-image img").src.includes("/imagenes/"), "Foto de la base");
  await p.click('[data-action="menu"]');
  check(p.$(".site-header").classList.contains("menu-open"), "Menú móvil abre");
  await p.click('[data-action="toggle-search"]');
  check(p.$(".site-header").classList.contains("search-open"), "Búsqueda abre");
  check(
    !p.$(".site-header").classList.contains("menu-open"),
    "Menú y búsqueda no se superponen",
  );
  const search = p.$("#header-search").form;
  check(
    search.method === "get" && search.action.endsWith("/catalogo"),
    "Header busca mediante GET",
  );
  p.input("#search", "No existe");
  check(p.$$(".card").length === 0, "Filtro sin coincidencias");
  p.input("#search", "");
  await p.click('[data-action="detail"]');
  check(p.$("#product-modal").open, "Modal abre");
  check(
    p.$(".detail-photo-slide img").src.includes("/imagenes/"),
    "Galería desde MySQL",
  );
  await p.click('[data-action="detail-size"][data-size="5"]');
  await p.click('#product-modal [data-action="add"]');
  check(p.json("cart")[0].size === "5", "Añadir tamaño correcto");
  check(p.w.localStorage.length === 0, "Sin catálogo en localStorage");
  p = await page("carrito", "/carrito");
  check(
    p.$$("#checkout input").length === 4,
    "Nombre, teléfono, dirección y términos",
  );
  p.input("#customer", "Cliente de prueba");
  p.input("#customer-phone", "999888777");
  p.input("#delivery-address", "Av. Prueba 123, Lima");
  p.$('[name="terms"]').checked = true;
  await p.submit("#checkout");
  check(p.opened.length === 1, "WhatsApp preparado");
  const url = new URL(p.opened[0][0]),
    msg = url.searchParams.get("text");
  check(
    url.host === "wa.me" &&
      msg.includes("Cliente de prueba") &&
      msg.includes("5 ml"),
    "Mensaje con datos correctos",
  );
  check(
    msg.includes("Por confirmar con el vendedor"),
    "Sin referencia ficticia de pedido",
  );
  check(
    !p.calls.some((c) => c.url.includes("pedidos")),
    "No finge guardar pedidos",
  );
  p = await page("promociones", "/promociones");
  check(p.$$(".promo").length === 1, "Pack recibido del API");
  await p.click('[data-action="promo-cart"]');
  check(
    p.json("cart").some((l) => l.size === "10" && l.qty === 2 && l.group),
    "Pack con perfume y tamaño correctos",
  );
  p = await page("login", "/cuenta");
  p.input("#email", "test@decanly.pe");
  p.input("#password", "Clave de prueba exclusiva");
  await p.submit("#login");
  const req = p.calls.find((c) => c.url.endsWith("api/sesion"));
  check(
    req.init.method === "POST" &&
      JSON.parse(req.init.body).email === "test@decanly.pe",
    "Login JSON al backend",
  );
  check(
    req.init.headers[p.$('meta[name="csrf-header"]').content] ===
      p.$('meta[name="csrf-token"]').content,
    "Login incluye CSRF",
  );
  check(
    req.init.credentials === "same-origin" &&
      p.navigation.includes("/producto"),
    "Cookie y navegación al panel",
  );
  p = await page("cuenta", "/cuenta");
  await p.click('[data-action="logout"]');
  check(
    p.calls.some(
      (c) => c.url.endsWith("api/sesion") && c.init.method === "DELETE",
    ),
    "Logout por API",
  );
  for (const file of [
    "producto",
    "create",
    "edit",
    "promocion",
    "pendiente",
    "inicio",
    "packs",
    "contacto",
    "terminos",
    "envios",
    "privacidad",
  ]) {
    p = await page(file, "/" + file);
    check(!p.$("#app").inert, "Página operativa: " + file);
    if (["create", "edit"].includes(file)) {
      const form = p.$("#product-form"),
        event = new p.w.Event("submit", { bubbles: true, cancelable: true });
      form.dispatchEvent(event);
      check(!event.defaultPrevented, "Perfumes conserva POST nativo");
      check(form.querySelector('[name="_csrf"]'), "Formulario CSRF");
      check(form.enctype === "multipart/form-data", "Formulario con fotos");
    }
  }
  p = await page("contexto", "/decanly/catalogo");
  check(
    p.calls.some((c) => c.url === "/decanly/api/packs"),
    "API respeta context-path",
  );
  check(
    p.$(".card-image img").src.includes("/decanly/imagenes/"),
    "Foto respeta context-path",
  );
  p = await page("catalogo", "/catalogo", {
    allowFailure: true,
    fetch: async () => ({
      ok: false,
      status: 500,
      json: async () => ({ message: "Falló MySQL" }),
    }),
  });
  check(!p.$("#connection-status").hidden, "Error visible");
  check(p.$$(".card").length === 0, "Sin productos simulados al fallar el API");
  for (const name of fs.readdirSync(path.join(resources, "css")))
    if (name.endsWith(".css"))
      css.parse(fs.readFileSync(path.join(resources, "css", name), "utf8"));
  check(true, "Sintaxis CSS");
  console.log(
    `${checks} comprobaciones del frontend aprobadas. Se usan respuestas preparadas de API; JUnit y MySQL comprueban el servidor.`,
  );
} finally {
  finishPage();
}
