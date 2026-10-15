(function () {
    'use strict';
    const card = document.querySelector('[data-stock-kpi], [data-stock-catalog]');
    if (!card) return;
    const value = card.querySelector('[data-stock-value]');
    const status = card.querySelector('[data-stock-status]');
    let endpoint = card.dataset.stockEndpoint;
    const catalogMode = card.dataset.stockCatalog === 'true';
    if (!endpoint || !catalogMode && (!value || !status)) return;
    const productCards = catalogMode ? Array.from(document.querySelectorAll('[data-catalog-product-id]')) : [];
    const productIds = Array.from(new Set(productCards.map(item => item.dataset.catalogProductId)));
    if (productIds.length) endpoint += (endpoint.includes('?') ? '&' : '?') + 'productIds=' + encodeURIComponent(productIds.join(','));
    let pending = false;
    let stopped = false;
    let controller;

    async function refresh() {
        if (pending || stopped || document.hidden) return;
        pending = true;
        controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 8000);
        try {
            const response = await fetch(endpoint, { credentials: 'same-origin', cache: 'no-store', signal: controller.signal });
            if (!response.ok) throw new Error('Stock unavailable');
            const data = await response.json();
            if (!Number.isSafeInteger(data.currentStockQuantity) || data.currentStockQuantity < 0)
                throw new Error('Invalid stock response');
            if (catalogMode) {
                for (const id of productIds) {
                    const stock = data.products && data.products[id];
                    if (!stock || !Number.isSafeInteger(stock.quantity) || stock.quantity < 0
                            || stock.quantity > 0 && (!Number.isSafeInteger(stock.purchaseStoreId) || stock.purchaseStoreId <= 0))
                        throw new Error('Invalid catalog stock response');
                }
            }
            if (stopped) return;
            for (const item of productCards) updateProduct(item, data.products[item.dataset.catalogProductId]);
            if (value) value.textContent = data.currentStockQuantity.toLocaleString('vi-VN');
            if (status) { status.hidden = true; status.textContent = ''; }
        } catch (_) {
            if (!stopped && status) {
                // Keep the last confirmed quantity; a failed request does not mean zero stock.
                status.textContent = 'Chưa cập nhật được tồn kho; đang hiển thị số liệu lần tải gần nhất.';
                status.hidden = false;
            }
        } finally {
            clearTimeout(timeout);
            pending = false;
        }
    }

    function updateProduct(item, stock) {
        const available = stock.quantity > 0;
        const label = item.querySelector('.catalog-stock');
        if (label) {
            label.classList.toggle('is-out-of-stock', !available);
            const text = label.querySelector('span');
            const icon = label.querySelector('i');
            if (text) text.textContent = available ? 'Còn ' + stock.quantity.toLocaleString('vi-VN') + ' sản phẩm' : 'Hết hàng';
            if (icon) icon.className = available ? 'bi bi-box-seam' : 'bi bi-x-octagon-fill';
        }
        const form = item.querySelector('.ajax-add-to-cart');
        if (!form) return;
        form.dataset.stockAvailable = available ? 'true' : 'false';
        const store = form.querySelector('[name="storeId"]');
        if (store) store.value = available ? String(stock.purchaseStoreId) : '';
        const button = form.querySelector('button[type="submit"]');
        if (!button) return;
        button.disabled = !available || Boolean(form.dataset.pending);
        if (!form.dataset.pending) {
            const text = button.querySelector('span');
            const icon = button.querySelector('i');
            if (text) text.textContent = available ? 'Thêm vào giỏ' : 'Hết hàng';
            if (icon) icon.className = available ? 'bi bi-cart-plus' : 'bi bi-x-circle';
        }
    }

    refresh();
    let timer = setInterval(refresh, 5000);
    document.addEventListener('visibilitychange', refresh);
    window.addEventListener('focus', refresh);
    window.addEventListener('pagehide', () => {
        stopped = true;
        clearInterval(timer);
        if (controller) controller.abort();
    });
    window.addEventListener('pageshow', () => {
        if (!stopped) return;
        stopped = false;
        timer = setInterval(refresh, 5000);
        refresh();
    });
}());
