// La galería solo cambia la fotografía; las compras conservan el formulario MVC.
(() => {
    const gallery = document.querySelector('.detail-gallery');
    if (!gallery) return;
    gallery.addEventListener('click', event => {
        const button = event.target.closest('button[data-photo]');
        if (!button) return;
        gallery.querySelector('#perfume-photo').src = button.dataset.photo;
        gallery.querySelectorAll('button[data-photo]').forEach(item => {
            item.classList.toggle('selected', item === button);
            item.setAttribute('aria-pressed', String(item === button));
        });
    });
})();
