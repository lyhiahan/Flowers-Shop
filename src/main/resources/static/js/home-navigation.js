// Tự động điền ngày nhận hoa hôm nay
document.addEventListener('DOMContentLoaded', function () {
    var today = new Date().toISOString().split('T')[0];
    document.querySelectorAll('input[name="deliveryDate"]').forEach(function (inp) {
        if (!inp.value) inp.value = today;
    });
});


// Gọi Ajax kiểm tra tồn kho tại chi nhánh hoa
function verifyStock(productId) {
    var storeSelect = document.getElementById('storeSelect_' + productId);
    var storeId = storeSelect.value;
    var hintBox = document.getElementById('stockHint_' + productId);
    var submitBtn = document.getElementById('submitBtn_' + productId);

    if (!storeId) {
        hintBox.classList.add('d-none');
        submitBtn.disabled = false;
        return;
    }

    fetch('/api/inventory/check?productId=' + productId + '&storeId=' + storeId)
        .then(response => response.json())
        .then(data => {
            hintBox.classList.remove('d-none');
            if (data.available) {
                hintBox.className = "alert alert-success py-1 px-2 small mb-2";
                hintBox.innerHTML = "✅ Sẵn sàng <strong>" + data.currentStock + "</strong> mẫu hoa tại cửa hàng này.";
                submitBtn.disabled = false;
            } else {
                hintBox.className = "alert alert-warning py-1 px-2 small mb-2";
                var html = "❌ Chi nhánh này vừa hết mẫu hoa! <br/>";
                if (data.alternativeSuggestion) {
                    html += "📍 <em>" + data.alternativeSuggestion + "</em>";
                }
                hintBox.innerHTML = html;
                submitBtn.disabled = true;
            }
        })
        .catch(err => console.error("Lỗi check tồn kho:", err));
}


// Dừng và giữ ngay lập tức tại vị trí danh sách sản phẩm (không có hiệu ứng cuộn, không bị hoa mắt)
(function () {
    var urlParams = new URLSearchParams(window.location.search);
    var hasFilter = urlParams.has('tab') || urlParams.has('keyword') || urlParams.has('categoryId') || urlParams.has('sort') || urlParams.has('page');
    if (hasFilter || window.location.hash === '#product-catalog') {
        if ('scrollRestoration' in history) {
            history.scrollRestoration = 'manual';
        }
        var jumpImmediately = function () {
            var catalogEl = document.getElementById('product-catalog');
            if (catalogEl) {
                catalogEl.scrollIntoView({ behavior: 'instant', block: 'start' });
            }
        };
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', jumpImmediately);
        } else {
            jumpImmediately();
        }
    }
})();

/* ========================================================
   JAVASCRIPT ĐIỀU KHIỂN BẢNG DANH MỤC (MEGA-MENU)
   - Hỗ trợ mở từ cả [Danh mục hoa] ở navbar và [Danh mục: ... ▾] ở catalog
   - Mở nổi ngay bên dưới nút được bấm, không đẩy nội dung
   - Tự động đóng khi bấm lại, bấm ngoài hoặc nhấn phím Esc
   ======================================================== */
function toggleFlowerMegaMenu(e, source) {
    if (e) {
        e.preventDefault();
        e.stopPropagation();
    }
    var panel = document.getElementById('flowerMegaMenuPanel');
    var backdrop = document.getElementById('flowerMegaMenuBackdrop');
    var navTrigger = document.getElementById('btnFlowerMegaMenu');
    var catTrigger = document.getElementById('btnCategoryPicker');
    if (!panel) return;

    var chosenSource = source || (panel.getAttribute('data-opened-from') || 'navbar');
    var targetWrapper = (chosenSource === 'catalog')
        ? document.getElementById('catalogCategoryDropdownWrapper')
        : document.getElementById('navbarCategoryDropdownWrapper');

    var isOpen = panel.classList.contains('is-open');
    var isCurrentSource = panel.getAttribute('data-opened-from') === chosenSource;

    if (isOpen && isCurrentSource) {
        closeFlowerMegaMenu();
        return;
    }

    document.body.appendChild(panel);
    panel.setAttribute('data-opened-from', chosenSource);
    var rect = targetWrapper ? targetWrapper.getBoundingClientRect() : {left:16,bottom:80};
    var width = Math.min(1060, window.innerWidth - 32);
    var top = Math.max(16, Math.min(rect.bottom + 8, window.innerHeight - 320));
    panel.style.width = width + 'px';
    panel.style.left = Math.max(16, Math.min(rect.left, window.innerWidth - width - 16)) + 'px';
    panel.style.top = top + 'px';
    panel.style.maxHeight = Math.max(160, window.innerHeight - top - 16) + 'px';
    panel.style.overflowY = 'auto';

    panel.classList.add('is-open');
    if (backdrop) backdrop.classList.add('is-open');

    if (chosenSource === 'catalog') {
        if (catTrigger) {
            catTrigger.classList.add('is-active');
            catTrigger.setAttribute('aria-expanded', 'true');
        }
        if (navTrigger) {
            navTrigger.classList.remove('is-active');
            navTrigger.setAttribute('aria-expanded', 'false');
        }
    } else {
        if (navTrigger) {
            navTrigger.classList.add('is-active');
            navTrigger.setAttribute('aria-expanded', 'true');
        }
        if (catTrigger) {
            catTrigger.classList.remove('is-active');
            catTrigger.setAttribute('aria-expanded', 'false');
        }
    }

    var searchInput = document.getElementById('megaMenuSearchInput');
    if (searchInput) {
        searchInput.value = '';
        filterMegaMenu('');
        setTimeout(function() {
            searchInput.focus();
        }, 120);
    }
}

function closeFlowerMegaMenu() {
    var panel = document.getElementById('flowerMegaMenuPanel');
    var backdrop = document.getElementById('flowerMegaMenuBackdrop');
    var navTrigger = document.getElementById('btnFlowerMegaMenu');
    var catTrigger = document.getElementById('btnCategoryPicker');
    if (panel) {
        panel.classList.remove('is-open');
        panel.removeAttribute('data-opened-from');
    }
    if (backdrop) backdrop.classList.remove('is-open');
    if (navTrigger) {
        navTrigger.classList.remove('is-active');
        navTrigger.setAttribute('aria-expanded', 'false');
    }
    if (catTrigger) {
        catTrigger.classList.remove('is-active');
        catTrigger.setAttribute('aria-expanded', 'false');
    }
}

function switchMegaTab(paneId, btnEl) {
    var allBtns = document.querySelectorAll('.mega-tab-btn');
    allBtns.forEach(function(b) {
        b.classList.remove('active');
        b.setAttribute('aria-selected', 'false');
    });
    if (btnEl) {
        btnEl.classList.add('active');
        btnEl.setAttribute('aria-selected', 'true');
    }

    var allPanes = document.querySelectorAll('.mega-tab-pane');
    allPanes.forEach(function(p) {
        p.classList.remove('active');
    });
    var targetPane = document.getElementById(paneId);
    if (targetPane) {
        targetPane.classList.add('active');
    }

    // Giữ lại từ khóa tìm kiếm danh mục nếu có
    var searchInput = document.getElementById('megaMenuSearchInput');
    if (searchInput && searchInput.value) {
        filterMegaMenu(searchInput.value);
    }
}

function filterMegaMenu(query) {
    var q = (query || '').trim().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd');
    var allCards = document.querySelectorAll('.mega-cat-card');

    allCards.forEach(function(card) {
        var dataName = (card.getAttribute('data-name') || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd');
        var cardText = (card.querySelector('.mega-cat-name') ? card.querySelector('.mega-cat-name').textContent : '').toLowerCase();
        if (!q || dataName.indexOf(q) !== -1 || cardText.indexOf(q) !== -1) {
            card.style.display = '';
        } else {
            card.style.display = 'none';
        }
    });
}

// Đóng bảng danh mục khi nhấn ESC hoặc bấm ngoài bảng
window.addEventListener('resize', closeFlowerMegaMenu);
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape' || e.key === 'Esc') {
        closeFlowerMegaMenu();
    }
});

document.addEventListener('click', function(e) {
    var panel = document.getElementById('flowerMegaMenuPanel');
    var navTrigger = document.getElementById('btnFlowerMegaMenu');
    var catTrigger = document.getElementById('btnCategoryPicker');
    if (!panel || !panel.classList.contains('is-open')) return;

    var clickedInsidePanel = panel.contains(e.target);
    var clickedNavTrigger = navTrigger && navTrigger.contains(e.target);
    var clickedCatTrigger = catTrigger && catTrigger.contains(e.target);

    if (!clickedInsidePanel && !clickedNavTrigger && !clickedCatTrigger) {
        closeFlowerMegaMenu();
    }
});
