(() => {
    let cola = Promise.resolve();
    let temporizador;
    const pendientes = new WeakSet();

    function avisar(mensaje) {
        const aviso = document.getElementById("toast");
        if (!aviso) return;

        clearTimeout(temporizador);
        aviso.textContent = mensaje;
        aviso.classList.add("show");

        temporizador = setTimeout(
            () => aviso.classList.remove("show"),
            3500
        );
    }


    window.decanlyAvisar = avisar;
    document.addEventListener("submit", (evento) => {
        const formulario = evento.target;

        if (!(formulario instanceof HTMLFormElement)
            || formulario.method !== "post") return;

        const boton = evento.submitter;

        const url = new URL(
            boton?.getAttribute("formaction") || formulario.action,
            document.baseURI
        );

        if (!/\/carrito\/(agregar|promocion)\/?$/.test(url.pathname)) return;

        const datos = boton
            ? new FormData(formulario, boton)
            : new FormData(formulario);

        if (datos.get("destino") === "comprar") return;

        evento.preventDefault();

        if (pendientes.has(formulario)) return;

        pendientes.add(formulario);
        formulario.setAttribute("aria-busy", "true");

        const botones = [...formulario.querySelectorAll(
            "button, input[type='submit']"
        )].map(elemento => [elemento, elemento.disabled]);

        botones.forEach(([elemento]) => elemento.disabled = true);

        url.pathname = url.pathname.replace(/\/$/, "") + "/ajax";

        const encabezados = { Accept: "application/json" };

        const token = document.querySelector(
            'meta[name="csrf-token"]'
        )?.content;

        const cabecera = document.querySelector(
            'meta[name="csrf-header"]'
        )?.content;

        if (token && cabecera) {
            encabezados[cabecera] = token;
        }

        cola = cola.then(async () => {
            try {
                const respuesta = await fetch(url, {
                    method: "POST",
                    body: datos,
                    headers: encabezados,
                    credentials: "same-origin",
                    redirect: "error"
                });

                const resultado = await respuesta.json().catch(() => null);

                if (!respuesta.ok) {
                    throw new Error(
                        resultado?.mensaje || resultado?.message
                        || "No se pudo añadir. Recarga la página e inténtalo nuevamente."
                    );
                }

                if (!Number.isSafeInteger(resultado?.cantidad)) {
                    throw new Error(
                        "No pudimos confirmar la operación. Revisa tu carrito."
                    );
                }

                const contador = document.getElementById("cart-count");

                if (contador) {
                    contador.textContent = resultado.cantidad;
                }

                avisar(resultado.mensaje);

            } catch (error) {
                avisar(error instanceof TypeError
                    ? "No pudimos confirmar si se añadió. Revisa el carrito antes de repetir."
                    : error.message);

            } finally {
                botones.forEach(([elemento, deshabilitado]) =>
                    elemento.disabled = deshabilitado);

                formulario.removeAttribute("aria-busy");
                pendientes.delete(formulario);
            }
        });
    });
})();