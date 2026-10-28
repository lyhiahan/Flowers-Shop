(() => {
    'use strict';
    const root = document.querySelector('.payment-main');
    if (!root) return;
    let state = root.dataset.state;
    let timer;
    const qr = document.getElementById('payment-qr');
    const phoneLink = document.getElementById('phone-payment-link');
    const phoneMessage = document.getElementById('phone-connection-message');
    let phoneUrl = phoneLink?.getAttribute('href') || '';

    function renderConnection(connection) {
        if (!qr || !phoneLink || !phoneMessage) return;
        const ready = connection.state === 'READY' && connection.url;
        qr.hidden = !ready;
        phoneLink.hidden = !ready;
        phoneMessage.hidden = !!ready;
        phoneMessage.textContent = connection.message || 'Đang bật kết nối điện thoại…';
        if (ready) {
            phoneLink.href = connection.url;
            if (phoneUrl !== connection.url || !qr.getAttribute('src')) qr.src = root.dataset.qrUrl;
            phoneUrl = connection.url;
        } else {
            phoneLink.removeAttribute('href');
            qr.removeAttribute('src');
            phoneUrl = '';
        }
    }

    async function pollConnection() {
        if (state !== 'PENDING' || !root.dataset.connectionUrl) return;
        try {
            const response = await fetch(root.dataset.connectionUrl, {cache:'no-store', headers:{Accept:'application/json'}});
            if (!response.ok || response.redirected) throw new Error('Connection unavailable');
            renderConnection(await response.json());
        } catch (_) {
            renderConnection({state:'STARTING', message:'Đang kiểm tra lại kết nối điện thoại…'});
        }
    }

    qr?.addEventListener('error', () => renderConnection({state:'STARTING', message:'Đang cập nhật mã QR…'}));
    const labels = { PENDING: 'Chờ xác nhận', SUCCESS: 'Đã thanh toán', EXPIRED: 'Đã hết hạn', FAILED: 'Không thể thanh toán' };
    function render(next) {
        state = next;
        if (state === 'SUCCESS') {
            clearTimeout(timer);
            window.location.replace(root.dataset.successUrl);
            return;
        }
        document.getElementById('payment-status').textContent = labels[state] || labels.FAILED;
        document.getElementById('pending-panel').hidden = state !== 'PENDING';
        document.getElementById('result-panel').hidden = state === 'PENDING';
        document.getElementById('expiry-row').hidden = state !== 'PENDING';
        if (state === 'PENDING') return;
        clearTimeout(timer);
        const expired = state === 'EXPIRED';
        document.getElementById('result-icon').className = 'bi bi-exclamation-circle result-icon';
        document.getElementById('result-title').textContent = expired ? 'Phiên thanh toán đã hết hạn' : 'Giao dịch không còn hợp lệ';
        document.getElementById('result-message').textContent = 'Vui lòng kiểm tra lại đơn hàng trên website.';
    }
    async function poll() {
        if (state !== 'PENDING' || !root.dataset.statusUrl) return;
        try {
            const response = await fetch(root.dataset.statusUrl, { cache: 'no-store', headers: { Accept: 'application/json' } });
            if (response.redirected || response.status === 401 || response.status === 403) {
                render('FAILED');
                document.getElementById('connection-status').textContent = 'Phiên đăng nhập đã kết thúc. Vui lòng đăng nhập lại.';
                return;
            }
            if (response.status === 404) { render('EXPIRED'); return; }
            if (response.status === 409) { render('FAILED'); return; }
            if (!response.ok) throw new Error('Connection failed');
            const payment = await response.json();
            document.getElementById('connection-status').textContent = '';
            render(payment.status);
        } catch (_) {
            document.getElementById('connection-status').textContent = 'Đang kết nối lại để kiểm tra thanh toán…';
        }
        await pollConnection();
        if (state === 'PENDING') timer = setTimeout(poll, 2000);
    }
    function countdown() {
        if (state !== 'PENDING') return;
        const remaining = Math.max(0, Math.ceil((Date.parse(root.dataset.expiry) - Date.now()) / 1000));
        document.getElementById('countdown').textContent = `${String(Math.floor(remaining / 60)).padStart(2, '0')}:${String(remaining % 60).padStart(2, '0')}`;
        // The server decides desktop status; a phone can immediately disable an expired form.
        if (!remaining && !root.dataset.statusUrl) render('EXPIRED');
    }
    document.querySelectorAll('form[action*="/confirm"]').forEach(formEl => {
        formEl.addEventListener('submit', () => {
            const button = formEl.querySelector('button');
            if (button) {
                button.disabled = true;
                button.textContent = 'Đang xác nhận…';
            }
        });
    });
    render(state);
    countdown();
    const countdownTimer = setInterval(() => {
        countdown();
        if (state !== 'PENDING') clearInterval(countdownTimer);
    }, 1000);
    poll();
})();
