(() => {
    const form = document.getElementById('pedido-manual');
    if (!form) return;
    const container = document.getElementById('pedido-lineas');
    const discount = document.getElementById('descuento');
    const rows = () => [...container.querySelectorAll('.order-line')];
    const money = cents => `S/ ${(cents / 100).toFixed(2)}`;
    const cents = value => {
        if (!/^\d+(\.\d{1,2})?$/.test(value)) return 0;
        const [whole, fraction = ''] = value.split('.');
        return Number(whole) * 100 + Number(fraction.padEnd(2, '0'));
    };
    function totals() {
        const subtotal = rows().reduce((sum, row) => sum + cents(row.querySelector('.order-price').value)
            * (Number(row.querySelector('.order-qty').value) || 0), 0);
        const reduction = cents(discount.value);
        document.getElementById('preview-subtotal').textContent = money(subtotal);
        document.getElementById('preview-descuento').textContent = money(reduction);
        document.getElementById('preview-total').textContent = money(subtotal - reduction);
        discount.setCustomValidity(reduction > subtotal ? 'El descuento no puede superar el subtotal.' : '');
    }
    function selection(row, replacePrice) {
        const product = catalogoPedidos.find(p => String(p.id) === row.querySelector('.order-product').value);
        const variant = product?.presentaciones.find(p => String(p.ml) === row.querySelector('.order-size').value);
        row.querySelector('.order-stock').textContent = variant ? `Stock actual: ${variant.stock} unidades` : '';
        if (replacePrice) row.querySelector('.order-price').value = variant ? Number(variant.precio).toFixed(2) : '';
        totals();
    }
    function reindex() {
        rows().forEach((row, i) => {
            row.querySelector('legend').textContent = `Perfume ${i + 1}`;
            row.querySelectorAll('[name^="lineas["]').forEach(input => {
                input.name = input.name.replace(/lineas\[\d+\]/, `lineas[${i}]`);
                input.id = input.name.replace(/[\[\].]/g, '-');
            });
            const remove = row.querySelector('.order-remove');
            remove.value = `quitar:${i}`;
            remove.disabled = rows().length === 1;
        });
        document.getElementById('pedido-agregar').disabled = rows().length >= 50;
        totals();
    }
    document.getElementById('pedido-agregar').addEventListener('click', event => {
        event.preventDefault();
        if (rows().length >= 50) return;
        const row = rows()[0].cloneNode(true);
        row.querySelector('.order-product').value = '';
        row.querySelector('.order-size').value = '3';
        row.querySelector('.order-qty').value = '1';
        row.querySelector('.order-price').value = '';
        row.querySelector('.order-stock').textContent = '';
        container.append(row);
        reindex();
        row.querySelector('.order-product').focus();
    });
    container.addEventListener('click', event => {
        const remove = event.target.closest('.order-remove');
        if (!remove) return;
        event.preventDefault();
        if (rows().length > 1) {
            remove.closest('.order-line').remove();
            reindex();
            document.getElementById('pedido-agregar').focus();
        }
    });
    container.addEventListener('change', event => {
        if (event.target.matches('.order-product, .order-size')) selection(event.target.closest('.order-line'), true);
    });
    form.addEventListener('input', totals);
    rows().forEach(row => selection(row, false));
    reindex();
})();
