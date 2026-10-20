"use strict";
// Click to Chat: no registra pedidos ni envía mensajes automáticamente.
const DecanlyWhatsApp = (() => {
  function preparar({ customer, lines, total, shipping }) {
    const phone = String(window.DECANLY_DATOS.sellerPhone);
    if (!/^51\d{9}$/.test(phone))
      throw new Error("Revisa el número del vendedor.");
    const text = DecanlyPedido.message({
      reference: "Por confirmar con el vendedor",
      customer,
      lines,
      total,
      shipping,
    });
    return "https://wa.me/" + phone + "?text=" + encodeURIComponent(text);
  }
  return Object.freeze({ preparar });
})();
