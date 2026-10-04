// Two-step withdrawal in a single Bootstrap modal; no request before confirmation.
(() => {
    const form = document.getElementById('formWithdraw');
    if (!form) return;
    const get = id => document.getElementById(id);
    const modal = get('withdrawModal');
    const amount = get('inputWithdrawAmount');
    const password = get('inputWithdrawPassword');
    const account = get('inputAccountNumber');
    const holder = get('inputAccountHolder');
    const bank = get('inputBankName');
    let reviewing = false;
    let pending = false;
    function setReview(value) {
        reviewing = value;
        get('withdrawFields').hidden = value;
        get('withdrawReview').hidden = !value;
        get('btnOpenConfirmWithdraw').hidden = value;
        get('btnConfirm').hidden = !value;
        get('btnCancel').hidden = !value;
        get('withdrawStep').textContent = value ? 'Bước 2/2 · Xác nhận yêu cầu' : 'Bước 1/2 · Thông tin rút tiền';
    }
    function validate() {
        account.value = account.value.trim();
        holder.value = holder.value.trim();
        const exceedsBalance = Number(amount.value) > Number(amount.dataset.balance);
        amount.setCustomValidity(exceedsBalance ? 'Số tiền rút vượt quá số dư khả dụng.' : '');
        return window.StoreFormValidation.validate(form);
    }
    amount.addEventListener('input', () => {
        amount.setCustomValidity('');
    });
    form.addEventListener('submit', event => {
        if (pending) { event.preventDefault(); return; }
        // Show fields before validation so an invalid control can receive focus.
        const wasReviewing = reviewing;
        setReview(false);
        if (!validate()) { event.preventDefault(); return; }
        if (!wasReviewing) {
            event.preventDefault();
            get('displayConfirmAmount').textContent = Number(amount.value).toLocaleString('vi-VN', { maximumFractionDigits: 2 }) + ' ₫';
            get('displayConfirmBank').textContent = bank.options[bank.selectedIndex].text;
            get('displayConfirmAccount').textContent = account.value;
            get('displayConfirmHolder').textContent = holder.value;
            setReview(true);
            get('withdrawReview').focus();
            return;
        }
        setReview(true);
        pending = true;
        get('btnConfirm').disabled = true;
        get('btnConfirm').textContent = 'Đang gửi…';
        get('btnCancel').disabled = true;
        get('btnCloseWithdraw').disabled = true;
        modal.querySelector('.btn-close').disabled = true;
    });
    get('btnCancel').addEventListener('click', () => {
        setReview(false);
        amount.focus();
    });
    modal.addEventListener('hide.bs.modal', event => { if (pending) event.preventDefault(); });
    modal.addEventListener('hidden.bs.modal', () => {
        setReview(false);
        password.value = '';
        amount.setCustomValidity('');
        window.StoreFormValidation.reset(form);
    });
    modal.addEventListener('shown.bs.modal', () => amount.focus());
})();
