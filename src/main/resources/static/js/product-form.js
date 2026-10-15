(function () {
    'use strict';
    const price = document.getElementById('productPrice');
    const promotion = document.getElementById('productPromotion');
    if (price && promotion) {
        function validatePromotion() {
            const amount = Number(promotion.value);
            const regular = Number(price.value);
            promotion.setCustomValidity(promotion.value !== '' &&
                (!Number.isInteger(amount) || amount < 1 || amount > regular)
                ? 'Giá khuyến mãi phải là số đồng nguyên, từ 1đ và không lớn hơn giá bán. Để trống nếu không áp dụng.' : '');
        }
        price.addEventListener('input', validatePromotion);
        promotion.addEventListener('input', validatePromotion);
        validatePromotion();
    }

    // Manager Dropdown Selectors
    const extraSummary = document.getElementById('managerExtraCatSummary');
    const extraBadge = document.getElementById('managerExtraCatBadge');
    const extraCheckboxes = document.querySelectorAll('.manager-extra-cat-cb');
    const clearExtraBtn = document.getElementById('managerClearExtraCatBtn');

    function updateManagerExtraSummary() {
        if (!extraSummary) return;
        const checked = [];
        extraCheckboxes.forEach(function(cb) {
            if (cb.checked) {
                const label = document.querySelector('label[for="' + cb.id + '"]');
                checked.push(label ? label.textContent.trim() : cb.value);
            }
        });

        if (checked.length === 0) {
            extraSummary.textContent = '-- Chọn các danh mục bổ sung --';
            extraSummary.classList.add('text-muted');
            if (extraBadge) extraBadge.classList.add('d-none');
        } else if (checked.length <= 2) {
            extraSummary.textContent = checked.join(', ');
            extraSummary.classList.remove('text-muted');
            if (extraBadge) {
                extraBadge.textContent = checked.length;
                extraBadge.classList.remove('d-none');
            }
        } else {
            extraSummary.textContent = 'Đã chọn ' + checked.length + ' danh mục (' + checked.slice(0, 2).join(', ') + '...)';
            extraSummary.classList.remove('text-muted');
            if (extraBadge) {
                extraBadge.textContent = checked.length;
                extraBadge.classList.remove('d-none');
            }
        }
    }

    extraCheckboxes.forEach(function(cb) {
        cb.addEventListener('change', updateManagerExtraSummary);
    });

    if (clearExtraBtn) {
        clearExtraBtn.addEventListener('click', function(e) {
            e.preventDefault();
            extraCheckboxes.forEach(function(cb) { cb.checked = false; });
            updateManagerExtraSummary();
        });
    }
    updateManagerExtraSummary();

    // Manager Attribute Groups
    document.querySelectorAll('.manager-attr-btn').forEach(function(btn) {
        const dropdownMenu = btn.nextElementSibling;
        if (!dropdownMenu) return;
        const summaryEl = btn.querySelector('.manager-attr-summary');
        const badgeEl = btn.querySelector('.manager-attr-badge');
        const cbs = dropdownMenu.querySelectorAll('.manager-attr-cb');
        const defaultPlaceholder = summaryEl ? summaryEl.textContent.trim() : '-- Chọn --';

        function updateAttrGroup() {
            const checkedNames = [];
            cbs.forEach(function(cb) {
                if (cb.checked) {
                    let name = cb.getAttribute('data-name') || cb.dataset.name;
                    if (!name) {
                        const lbl = dropdownMenu.querySelector('label[for="' + cb.id + '"]');
                        name = lbl ? lbl.textContent.trim() : cb.value;
                    }
                    checkedNames.push(name);
                }
            });

            if (checkedNames.length === 0) {
                if (summaryEl) {
                    summaryEl.textContent = defaultPlaceholder;
                    summaryEl.classList.add('text-muted');
                }
                if (badgeEl) badgeEl.classList.add('d-none');
            } else if (checkedNames.length === 1) {
                if (summaryEl) {
                    summaryEl.textContent = checkedNames[0];
                    summaryEl.classList.remove('text-muted');
                }
                if (badgeEl) {
                    badgeEl.textContent = '1';
                    badgeEl.classList.remove('d-none');
                }
            } else {
                if (summaryEl) {
                    summaryEl.textContent = checkedNames[0] + ' (+' + (checkedNames.length - 1) + ')';
                    summaryEl.classList.remove('text-muted');
                }
                if (badgeEl) {
                    badgeEl.textContent = checkedNames.length;
                    badgeEl.classList.remove('d-none');
                }
            }
        }

        cbs.forEach(function(cb) {
            cb.addEventListener('change', updateAttrGroup);
        });
        updateAttrGroup();
    });
})();

