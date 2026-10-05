/* Profile interactions using the project's Bootstrap bundle and vanilla JS. */
(function () {
    window.handleRoleChange = function (role) {
        const sections = { MANAGER: 'sectionManager', MANAGER: 'sectionVendor' };
        Object.entries(sections).forEach(([value, id]) => {
            const section = document.getElementById(id);
            if (!section) return;
            section.hidden = value !== role;
            section.style.display = value === role ? 'block' : 'none';
            section.querySelectorAll('input, select, textarea').forEach(input => { input.disabled = value !== role; });
        });
        document.getElementById('btnSubmitRoleRequest').disabled = !sections[role];
    };
    const select = document.getElementById('selectTargetRole');
    if (select) handleRoleChange(select.value);
    const tab = new URLSearchParams(location.search).get('tab');
    const tabId = tab === 'addresses' ? 'address' : tab;
    const tabButton = document.getElementById(tabId + '-tab');
    if (tabButton) bootstrap.Tab.getOrCreateInstance(tabButton).show();
    document.querySelectorAll('input[data-preview]').forEach(input => {
        input.addEventListener('change', () => {
            const file = input.files[0];
            if (!file) return;
            if (!file.type.startsWith('image/')) { input.value = ''; return; }
            let preview = document.getElementById(input.dataset.preview);
            if (!preview) {
                preview = document.createElement('img');
                preview.id = input.dataset.preview;
                preview.className = 'floral-cover w-100 mt-3';
                preview.alt = 'Ảnh bìa xem trước';
                input.parentElement.appendChild(preview);
            }
            const reader = new FileReader();
            reader.onload = () => { preview.src = reader.result; };
            reader.readAsDataURL(file);
        });
    });
    const otpButton = document.getElementById('sendPasswordOtp');
    otpButton?.addEventListener('click', async () => {
        const status = document.getElementById('otpStatus');
        const form = document.getElementById('passwordForm');
        const data = new FormData();
        data.set('email', otpButton.dataset.email || '');
        const csrf = form.querySelector('input[name="_csrf"]');
        if (csrf) data.set('_csrf', csrf.value);
        otpButton.disabled = true;
        status.textContent = 'Đang gửi OTP…';
        try {
            const response = await fetch(otpButton.dataset.url, { method: 'POST', body: data, headers: { 'X-Requested-With': 'XMLHttpRequest' } });
            if (!response.ok || response.redirected) throw new Error('Không gửi được OTP. Vui lòng kiểm tra phiên đăng nhập.');
            const result = await response.json();
            status.textContent = result.message || 'Đã gửi OTP.';
        } catch (error) { status.textContent = error.message; }
        finally { otpButton.disabled = false; }
    });
})();
