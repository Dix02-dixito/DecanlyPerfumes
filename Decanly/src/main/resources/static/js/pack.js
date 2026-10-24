(() => {
    const panel = document.getElementById("pack-summary");
    if (!panel) return;

    const lista = document.getElementById("pack-list");
    const plantilla = document.getElementById("pack-item-template");
    const pendientes = new WeakSet();

    const moneda = new Intl.NumberFormat("es-PE", {
        style: "currency",
        currency: "PEN"
    });

    let cola = Promise.resolve();
	let listo = false;
	let operaciones = 0;

    function escribir(id, texto) {
        const elemento = document.getElementById(id);
        if (elemento) elemento.textContent = texto;
    }

    function avisar(mensaje) {
        if (window.decanlyAvisar) {
            window.decanlyAvisar(mensaje);
        } else {
            escribir("pack-status", mensaje);
        }
    }
	
	function actualizarBotonCarrito() {
	    const boton = document.getElementById("pack-to-cart");

	    if (boton) {
	        boton.disabled = !listo || operaciones > 0;
	    }
	}

    const dinero = valor =>
        valor == null ? "—" : moneda.format(Number(valor));

    function crearFila(item) {
        const fila = plantilla.content.firstElementChild.cloneNode(true);
        const imagen = fila.querySelector("[data-pack-imagen]");

        if (item.imagenId == null) {
            imagen.remove();
        } else {
            imagen.src = panel.dataset.imagenesUrl + item.imagenId;
            imagen.alt = item.nombre;
        }

        fila.querySelector("[data-pack-nombre]").textContent = item.nombre;

        fila.querySelector("[data-pack-detalle]").textContent =
            `${item.ml} ml · ${item.cantidad} × ${dinero(item.precio)}`;

        const error = fila.querySelector("[data-pack-error]");
        error.hidden = !item.error;
        error.textContent = item.error || "";

        const quitar = fila.querySelector("[data-quitar-pack]");
        quitar.dataset.quitarPack = item.productoId;
        quitar.setAttribute(
            "aria-label", `Quitar ${item.nombre} del pack`
        );

        return fila;
    }

    function pintar(resumen) {
        if (!resumen || !Array.isArray(resumen.items)) {
            throw new Error("No se pudo leer el resumen del pack.");
        }

        if (resumen.items.length) {
            lista.replaceChildren(...resumen.items.map(crearFila));
        } else {
            const vacio = document.createElement("p");
            vacio.className = "hint";
            vacio.textContent =
                "Añade perfumes de la colección para crear tu pack.";

            lista.replaceChildren(vacio);
        }

        escribir("pack-subtotal", dinero(resumen.subtotal));

        escribir("pack-ahorro", resumen.ahorro == null
            ? "—" : "−" + dinero(resumen.ahorro));

        escribir("pack-total", dinero(resumen.total));
        escribir("pack-mobile-total", dinero(resumen.total));

        escribir(
            "pack-mobile-count",
            `Tu pack · ${resumen.unidades} decants`
        );

        document.getElementById("pack-progress").style.width =
            Math.min(100, resumen.perfumes * 100 / 3) + "%";

        escribir("pack-status", resumen.faltantes > 0
            ? `Faltan ${resumen.faltantes} perfumes diferentes para completar el mínimo de tres.`
            : resumen.listo
                ? "Tu selección cumple el mínimo de tres perfumes diferentes."
                : "Revisa los perfumes que no tienen disponibilidad.");
				listo = resumen.listo === true;
				actualizarBotonCarrito();
    }

    async function solicitar(url, datos) {
        const headers = { Accept: "application/json" };

        const token = document.querySelector(
            'meta[name="csrf-token"]'
        )?.content;

        const cabecera = document.querySelector(
            'meta[name="csrf-header"]'
        )?.content;

        if (token && cabecera) headers[cabecera] = token;

        const respuesta = await fetch(url, {
            method: datos ? "POST" : "GET",
            body: datos,
            headers,
            credentials: "same-origin",
            cache: "no-store",
            redirect: "error"
        });

        const resultado = await respuesta.json().catch(() => null);

        if (!respuesta.ok || !resultado) {
            throw new Error(
                resultado?.mensaje || resultado?.message
                || "No se pudo actualizar el pack. Recarga la página."
            );
        }

        return resultado;
    }

	function ejecutar(tarea, control) {
	    if (control && pendientes.has(control)) return;
	    if (control) pendientes.add(control);

	    const elementos = control instanceof HTMLFormElement
	        ? [...control.querySelectorAll("button")]
	        : control ? [control] : [];

	    const estados = elementos.map(
	        elemento => [elemento, elemento.disabled]
	    );

	    elementos.forEach(elemento => elemento.disabled = true);

	    operaciones++;
	    actualizarBotonCarrito();

	    cola = cola.then(async () => {
	        try {
	            await tarea();

	        } catch (error) {
	            listo = false;

	            avisar(error instanceof TypeError
	                ? "No pudimos confirmar el cambio. Recarga para revisar tu selección."
	                : error.message);

	        } finally {
	            estados.forEach(([elemento, estado]) =>
	                elemento.disabled = estado);

	            if (control) pendientes.delete(control);

	            operaciones--;
	            actualizarBotonCarrito();
	        }
	    });
	}

	document.addEventListener("submit", evento => {
	    const formulario = evento.target;

	    if (!(formulario instanceof HTMLFormElement)
	            || formulario.method !== "post") return;

	    const url = new URL(formulario.action, document.baseURI);

	    const coincidencia =
	        /\/packs\/(agregar|carrito)\/?$/.exec(url.pathname);

	    if (!coincidencia) return;

	    evento.preventDefault();

	    const enviar = coincidencia[1] === "carrito";

	    if (enviar && (!listo || operaciones > 0)) return;

	    const datos = new FormData(formulario);
	    url.pathname = url.pathname.replace(/\/$/, "") + "/ajax";

	    ejecutar(async () => {
	        const resultado = await solicitar(url, datos);

	        if (enviar && !Number.isSafeInteger(resultado.cantidad)) {
	            throw new Error(
	                "No pudimos confirmar el envío. Recarga para revisar tu carrito."
	            );
	        }

	        pintar(resultado.resumen);

	        if (enviar) {
	            escribir("cart-count", resultado.cantidad);
	        }

	        avisar(resultado.mensaje);
	    }, formulario);
	});

    lista.addEventListener("click", evento => {
        const boton = evento.target.closest("button[data-quitar-pack]");
        if (!boton) return;

        const datos = new FormData();
        datos.append("productoId", boton.dataset.quitarPack);

        ejecutar(async () => {
            const resultado = await solicitar(
                panel.dataset.quitarUrl, datos
            );

            pintar(resultado.resumen);
            avisar(resultado.mensaje);
        }, boton);
    });

    ejecutar(async () =>
        pintar(await solicitar(panel.dataset.resumenUrl))
    );
})();