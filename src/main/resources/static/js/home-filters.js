(function () {
    function getQueryParams() {
        return new URLSearchParams(window.location.search);
    }

    function parseParamValues(params, key) {
        const list = [];
        params.getAll(key).forEach(function (item) {
            item.split(',').forEach(function (v) {
                const s = v.trim();
                if (s && !list.includes(s)) list.push(s);
            });
        });
        return list;
    }

    function getAppliedState() {
        const p = getQueryParams();
        return {
            flowerStyle: new Set(parseParamValues(p, 'flowerStyle')),
            flowerType: new Set(parseParamValues(p, 'flowerType')),
            flowerColor: new Set(parseParamValues(p, 'flowerColor')),
            flowerOccasion: new Set(parseParamValues(p, 'flowerOccasion')),
            minPrice: p.get('minPrice') || '',
            maxPrice: p.get('maxPrice') || '',
            storeId: p.get('storeId') || ''
        };
    }

    let appliedFilters = getAppliedState();
    let draftFilters = {
        flowerStyle: new Set(appliedFilters.flowerStyle),
        flowerType: new Set(appliedFilters.flowerType),
        flowerColor: new Set(appliedFilters.flowerColor),
        flowerOccasion: new Set(appliedFilters.flowerOccasion),
        minPrice: appliedFilters.minPrice,
        maxPrice: appliedFilters.maxPrice,
        storeId: appliedFilters.storeId
    };

    const drawerEl = document.getElementById('allFiltersOffcanvas');
    const minPriceInput = document.getElementById('drawerMinPrice');
    const maxPriceInput = document.getElementById('drawerMaxPrice');
    const btnReset = document.getElementById('btnDrawerReset');
    const btnApply = document.getElementById('btnDrawerApply');

    function syncDrawerUI() {
        if (!drawerEl) return;
        // Sync nhóm chi nhánh
        drawerEl.querySelectorAll('.drawer-filter-chip[data-store-id]').forEach(function (btn) {
            const selected = btn.getAttribute('data-store-id') === draftFilters.storeId;
            btn.classList.toggle('active', selected);
            btn.setAttribute('aria-pressed', String(selected));
        });

        // Sync nhóm thuộc tính: Kiểu dáng, Loại hoa, Màu sắc, Dịp tặng
        drawerEl.querySelectorAll('.drawer-filter-chip[data-group]').forEach(function (btn) {
            const grp = btn.getAttribute('data-group');
            const val = btn.getAttribute('data-val');
            if (draftFilters[grp] && draftFilters[grp].has(val)) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });

        // Sync khoảng giá
        if (minPriceInput) minPriceInput.value = draftFilters.minPrice || '';
        if (maxPriceInput) maxPriceInput.value = draftFilters.maxPrice || '';

        drawerEl.querySelectorAll('.drawer-price-chip').forEach(function (btn) {
            const min = btn.getAttribute('data-min') || '';
            const max = btn.getAttribute('data-max') || '';
            const isMatch = (draftFilters.minPrice === min) && (draftFilters.maxPrice === max);
            if (isMatch && (min !== '' || max !== '')) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });
    }

    syncDrawerUI();

    // Mở lại bảng: hiển thị đúng các bộ lọc đã áp dụng
    if (drawerEl) {
        drawerEl.addEventListener('show.bs.offcanvas', function () {
            appliedFilters = getAppliedState();
            draftFilters = {
                flowerStyle: new Set(appliedFilters.flowerStyle),
                flowerType: new Set(appliedFilters.flowerType),
                flowerColor: new Set(appliedFilters.flowerColor),
                flowerOccasion: new Set(appliedFilters.flowerOccasion),
                minPrice: appliedFilters.minPrice,
                maxPrice: appliedFilters.maxPrice,
                storeId: appliedFilters.storeId
            };
            syncDrawerUI();
        });

        // Đóng bằng × hoặc bấm ra ngoài: bỏ thay đổi chưa áp dụng, giữ kết quả cũ
        drawerEl.addEventListener('hidden.bs.offcanvas', function () {
            draftFilters = {
                flowerStyle: new Set(appliedFilters.flowerStyle),
                flowerType: new Set(appliedFilters.flowerType),
                flowerColor: new Set(appliedFilters.flowerColor),
                flowerOccasion: new Set(appliedFilters.flowerOccasion),
                minPrice: appliedFilters.minPrice,
                maxPrice: appliedFilters.maxPrice,
                storeId: appliedFilters.storeId
            };
            syncDrawerUI();
        });
    }

    // Chọn nhiều trong từng nhóm: Bấm lại để bỏ chọn; chọn mục mới không làm mất mục đã chọn trước
    if (drawerEl) {
        drawerEl.addEventListener('click', function (e) {
            const chipBtn = e.target.closest('.drawer-filter-chip');
            if (!chipBtn) return;

            if (chipBtn.hasAttribute('data-store-id')) {
                e.preventDefault();
                draftFilters.storeId = chipBtn.getAttribute('data-store-id');
                syncDrawerUI();
                return;
            }

            const grp = chipBtn.getAttribute('data-group');
            const val = chipBtn.getAttribute('data-val');

            if (grp && val) {
                e.preventDefault();
                if (draftFilters[grp].has(val)) {
                    draftFilters[grp].delete(val);
                    chipBtn.classList.remove('active');
                } else {
                    draftFilters[grp].add(val);
                    chipBtn.classList.add('active');
                }
                return;
            }

            // Khoảng giá nhanh
            if (chipBtn.classList.contains('drawer-price-chip')) {
                e.preventDefault();
                const min = chipBtn.getAttribute('data-min') || '';
                const max = chipBtn.getAttribute('data-max') || '';

                if (draftFilters.minPrice === min && draftFilters.maxPrice === max) {
                    draftFilters.minPrice = '';
                    draftFilters.maxPrice = '';
                } else {
                    draftFilters.minPrice = min;
                    draftFilters.maxPrice = max;
                }
                syncDrawerUI();
            }
        });
    }

    // Nhập tay khoảng giá
    if (minPriceInput) {
        minPriceInput.addEventListener('input', function () {
            draftFilters.minPrice = this.value.trim();
            drawerEl.querySelectorAll('.drawer-price-chip').forEach(function (btn) {
                const min = btn.getAttribute('data-min') || '';
                const max = btn.getAttribute('data-max') || '';
                if (draftFilters.minPrice === min && draftFilters.maxPrice === max && (min !== '' || max !== '')) {
                    btn.classList.add('active');
                } else {
                    btn.classList.remove('active');
                }
            });
        });
    }
    if (maxPriceInput) {
        maxPriceInput.addEventListener('input', function () {
            draftFilters.maxPrice = this.value.trim();
            drawerEl.querySelectorAll('.drawer-price-chip').forEach(function (btn) {
                const min = btn.getAttribute('data-min') || '';
                const max = btn.getAttribute('data-max') || '';
                if (draftFilters.minPrice === min && draftFilters.maxPrice === max && (min !== '' || max !== '')) {
                    btn.classList.add('active');
                } else {
                    btn.classList.remove('active');
                }
            });
        });
    }

    // Đặt lại: bỏ các lựa chọn trong bảng; danh sách bên ngoài chỉ thay đổi sau khi bấm Áp dụng
    if (btnReset) {
        btnReset.addEventListener('click', function () {
            draftFilters.flowerStyle.clear();
            draftFilters.flowerType.clear();
            draftFilters.flowerColor.clear();
            draftFilters.flowerOccasion.clear();
            draftFilters.minPrice = '';
            draftFilters.maxPrice = '';
            draftFilters.storeId = '';
            syncDrawerUI();
        });
    }

    // [✓ Áp dụng]: lưu các lựa chọn, đóng bảng, cập nhật sản phẩm và quay về trang kết quả số 1
    if (btnApply) {
        btnApply.addEventListener('click', function () {
            const p = getQueryParams();

            p.delete('flowerStyle');
            p.delete('flowerType');
            p.delete('flowerColor');
            p.delete('flowerOccasion');
            p.delete('minPrice');
            p.delete('maxPrice');

            draftFilters.flowerStyle.forEach(function (v) { p.append('flowerStyle', v); });
            draftFilters.flowerType.forEach(function (v) { p.append('flowerType', v); });
            draftFilters.flowerColor.forEach(function (v) { p.append('flowerColor', v); });
            draftFilters.flowerOccasion.forEach(function (v) { p.append('flowerOccasion', v); });

            if (draftFilters.minPrice) p.set('minPrice', draftFilters.minPrice);
            if (draftFilters.maxPrice) p.set('maxPrice', draftFilters.maxPrice);
            if (draftFilters.storeId) p.set('storeId', draftFilters.storeId);
            else p.delete('storeId');

            // Quay về trang kết quả số 1
            p.set('page', '1');

            // Giữ nguyên từ khóa tìm kiếm và danh mục đang xem (đã có trong p)

            const bsOffcanvas = bootstrap.Offcanvas.getInstance(drawerEl);
            if (bsOffcanvas) {
                bsOffcanvas.hide();
            }

            window.location.href = window.location.pathname + '?' + p.toString() + '#product-catalog';
        });
    }

    // Thao tác bỏ từng nhãn lọc bằng thuộc tính data-*
    window.removeFilterBadge = function (btn) {
        if (!btn) return;
        const group = btn.getAttribute('data-group');
        const value = btn.getAttribute('data-value') || btn.getAttribute('data-val');
        if (group && value) {
            removeSingleFilter(group, value);
        }
    };

    // Các hàm bỏ từng điều kiện lọc ở thanh "Đang lọc: [Tên điều kiện ×]"
    window.removeSingleFilter = function (group, value) {
        const p = getQueryParams();
        const existing = [];
        const targetVal = (value || '').trim().toLowerCase();
        p.getAll(group).forEach(function (item) {
            item.split(',').forEach(function (v) {
                const s = v.trim();
                if (s && s.toLowerCase() !== targetVal && !existing.includes(s)) {
                    existing.push(s);
                }
            });
        });

        p.delete(group);
        existing.forEach(function (v) { p.append(group, v); });
        p.set('page', '1');
        window.location.href = window.location.pathname + '?' + p.toString() + '#product-catalog';
    };

    window.removePriceFilter = function () {
        const p = getQueryParams();
        p.delete('minPrice');
        p.delete('maxPrice');
        p.set('page', '1');
        window.location.href = window.location.pathname + '?' + p.toString() + '#product-catalog';
    };

    window.removeKeywordFilter = function () {
        const p = getQueryParams();
        p.delete('keyword');
        p.set('page', '1');
        window.location.href = window.location.pathname + '?' + p.toString() + '#product-catalog';
    };

    window.removeRatingFilter = function () {
        const p = getQueryParams();
        p.delete('minRating');
        p.set('page', '1');
        window.location.href = window.location.pathname + '?' + p.toString() + '#product-catalog';
    };

    window.removeTabFilter = function () {
        const p = getQueryParams();
        p.delete('tab');
        p.set('page', '1');
        window.location.href = window.location.pathname + '?' + p.toString() + '#product-catalog';
    };

    window.resetAllCatalogFilters = function () {
        const p = getQueryParams();
        p.delete('storeId');
        p.delete('flowerStyle');
        p.delete('flowerType');
        p.delete('flowerColor');
        p.delete('flowerOccasion');
        p.delete('minPrice');
        p.delete('maxPrice');
        p.delete('minRating');
        p.delete('tab');
        p.set('page', '1');
        window.location.href = window.location.pathname + '?' + p.toString() + '#product-catalog';
    };

    window.removeCategoryFilter = function () {
        const p = getQueryParams();
        p.delete('categoryId');
        p.set('page', '1');
        window.location.href = window.location.pathname + '?' + p.toString() + '#product-catalog';
    };

    window.resetAttributeFiltersOnly = function () {
        const p = getQueryParams();
        p.delete('storeId');
        p.delete('flowerStyle');
        p.delete('flowerType');
        p.delete('flowerColor');
        p.delete('flowerOccasion');
        p.delete('minPrice');
        p.delete('maxPrice');
        p.delete('minRating');
        p.delete('tab');
        p.set('page', '1');
        window.location.href = window.location.pathname + '?' + p.toString() + '#product-catalog';
    };

    window.applyCatalogStore = function (storeId) {
        const p = getQueryParams();
        if (storeId) {
            p.set('storeId', storeId);
        } else {
            p.delete('storeId');
        }
        p.set('page', '1');
        window.location.href = window.location.pathname + '?' + p.toString() + '#product-catalog';
    };

    window.applyCatalogSort = function (sortVal) {
        const p = getQueryParams();
        if (sortVal && sortVal !== 'default') {
            p.set('sort', sortVal);
        } else {
            p.delete('sort');
        }
        p.set('page', '1');
        window.location.href = window.location.pathname + '?' + p.toString() + '#product-catalog';
    };

    window.scrollToFilterGroup = function (groupId) {
        const drawer = document.getElementById('allFiltersOffcanvas');
        if (drawer) {
            const bsOffcanvas = bootstrap.Offcanvas.getOrCreateInstance(drawer);
            bsOffcanvas.show();
            setTimeout(function () {
                const targetEl = document.getElementById(groupId);
                if (targetEl) {
                    targetEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
            }, 350);
        }
    };
})();
