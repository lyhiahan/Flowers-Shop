(function () {
    const label = document.getElementById('bankPaymentStatus');
    let timer, request, checks = 0;
    function render(payment, order) {
        if (['REFUNDED', 'CANCELLED'].includes(order) || payment === 'REFUNDED') {
            document.getElementById('bankTransferInstructions')?.setAttribute('hidden', '');
            label.textContent = 'Đơn đã hủy hoặc hoàn tiền. Không chuyển khoản cho đơn này.'; return true;
        }
        if (payment === 'PAID') {
            document.getElementById('bankTransferInstructions')?.setAttribute('hidden', '');
            label.textContent = 'Đã xác nhận thanh toán'; label.classList.add('text-success'); return true;
        }
        label.textContent = 'Chờ cửa hàng xác nhận thanh toán…'; return false;
    }
    async function check() {
        request = new AbortController();
        try {
            const response = await fetch(label.dataset.statusUrl, { signal: request.signal });
            if (response.redirected) { label.textContent = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'; return; }
            if (!response.ok) throw new Error('Không tải được trạng thái');
            const result = await response.json();
            if (render(result.paymentStatus, result.orderStatus)) return;
        } catch (error) {
            if (error.name === 'AbortError') return;
            label.textContent = 'Chưa cập nhật được trạng thái. Hệ thống sẽ thử lại.';
        }
        if (++checks < 180) timer = setTimeout(check, 10000);
    }
    if (!render(label.dataset.paymentStatus, label.dataset.orderStatus)) timer = setTimeout(check, 10000);
    window.addEventListener('pagehide', () => { clearTimeout(timer); request?.abort(); });
})();
