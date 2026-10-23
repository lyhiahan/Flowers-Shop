(function () {
    'use strict';

    /** Cập nhật số lượng trên biểu tượng giỏ hàng ở header */
    function updateCartBadge(count) {
        const cartLink = document.getElementById('headerCartLink');
        if (!cartLink || !Number.isInteger(count) || count < 0) return;
        const badge = cartLink.querySelector('.cart-badge-pill');
        cartLink.setAttribute('aria-label', 'Giỏ hàng, ' + count + ' sản phẩm');
        if (count > 0) {
            if (badge) {
                badge.textContent = count;
                badge.style.display = '';
            } else {
                const span = document.createElement('span');
                span.id = 'headerCartBadge';
                span.className = 'cart-badge-pill';
                span.setAttribute('aria-live', 'polite');
                span.textContent = count;
                cartLink.appendChild(span);
            }
        } else {
            if (badge) badge.style.display = 'none';
        }
    }

    /** Lấy CSRF token từ meta tag */
    function getCsrfMeta() {
        const token = document.querySelector('meta[name="_csrf"]');
        const header = document.querySelector('meta[name="_csrf_header"]');
        return {
            token: token ? token.content : '',
            header: header ? header.content : 'X-CSRF-TOKEN'
        };
    }

    document.addEventListener('submit', function (e) {
        const form = e.target.closest('.ajax-add-to-cart');
        if (!form) return;
        e.preventDefault();

        const btn = form.querySelector('button[type="submit"]');
        const errEl = form.querySelector('.ajax-cart-error');
        if (form.dataset.pending) return;  // chặn bấm liên tiếp
        form.dataset.pending = 'true';
        if (btn) btn.disabled = true;
        if (errEl) errEl.style.display = 'none';

        const csrf = getCsrfMeta();
        const headers = {
            'X-Requested-With': 'XMLHttpRequest'
        };
        if (csrf.token) headers[csrf.header] = csrf.token;

        fetch(form.action || '/cart/add', {
            method: 'POST',
            headers: headers,
            credentials: 'same-origin',
            body: new FormData(form)
        })
        .then(function (res) {
            if (res.status === 401) { window.location.href = '/login'; return null; }
            if (res.status === 403) {
                return res.json().then(function (d) {
                    throw new Error(d.message || 'Bạn không có quyền thực hiện thao tác này.');
                });
            }
            return res.json();
        })
        .then(function (data) {
            if (!data) return;
            if (data.success) {
                updateCartBadge(data.count);
                /* Hiệu ứng nhỏ: nút nhấp nháy xanh rồi trở lại */
                if (btn) {
                    const orig = btn.innerHTML;
                    btn.innerHTML = '<i class="bi bi-check-lg"></i>';
                    btn.style.background = '#2ecc71';
                    setTimeout(function () {
                        btn.innerHTML = orig;
                        if (form.dataset.stockAvailable === 'false') btn.innerHTML = '<i class="bi bi-x-circle"></i><span>' + (form.dataset.salePaused === 'true' ? 'Tạm ngưng bán' : 'Hết hàng') + '</span>';
                        btn.style.background = '';
                    }, 800);
                }
            } else {
                if (data.paused === true) {
                    form.dataset.stockAvailable = 'false';
                    form.dataset.salePaused = 'true';
                    if (btn) btn.innerHTML = '<i class="bi bi-pause-circle"></i><span>Tạm ngưng bán</span>';
                    const card = form.closest('[data-catalog-product-id]');
                    const label = card ? card.querySelector('.catalog-stock') : document.getElementById('productStock');
                    if (label) {
                        label.classList.add('sale-paused-tag');
                        label.textContent = 'Sản phẩm tạm ngưng bán';
                    }
                    return;
                }
                if (errEl) { errEl.textContent = data.error || 'Không thể thêm vào giỏ.'; errEl.style.display = 'block'; }
            }
        })
        .catch(function (err) {
            if (errEl) { errEl.textContent = err.message || 'Lỗi kết nối, vui lòng thử lại.'; errEl.style.display = 'block'; }
        })
        .finally(function () {
            delete form.dataset.pending;
            if (btn) btn.disabled = form.dataset.stockAvailable === 'false';
        });
    });
})();
