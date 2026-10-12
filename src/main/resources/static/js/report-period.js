(() => {
    'use strict';
    document.querySelectorAll('[data-report-period-controls]').forEach(controls => {
        const select = controls.querySelector('[data-report-period]');
        const start = controls.querySelector('[data-report-start]');
        const end = controls.querySelector('[data-report-end]');
        const customFields = controls.querySelectorAll('[data-report-custom]');
        const validate = () => {
            const invalid = select.value === 'custom' && start.value && end.value && start.value > end.value;
            end.setCustomValidity(invalid ? 'Đến ngày phải bằng hoặc sau từ ngày.' : '');
        };
        const toggle = () => {
            const custom = select.value === 'custom';
            customFields.forEach(field => { field.hidden = !custom; });
            [start, end].forEach(input => { input.disabled = !custom; input.required = custom; });
            validate();
        };
        select.addEventListener('change', () => {
            toggle();
            if (select.value === 'custom') start.focus();
            else select.form.requestSubmit();
        });
        [start, end].forEach(input => input.addEventListener('input', validate));
        toggle();
    });
})();
