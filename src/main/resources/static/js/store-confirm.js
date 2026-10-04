// Shared inline validation for store forms; keep native constraints, localize feedback.
window.StoreFormValidation = (() => {
    let sequence = 0;
    function fields(form) {
        return Array.from(form.elements).filter(field => field.matches('input, select, textarea') && field.willValidate);
    }
    function message(field) {
        const validity = field.validity;
        if (validity.customError) return field.validationMessage;
        if (validity.valueMissing || (field.type !== 'password' && field.required && !field.value.trim()))
            return field.dataset.errorRequired || 'Vui lòng nhập hoặc chọn thông tin này.';
        if (validity.badInput || validity.typeMismatch) return 'Vui lòng nhập đúng định dạng.';
        if (validity.rangeUnderflow) return field.dataset.errorMin || `Giá trị tối thiểu là ${field.min}.`;
        if (validity.rangeOverflow) return field.dataset.errorMax || `Giá trị tối đa là ${field.max}.`;
        if (validity.stepMismatch) return field.dataset.errorStep || 'Giá trị không đúng bước cho phép.';
        if (validity.patternMismatch) return field.dataset.errorPattern || 'Thông tin không đúng định dạng yêu cầu.';
        if (validity.tooLong) return `Vui lòng nhập tối đa ${field.maxLength} ký tự.`;
        if (validity.tooShort) return `Vui lòng nhập ít nhất ${field.minLength} ký tự.`;
        return validity.valid ? '' : 'Vui lòng kiểm tra lại thông tin này.';
    }
    function render(field) {
        let feedback = field._storeFeedback;
        if (!feedback) {
            feedback = document.createElement('div');
            feedback.id = `store-field-error-${++sequence}`;
            feedback.className = 'invalid-feedback';
            (field.closest('label') || field).insertAdjacentElement('afterend', feedback);
            field.setAttribute('aria-describedby', [field.getAttribute('aria-describedby'), feedback.id].filter(Boolean).join(' '));
            field._storeFeedback = feedback;
        }
        const error = message(field);
        feedback.textContent = error;
        feedback.style.display = error ? 'block' : 'none';
        field.classList.toggle('is-invalid', Boolean(error));
        field.setAttribute('aria-invalid', String(Boolean(error)));
        return !error;
    }
    function validate(form) {
        const controls = fields(form);
        const valid = controls.map(render).every(Boolean);
        if (!valid) controls.find(field => field.classList.contains('is-invalid')).focus();
        return valid;
    }
    function reset(form) {
        fields(form).forEach(field => {
            field.classList.remove('is-invalid');
            field.removeAttribute('aria-invalid');
            if (field._storeFeedback) field._storeFeedback.textContent = '';
        });
    }
    document.querySelectorAll('form[data-confirm], form[data-inline-validation]').forEach(form => {
        form.noValidate = true;
        form.addEventListener('input', event => {
            if (event.target.classList.contains('is-invalid')) render(event.target);
        });
        form.addEventListener('change', event => {
            if (event.target.willValidate) render(event.target);
        });
    });
    return { validate, reset };
})();

// Explicit confirmations keep validation and submit handlers; Cancel sends no request.
document.addEventListener('submit', function (event) {
    const form = event.target;
    if (!form.matches('form[data-confirm]')) return;
    if (form.dataset.confirmed === 'true') {
        delete form.dataset.confirmed;
        if (form.dataset.pending === 'true' || !window.StoreFormValidation.validate(form)) event.preventDefault();
        else form.dataset.pending = 'true';
        return;
    }
    event.preventDefault();
    if (form.dataset.pending === 'true' || form.dataset.reviewing === 'true') return;
    if (!window.StoreFormValidation.validate(form)) return;
    if (typeof window.showGlobalConfirmModal !== 'function') return;
    form.dataset.reviewing = 'true';
    const submitter = event.submitter;
    window.showGlobalConfirmModal({ title: 'Xác nhận', message: form.dataset.confirm,
        confirmText: 'Xác nhận', cancelText: 'Hủy',
        onCancel: function () { delete form.dataset.reviewing; },
        onConfirm: function () {
            delete form.dataset.reviewing;
            form.dataset.confirmed = 'true';
            form.requestSubmit(submitter || undefined);
        }
    });
});
