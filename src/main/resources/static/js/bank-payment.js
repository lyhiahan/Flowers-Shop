(function () {
    const label = document.getElementById('bankPaymentStatus');
    const simulation = document.getElementById('bankSandboxForm');
    let countdown;
    if (simulation) {
        const updateCountdown = () => {
            const seconds = Math.max(0, Math.ceil((Number(simulation.dataset.expiresAt) - Date.now()) / 1000));
            document.getElementById('bankCountdown').textContent = seconds ? `Thời gian còn lại: ${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}` : 'Phiên đã hết hạn. Tải lại trang để thử lại.';
            if (!seconds) { document.getElementById('simulateBankPayment').disabled = true; clearInterval(countdown); }
        };
        countdown = setInterval(updateCountdown, 1000); updateCountdown();
    }
    simulation?.addEventListener('submit', function (event) {
        if (simulation.dataset.confirmed !== 'true') {
            event.preventDefault();
            const message = 'Xác nhận mô phỏng thanh toán đơn này? Không có tiền thật được chuyển.';
            const send = () => { simulation.dataset.confirmed = 'true'; simulation.requestSubmit(); };
            if (typeof window.showGlobalConfirmModal === 'function') {
                window.showGlobalConfirmModal({ message, onConfirm: send });
            } else if (window.confirm(message)) send();
            return;
        }
        const button = document.getElementById('simulateBankPayment');
        button.disabled = true; button.textContent = 'Đang xác minh thanh toán…';
    });
    let timer, request, checks = 0;
    function render(payment, order) {
        if (['REFUNDED', 'CANCELLED', 'REFUND_REQUESTED'].includes(order) || payment === 'REFUNDED') {
            document.getElementById('bankTransferInstructions')?.setAttribute('hidden', '');
            label.textContent = 'Đơn đã hủy hoặc hoàn tiền. Không chuyển khoản cho đơn này.'; return true;
        }
        if (payment === 'PAID') {
            document.getElementById('bankTransferInstructions')?.setAttribute('hidden', '');
            label.textContent = 'Đã xác nhận thanh toán'; label.classList.add('text-success'); return true;
        }
        label.textContent = simulation ? 'Chờ thanh toán thử nghiệm…' : 'Chờ cửa hàng xác nhận thanh toán…'; return false;
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
    window.addEventListener('pagehide', () => { clearTimeout(timer); clearInterval(countdown); request?.abort(); });
})();
