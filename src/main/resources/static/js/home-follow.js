let cardFollowToastTimer = null;
function showCardToast(msg) {
    const toast = document.getElementById('cardFollowToast');
    const toastMsg = document.getElementById('cardFollowToastMsg');
    if (!toast || !toastMsg) return;
    toastMsg.textContent = msg;
    toast.style.display = 'inline-flex';

    if (cardFollowToastTimer) clearTimeout(cardFollowToastTimer);
    cardFollowToastTimer = setTimeout(() => {
        toast.style.display = 'none';
    }, 2500);
}

function toggleCardFollowAjax(event, formEl) {
    if (event) event.preventDefault();
    const btn = formEl.querySelector('button');
    if (formEl.dataset.pending) return;
    formEl.dataset.pending = 'true';
    if (btn) btn.disabled = true;
    const icon = btn ? btn.querySelector('i') : null;
    const url = formEl.getAttribute('action');
    const csrfInput = formEl.querySelector('input[name="_csrf"]');
    const csrfToken = csrfInput ? csrfInput.value : '';

    const headers = {
        'X-Requested-With': 'XMLHttpRequest'
    };
    if (csrfToken) {
        headers['X-CSRF-TOKEN'] = csrfToken;
    }

    fetch(url, {
        method: 'POST',
        headers: headers,
        body: new FormData(formEl)
    })
        .then(res => {
            if (res.status === 401 || res.status === 403) {
                window.location.href = '/login';
                return null;
            }
            if (!res.ok || res.redirected) throw new Error('Không cập nhật được theo dõi. Vui lòng kiểm tra đăng nhập rồi tải lại trang.');
            return res.json();
        })
        .then(data => {
            if (!data) return;
            showCardToast(data.message || (data.liked ? 'Đã theo dõi sản phẩm thành công!' : 'Đã hủy theo dõi sản phẩm!'));
            if (btn && icon) {
                if (data.liked) {
                    btn.classList.remove('text-muted');
                    btn.classList.add('text-danger');
                    btn.setAttribute('title', 'Đang theo dõi (Bấm để hủy)');
                    icon.className = 'bi bi-heart-fill text-danger';
                } else {
                    btn.classList.remove('text-danger');
                    btn.classList.add('text-muted');
                    btn.setAttribute('title', 'Theo dõi nhanh');
                    icon.className = 'bi bi-heart';
                }
            }
        })
        .catch(err => {
            console.error(err);
            showCardToast('Chưa xác nhận được kết quả. Vui lòng tải lại trang trước khi thử lại.');
        }).finally(() => { delete formEl.dataset.pending; if (btn) btn.disabled = false; });
}
