"use strict";
// Cliente HTTP: la sesión y los packs pertenecen al backend de los compañeros.
const DecanlyAPI = (() => {
  async function request(endpoint, { method = "GET", body } = {}) {
    const base = document.querySelector('meta[name="decanly-base"]').content,
      headers = { Accept: "application/json" };
    if (body) headers["Content-Type"] = "application/json";
    if (method !== "GET") {
      const token = document.querySelector('meta[name="csrf-token"]')?.content,
        header = document.querySelector('meta[name="csrf-header"]')?.content;
      if (!token || !header)
        throw new Error("Recarga la página para actualizar el formulario.");
      headers[header] = token;
    }
    const response = await fetch(base + "api/" + endpoint, {
      method,
      headers,
      credentials: "same-origin",
      ...(body ? { body: JSON.stringify(body) } : {}),
    });
    if (response.status === 204) return null;
    const data = await response.json().catch(() => null);
    if (!response.ok)
      throw new Error(
        data?.message ||
          "No se pudo completar la solicitud. Recarga la página.",
      );
    return data;
  }
  return Object.freeze({
    listPacks: () => request("packs"),
    session: () => request("sesion"),
    login: (email, password) =>
      request("sesion", { method: "POST", body: { email, password } }),
    logout: () => request("sesion", { method: "DELETE" }),
  });
})();
