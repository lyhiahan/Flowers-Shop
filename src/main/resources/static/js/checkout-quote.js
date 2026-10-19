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
        button.disabled = true;
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
                status.textContent = '';
                ready = true;
                button.disabled = false;
            } catch (error) {
                if (error.name === 'AbortError' || current !== sequence) return;
                status.textContent = error.message;
                document.getElementById('displayGrandTotal').textContent = 'Chưa xác định';
            }
        }, 250);
    };
    form.addEventListener('input', event => {
        if (['voucherCode', 'recipientAddress'].includes(event.target.name)) updateCheckoutTotal();
    });
    form.addEventListener('change', event => {
        if (['shippingMethod', 'pickupStoreId', 'deliveryUnitId', 'deliveryZone', 'voucherCode'].includes(event.target.name)) updateCheckoutTotal();
    });
    form.addEventListener('submit', event => {
        if (!ready) { event.preventDefault(); updateCheckoutTotal(); return; }
        button.disabled = true;
        button.textContent = 'Đang đặt hàng…';
    });
    document.addEventListener('DOMContentLoaded', () => {
        const saved = document.querySelector('#savedAddressSection select');
        if (saved) fillAddress(saved);
        toggleShippingMethod();
    });
})();
