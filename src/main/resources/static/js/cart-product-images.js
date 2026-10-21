(function () {
    'use strict';

    function init() {
        document.querySelectorAll('[data-cart-product-image]').forEach(image => {
            const fallback = image.dataset.fallbackSrc;
            const productName = image.alt;
            function useFallback() {
                if (!fallback || image.dataset.imageFallback === 'true') return;
                image.dataset.imageFallback = 'true';
                image.classList.add('is-image-fallback');
                image.alt = 'Chưa có ảnh cho ' + productName;
                image.title = 'Ảnh sản phẩm chưa sẵn sàng';
                if (image.getAttribute('src') !== fallback) image.src = fallback;
            }
            image.addEventListener('error', useFallback, { once: true });
            if (image.getAttribute('src') === fallback || (image.complete && image.naturalWidth === 0)) {
                useFallback();
            }
        });
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
    else init();
}());
