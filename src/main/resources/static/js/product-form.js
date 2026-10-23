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

    // Multi-image upload, live preview & main image selection
    const fileInput = document.getElementById('productImageFiles');
    const legacyInput = document.getElementById('productImageFile');
    const mainIndexInput = document.getElementById('mainImageIndex');
    const previewContainer = document.getElementById('imagePreviewContainer');
    const previewGrid = document.getElementById('imagePreviewGrid');
    const countBadge = document.getElementById('imageCountBadge');
    const mainLabel = document.getElementById('currentMainLabel');

    if (fileInput && previewGrid && mainIndexInput) {
        let currentFiles = [];
        let selectedMainIndex = 0;

        function formatBytes(bytes) {
            if (!bytes || bytes === 0) return '0 B';
            const k = 1024;
            const sizes = ['B', 'KB', 'MB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
        }

        function renderPreviews() {
            previewGrid.innerHTML = '';
            if (currentFiles.length === 0) {
                if (previewContainer) previewContainer.classList.add('d-none');
                if (countBadge) {
                    countBadge.textContent = 'Chưa chọn ảnh mới';
                    countBadge.className = 'badge rounded-pill bg-white text-secondary border px-3 py-1.5';
                }
                mainIndexInput.value = '0';
                if (legacyInput) legacyInput.value = '';
                return;
            }

            if (previewContainer) previewContainer.classList.remove('d-none');
            if (countBadge) {
                countBadge.textContent = 'Đã chọn ' + currentFiles.length + ' ảnh';
                countBadge.className = 'badge rounded-pill bg-success text-white px-3 py-1.5';
            }

            // Ensure selectedMainIndex is within bounds
            if (selectedMainIndex < 0 || selectedMainIndex >= currentFiles.length) {
                selectedMainIndex = 0;
            }
            mainIndexInput.value = selectedMainIndex;
            if (mainLabel) {
                mainLabel.textContent = 'Ảnh #' + (selectedMainIndex + 1) + ' là ảnh chính';
            }

            // Sync with legacy input if needed
            if (legacyInput && currentFiles.length > 0) {
                try {
                    const dtLegacy = new DataTransfer();
                    dtLegacy.items.add(currentFiles[selectedMainIndex]);
                    legacyInput.files = dtLegacy.files;
                } catch (e) {}
            }

            currentFiles.forEach(function (file, index) {
                const isMain = (index === selectedMainIndex);
                const card = document.createElement('div');
                card.className = 'product-img-card position-relative ' + (isMain ? 'is-main-card' : '');
                card.setAttribute('data-index', index);

                const objectUrl = URL.createObjectURL(file);

                card.innerHTML = `
                    <div class="position-absolute top-0 start-0 m-2 z-2">
                        ${isMain 
                            ? '<span class="badge rounded-pill bg-success text-white shadow-sm px-2 py-1"><i class="bi bi-star-fill me-1"></i> Ảnh chính</span>' 
                            : '<span class="badge rounded-pill bg-white text-muted border shadow-xs px-2 py-1"><i class="bi bi-image me-1"></i> #' + (index + 1) + '</span>'}
                    </div>
                    <button type="button" class="btn btn-sm btn-danger position-absolute top-0 end-0 m-2 rounded-circle p-0 remove-file-btn shadow-xs" 
                            data-index="${index}" title="Xóa ảnh này" style="width: 24px; height: 24px; line-height: 1; z-index: 3;">
                        <i class="bi bi-x"></i>
                    </button>
                    <div class="ratio ratio-1x1 rounded-3 overflow-hidden bg-light border mb-2">
                        <img src="${objectUrl}" class="w-100 h-100 object-fit-cover" alt="Ảnh ${index + 1}">
                    </div>
                    <div class="text-center">
                        <div class="text-truncate fw-semibold mb-0" title="${file.name}" style="font-size: 12px; color: #20382E;">${file.name}</div>
                        <div class="text-muted" style="font-size: 11px;">${formatBytes(file.size)}</div>
                        <button type="button" class="btn btn-xs w-100 mt-2 select-main-btn ${isMain ? 'btn-success text-white' : 'btn-outline-success'}" data-index="${index}">
                            <i class="bi ${isMain ? 'bi-check2-circle' : 'bi-star'} me-1"></i> ${isMain ? 'Đang là ảnh chính' : 'Đặt làm ảnh chính'}
                        </button>
                    </div>
                `;

                // Set main on card click (except remove button)
                card.addEventListener('click', function (e) {
                    if (e.target.closest('.remove-file-btn')) return;
                    selectedMainIndex = index;
                    renderPreviews();
                });

                // Remove button
                const removeBtn = card.querySelector('.remove-file-btn');
                if (removeBtn) {
                    removeBtn.addEventListener('click', function (e) {
                        e.stopPropagation();
                        currentFiles.splice(index, 1);
                        try {
                            const dt = new DataTransfer();
                            currentFiles.forEach(f => dt.items.add(f));
                            fileInput.files = dt.files;
                        } catch (err) {}
                        if (selectedMainIndex >= currentFiles.length) {
                            selectedMainIndex = Math.max(0, currentFiles.length - 1);
                        }
                        renderPreviews();
                    });
                }

                previewGrid.appendChild(card);
            });
        }

        fileInput.addEventListener('change', function () {
            if (fileInput.files && fileInput.files.length > 0) {
                currentFiles = Array.from(fileInput.files);
                selectedMainIndex = 0; // Default: first image is main
                renderPreviews();
            } else {
                currentFiles = [];
                renderPreviews();
            }
        });
    }
})();

