(function () {
    const form = document.getElementById('checkoutForm');
    const button = document.getElementById('btnPlaceOrder');
    const status = document.getElementById('quoteStatus');
    let timer, request, sequence = 0, ready = false;
    const money = value => new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
    window.updateCheckoutTotal = function () {
        clearTimeout(timer);
        const current = ++sequence;
        ready = false;
        status.textContent = 'Đang cập nhật số tiền…';
        request?.abort();
        timer = setTimeout(async () => {
            request = new AbortController();
            try {
                const response = await fetch(form.dataset.quoteUrl, {
                    method: 'POST', body: new FormData(form), signal: request.signal,
                    headers: { 'X-Requested-With': 'XMLHttpRequest' }
                });
                if (response.redirected) throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
                const result = await response.json();
                if (!response.ok) throw new Error(result.message || 'Không tính được số tiền. Vui lòng thử lại.');
                if (current !== sequence) return;
                document.getElementById('displaySubtotal').textContent = money(result.subtotal);
                document.getElementById('displayShippingFee').textContent = money(result.shippingFee);
                document.getElementById('displayDiscount').textContent = '-' + money(result.discount);
                document.getElementById('displayGrandTotal').textContent = money(result.total);
                const deliveryName = document.getElementById('assignedDeliveryName');
                if (deliveryName && form.querySelector('#methodDelivery')?.checked) {
                    deliveryName.textContent = result.deliveryUnitName || 'Chưa cấu hình đơn vị vận chuyển cho vùng này.';
                }
                status.textContent = '';
                ready = true;
                button.disabled = button.dataset.pickupStockBlocked === 'true';
            } catch (error) {
                if (error.name === 'AbortError' || current !== sequence) return;
                status.textContent = error.message;
                document.getElementById('displayGrandTotal').textContent = 'Chưa xác định';
                const deliveryName = document.getElementById('assignedDeliveryName');
                if (deliveryName && form.querySelector('#methodDelivery')?.checked) {
                    deliveryName.textContent = 'Chưa chọn được đơn vị vận chuyển phù hợp.';
                }
                button.disabled = button.dataset.pickupStockBlocked === 'true';
            }
        }, 250);
    };
    form.addEventListener('input', event => {
        if (['voucherCode', 'recipientAddress'].includes(event.target.name)) updateCheckoutTotal();
    });
    form.addEventListener('change', event => {
        if (['shippingMethod', 'pickupStoreId', 'deliveryZone', 'voucherCode'].includes(event.target.name)) updateCheckoutTotal();
    });
    form.addEventListener('submit', event => {
        if (event.defaultPrevented) return;
        if (!ready) {
            event.preventDefault();
            const message = status.textContent && status.textContent !== 'Đang cập nhật số tiền…'
                ? status.textContent : 'Vui lòng chờ hệ thống xác nhận phí giao hàng rồi thử lại.';
            if (typeof window.showGlobalAlertModal === 'function') {
                window.showGlobalAlertModal({ title: 'Chưa thể đặt hàng', message, isError: true });
            } else {
                const error = document.getElementById('checkoutValidationError');
                if (error) { error.textContent = message; error.classList.remove('d-none'); }
            }
            updateCheckoutTotal();
            return;
        }
        button.disabled = true;
        button.textContent = 'Đang đặt hàng…';
    });
    document.addEventListener('DOMContentLoaded', () => {
        toggleShippingMethod();
    });
})();
