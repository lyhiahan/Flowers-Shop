/**
 * Global Screen Modal System (Hộp Thoại Thông Báo & Xác Nhận Đưa Ra Giữa Màn Hình)
 * Thay thế hoàn toàn các hộp thoại mặc định thô sơ của trình duyệt (localhost:8080 says...)
 * như window.alert(), window.confirm(), onclick="return confirm(...)", onsubmit="return confirm(...)"
 * Bằng hộp thoại Modal giao diện thương hiệu Hoa Tươi Sài Gòn (Centering, bo góc 18px, nút bấm trang nhã, hiệu ứng mờ nền).
 */
(function () {
    // 1. Tạo CSS riêng cho Global Modal nếu chưa có
    var styleId = 'global-modal-styles';
    if (!document.getElementById(styleId)) {
        var style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
            .g-modal-backdrop {
                position: fixed;
                top: 0;
                left: 0;
                width: 100vw;
                height: 100vh;
                background: rgba(15, 23, 42, 0.6);
                backdrop-filter: blur(5px);
                -webkit-backdrop-filter: blur(5px);
                z-index: 999999;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 16px;
                opacity: 0;
                visibility: hidden;
                transition: opacity 0.22s ease, visibility 0.22s ease;
            }
            .g-modal-backdrop.show {
                opacity: 1;
                visibility: visible;
            }
            .g-modal-box {
                background: #FFFFFF;
                border-radius: 20px;
                width: 100%;
                max-width: 460px;
                box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25), 0 0 0 1px rgba(0, 0, 0, 0.06);
                overflow: hidden;
                transform: scale(0.92) translateY(12px);
                transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
                font-family: 'Nunito', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            }
            .g-modal-backdrop.show .g-modal-box {
                transform: scale(1) translateY(0);
            }
            .g-modal-header {
                padding: 18px 24px;
                display: flex;
                align-items: center;
                justify-content: space-between;
                border-bottom: 1px solid #F1F5F9;
            }
            .g-modal-header.danger {
                background: linear-gradient(135deg, #FFF1F2 0%, #FFE4E6 100%);
            }
            .g-modal-header.success {
                background: linear-gradient(135deg, #ECFDF5 0%, #D1FAE5 100%);
            }
            .g-modal-header.warning {
                background: linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 100%);
            }
            .g-modal-header.info {
                background: linear-gradient(135deg, #F0FDF4 0%, #E8F5E9 100%);
            }
            .g-modal-title-group {
                display: flex;
                align-items: center;
                gap: 12px;
            }
            .g-modal-icon-badge {
                width: 40px;
                height: 40px;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 19px;
                flex-shrink: 0;
            }
            .g-modal-icon-badge.danger {
                background: rgba(225, 29, 72, 0.15);
                color: #E11D48;
            }
            .g-modal-icon-badge.success {
                background: rgba(16, 185, 129, 0.15);
                color: #059669;
            }
            .g-modal-icon-badge.warning {
                background: rgba(245, 158, 11, 0.18);
                color: #D97706;
            }
            .g-modal-icon-badge.info {
                background: rgba(53, 84, 71, 0.12);
                color: #355447;
            }
            .g-modal-title {
                font-size: 17.5px;
                font-weight: 700;
                color: #1E293B;
                margin: 0;
                line-height: 1.25;
            }
            .g-modal-subtitle {
                font-size: 12px;
                color: #64748B;
                margin-top: 2px;
            }
            .g-modal-close-btn {
                background: transparent;
                border: none;
                font-size: 22px;
                color: #94A3B8;
                cursor: pointer;
                padding: 4px 8px;
                border-radius: 8px;
                line-height: 1;
                transition: background 0.15s, color 0.15s;
            }
            .g-modal-close-btn:hover {
                background: rgba(0, 0, 0, 0.05);
                color: #334155;
            }
            .g-modal-body {
                padding: 22px 24px;
                font-size: 15px;
                color: #334155;
                line-height: 1.6;
            }
            .g-modal-message-box {
                background: #F8FAF9;
                border: 1px solid #E2E8F0;
                border-radius: 14px;
                padding: 16px 18px;
                margin-bottom: 8px;
                font-size: 15px;
                color: #1E293B;
                font-weight: 600;
                white-space: pre-line;
                word-break: break-word;
            }
            .g-modal-footer {
                padding: 14px 24px;
                background: #F8FAFC;
                border-top: 1px solid #F1F5F9;
                display: flex;
                justify-content: flex-end;
                gap: 10px;
            }
            .g-modal-btn {
                padding: 9.5px 22px;
                border-radius: 9999px;
                font-size: 14.5px;
                font-weight: 600;
                cursor: pointer;
                border: 1px solid transparent;
                transition: all 0.15s ease;
                display: inline-flex;
                align-items: center;
                gap: 6px;
            }
            .g-modal-btn-cancel {
                background: #FFFFFF;
                border-color: #CBD5E1;
                color: #475569;
            }
            .g-modal-btn-cancel:hover {
                background: #F1F5F9;
                color: #1E293B;
            }
            .g-modal-btn-confirm-danger {
                background: #E11D48;
                color: #FFFFFF;
                border-color: #E11D48;
                box-shadow: 0 2px 8px rgba(225, 29, 72, 0.25);
            }
            .g-modal-btn-confirm-danger:hover {
                background: #BE123C;
                border-color: #BE123C;
            }
            .g-modal-btn-confirm-primary {
                background: #355447;
                color: #FFFFFF;
                border-color: #355447;
                box-shadow: 0 2px 8px rgba(53, 84, 71, 0.25);
            }
            .g-modal-btn-confirm-primary:hover {
                background: #253D33;
                border-color: #253D33;
            }
        `;
        document.head.appendChild(style);
    }

    // 2. Tạo DOM cho Global Modal
    var modalBackdrop = document.createElement('div');
    modalBackdrop.className = 'g-modal-backdrop';
    modalBackdrop.id = 'gModalBackdrop';
    modalBackdrop.innerHTML = `
        <div class="g-modal-box" id="gModalBox">
            <div class="g-modal-header" id="gModalHeader">
                <div class="g-modal-title-group">
                    <div class="g-modal-icon-badge" id="gModalIconBadge">⚠️</div>
                    <div>
                        <h5 class="g-modal-title" id="gModalTitle">Thông Báo</h5>
                        <div class="g-modal-subtitle" id="gModalSubtitle">Hệ thống Hoa Tươi Sài Gòn</div>
                    </div>
                </div>
                <button type="button" class="g-modal-close-btn" id="gModalCloseBtn" aria-label="Đóng">&times;</button>
            </div>
            <div class="g-modal-body">
                <div class="g-modal-message-box" id="gModalMessageBox">Nội dung</div>
                <div class="small" id="gModalExtraText" style="color: #64748B; font-size: 13px;">Vui lòng xác nhận thao tác để tiếp tục.</div>
            </div>
            <div class="g-modal-footer" id="gModalFooter">
                <button type="button" class="g-modal-btn g-modal-btn-cancel" id="gModalCancelBtn">Hủy bỏ</button>
                <button type="button" class="g-modal-btn g-modal-btn-confirm-danger" id="gModalConfirmBtn">Xác nhận</button>
            </div>
        </div>
    `;

    var activeOnConfirm = null;
    var activeOnCancel = null;

    function closeModal() {
        modalBackdrop.classList.remove('show');
        activeOnConfirm = null;
        activeOnCancel = null;
    }

    function initModalDom() {
        if (!document.body) return;
        if (!document.getElementById('gModalBackdrop')) {
            document.body.appendChild(modalBackdrop);
        }

        var closeBtn = document.getElementById('gModalCloseBtn');
        var cancelBtn = document.getElementById('gModalCancelBtn');
        var confirmBtn = document.getElementById('gModalConfirmBtn');

        if (closeBtn && !closeBtn.__bound) {
            closeBtn.__bound = true;
            closeBtn.addEventListener('click', closeModal);
        }
        if (cancelBtn && !cancelBtn.__bound) {
            cancelBtn.__bound = true;
            cancelBtn.addEventListener('click', function () {
                var cb = activeOnCancel;
                closeModal();
                if (typeof cb === 'function') cb();
            });
        }
        if (confirmBtn && !confirmBtn.__bound) {
            confirmBtn.__bound = true;
            confirmBtn.addEventListener('click', function () {
                var cb = activeOnConfirm;
                closeModal();
                if (typeof cb === 'function') cb();
            });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initModalDom);
    } else {
        initModalDom();
    }

    modalBackdrop.addEventListener('click', function (e) {
        if (e.target === modalBackdrop) {
            closeModal();
        }
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && modalBackdrop.classList.contains('show')) {
            closeModal();
        }
    });

    /**
     * Trích xuất thông điệp từ biểu thức confirm(...) một cách chính xác
     */
    function extractConfirmMessage(element, rawCode) {
        if (!rawCode) return 'Bạn có chắc chắn muốn thực hiện thao tác này?';
        try {
            var startIdx = rawCode.indexOf('confirm(');
            if (startIdx !== -1) {
                var openCount = 0;
                var argStart = startIdx + 8;
                var argEnd = -1;
                for (var i = argStart - 1; i < rawCode.length; i++) {
                    if (rawCode[i] === '(') openCount++;
                    else if (rawCode[i] === ')') {
                        openCount--;
                        if (openCount === 0) {
                            argEnd = i;
                            break;
                        }
                    }
                }
                if (argEnd > argStart) {
                    var expr = rawCode.substring(argStart, argEnd);
                    var result = new Function('return (' + expr + ');').call(element);
                    if (result) return String(result);
                }
            }
        } catch (err) {
            // bỏ qua lỗi eval, fallback regex
        }

        var match = rawCode.match(/confirm\(\s*(?:'|")(.*?)(?:'|")\s*\)/);
        if (match) {
            return match[1].replace(/\\n/g, '\n').replace(/\\'/g, "'");
        }
        return 'Bạn có chắc chắn muốn thực hiện thao tác này?';
    }

    /**
     * API hiển thị Confirm Modal đẹp trên màn hình (Hộp Đưa Ra Màn Hình)
     */
    window.showGlobalConfirmModal = function (opts) {
        initModalDom();
        var options = opts || {};
        var message = options.message || 'Bạn có chắc chắn muốn thực hiện thao tác này?';
        var isDanger = options.isDanger !== undefined ? options.isDanger : /xóa|hủy|ngừng|cấm|từ chối|bỏ theo dõi|rút yêu cầu|thu hồi|vĩnh viễn/i.test(message);
        var title = options.title || (isDanger ? 'Xác Nhận Thao Tác' : 'Thông Báo Xác Nhận');
        var confirmText = options.confirmText || (isDanger ? 'Xác nhận xóa' : 'Xác nhận');
        var cancelText = options.cancelText || 'Hủy bỏ';
        var onConfirm = options.onConfirm || null;
        var onCancel = options.onCancel || null;

        activeOnConfirm = onConfirm;
        activeOnCancel = onCancel;

        var header = document.getElementById('gModalHeader');
        var iconBadge = document.getElementById('gModalIconBadge');
        var titleEl = document.getElementById('gModalTitle');
        var subtitleEl = document.getElementById('gModalSubtitle');
        var msgBox = document.getElementById('gModalMessageBox');
        var extraText = document.getElementById('gModalExtraText');
        var cancelBtn = document.getElementById('gModalCancelBtn');
        var confirmBtn = document.getElementById('gModalConfirmBtn');

        if (!header) return;

        header.className = 'g-modal-header ' + (isDanger ? 'danger' : 'info');
        iconBadge.className = 'g-modal-icon-badge ' + (isDanger ? 'danger' : 'info');
        iconBadge.innerHTML = isDanger ? '🗑️' : '🌸';
        titleEl.innerText = title;
        subtitleEl.innerText = 'Xác nhận trên hệ thống Hoa Tươi Sài Gòn';
        msgBox.innerText = message;
        extraText.innerText = isDanger
            ? 'Thao tác này sẽ áp dụng ngay và cập nhật dữ liệu của bạn.'
            : 'Vui lòng kiểm tra kỹ thông tin trước khi xác nhận.';

        cancelBtn.style.display = 'inline-flex';
        cancelBtn.innerText = cancelText;

        confirmBtn.className = 'g-modal-btn ' + (isDanger ? 'g-modal-btn-confirm-danger' : 'g-modal-btn-confirm-primary');
        confirmBtn.innerText = confirmText;

        modalBackdrop.classList.add('show');
    };

    /**
     * API hiển thị Alert Modal đẹp trên màn hình (Hộp Đưa Ra Màn Hình)
     */
    window.showGlobalAlertModal = function (opts) {
        initModalDom();
        var options = typeof opts === 'string' ? { message: opts } : (opts || {});
        var message = options.message || '';
        var isError = options.isError === true;
        var title = options.title || (isError ? 'Thông Báo Cảnh Báo' : 'Thông Báo Hệ Thống');
        var btnText = options.btnText || 'Đã hiểu & Đóng';

        activeOnConfirm = options.onClose || null;
        activeOnCancel = null;

        var header = document.getElementById('gModalHeader');
        var iconBadge = document.getElementById('gModalIconBadge');
        var titleEl = document.getElementById('gModalTitle');
        var subtitleEl = document.getElementById('gModalSubtitle');
        var msgBox = document.getElementById('gModalMessageBox');
        var extraText = document.getElementById('gModalExtraText');
        var cancelBtn = document.getElementById('gModalCancelBtn');
        var confirmBtn = document.getElementById('gModalConfirmBtn');

        if (!header) return;

        header.className = 'g-modal-header ' + (isError ? 'danger' : 'success');
        iconBadge.className = 'g-modal-icon-badge ' + (isError ? 'danger' : 'success');
        iconBadge.innerHTML = isError ? '⚠️' : '✅';
        titleEl.innerText = title;
        subtitleEl.innerText = 'Thông báo từ Hoa Tươi Sài Gòn';
        msgBox.innerText = message;
        extraText.innerText = '';

        cancelBtn.style.display = 'none';
        confirmBtn.className = 'g-modal-btn g-modal-btn-confirm-primary';
        confirmBtn.innerText = btnText;

        modalBackdrop.classList.add('show');
    };

    // 3. Tự động chặn và chuyển đổi mọi onclick có confirm(...) thành Global Modal
    document.addEventListener('click', function (e) {
        var btn = e.target.closest('[onclick*="confirm("], a[onclick*="confirm("]');
        if (btn) {
            var rawOnclick = btn.getAttribute('onclick');
            if (rawOnclick && rawOnclick.indexOf('confirm(') !== -1) {
                e.preventDefault();
                e.stopImmediatePropagation();

                var message = extractConfirmMessage(btn, rawOnclick);
                var isDanger = /xóa|hủy|ngừng|cấm|từ chối|bỏ theo dõi|rút yêu cầu|thu hồi|vĩnh viễn/i.test(message);

                window.showGlobalConfirmModal({
                    title: isDanger ? 'Xác Nhận Xóa / Hủy Bỏ' : 'Xác Nhận Thao Tác',
                    message: message,
                    isDanger: isDanger,
                    confirmText: isDanger ? 'Xác nhận xóa' : 'Đồng ý',
                    onConfirm: function () {
                        if (btn.tagName === 'A' && btn.href && !btn.href.startsWith('javascript:')) {
                            window.location.href = btn.href;
                        } else if (btn.form && (btn.type === 'submit' || !btn.type)) {
                            btn.form.__global_confirmed = true;
                            btn.form.submit();
                        } else {
                            var origOnclick = btn.getAttribute('onclick');
                            btn.removeAttribute('onclick');
                            btn.click();
                            btn.setAttribute('onclick', origOnclick);
                        }
                    }
                });
                return;
            }
        }
    }, true); // Bắt ở capture phase để chặn trước inline handler!

    // 4. Tự động chặn và chuyển đổi mọi onsubmit có confirm(...) thành Global Modal
    document.addEventListener('submit', function (e) {
        var form = e.target;
        if (!form) return;
        var rawOnsubmit = form.getAttribute('onsubmit');
        if (rawOnsubmit && rawOnsubmit.indexOf('confirm(') !== -1 && !form.__global_confirmed) {
            e.preventDefault();
            e.stopImmediatePropagation();

            var message = extractConfirmMessage(form, rawOnsubmit);
            var isDanger = /xóa|hủy|ngừng|cấm|từ chối|bỏ theo dõi|rút yêu cầu|thu hồi|vĩnh viễn/i.test(message);

            window.showGlobalConfirmModal({
                title: isDanger ? 'Xác Nhận Xóa / Hủy Bỏ' : 'Xác Nhận Thao Tác',
                message: message,
                isDanger: isDanger,
                confirmText: isDanger ? 'Xác nhận' : 'Đồng ý',
                onConfirm: function () {
                    form.__global_confirmed = true;
                    form.submit();
                }
            });
        }
    }, true);

    // 5. Ghi đè window.alert mặc định để không bao giờ hiện "localhost:8080 says"
    window.alert = function (message) {
        window.showGlobalAlertModal({
            title: 'Thông Báo',
            message: message,
            isError: false
        });
    };

    // 6. Ghi đè window.confirm mặc định phòng ngừa gọi trực tiếp từ script
    window.confirm = function (message) {
        window.showGlobalConfirmModal({
            message: message
        });
        return false;
    };
})();
