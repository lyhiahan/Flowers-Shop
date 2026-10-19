/**
 * Real-time Notifications & Bell Dropdown Manager for Hoa Tươi Sài Gòn
 * Handles:
 * 1. Unread badge count synchronization across Header, Sidebar, and Mobile Nav
 * 2. WebSocket (STOMP via SockJS) push notifications
 * 3. Bell dropdown with latest notifications & mark-as-read actions
 * 4. In-page live list updates on /notifications and /manager/notifications
 * 5. Floating Toast alerts for instant user feedback
 */
(function () {
    'use strict';

    let currentUserId = null;
    let currentUnreadCount = 0;
    let isManagerUser = false;
    let stompClient = null;
    let isConnecting = false;

    // Helper: CSRF Header
    function getCsrfHeaders() {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        const headers = { 'Content-Type': 'application/json' };
        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }
        return headers;
    }

    // Helper: Format relative time
    function formatTimeAgo(isoString) {
        if (!isoString) return '';
        try {
            const date = new Date(isoString);
            const now = new Date();
            const diffSec = Math.floor((now - date) / 1000);
            if (diffSec < 60) return 'Vừa xong';
            const diffMin = Math.floor(diffSec / 60);
            if (diffMin < 60) return diffMin + ' phút trước';
            const diffHour = Math.floor(diffMin / 60);
            if (diffHour < 24) return diffHour + ' giờ trước';
            const diffDay = Math.floor(diffHour / 24);
            if (diffDay < 7) return diffDay + ' ngày trước';
            return date.toLocaleDateString('vi-VN');
        } catch (e) {
            return '';
        }
    }

    // Helper: Map notification type to icon & colors
    function getNotifVisuals(type) {
        switch (type) {
            case 'ORDER_CREATED':
                return { icon: 'bi-bag-plus-fill', bg: '#ECFDF5', color: '#059669', label: 'Đơn mới' };
            case 'ORDER_CONFIRMED':
            case 'ORDER_DELIVERED':
                return { icon: 'bi-flower1', bg: '#E0F2FE', color: '#0284C7', label: 'Cập nhật đơn' };
            case 'ORDER_CANCELLED':
                return { icon: 'bi-x-circle-fill', bg: '#FEE2E2', color: '#DC2626', label: 'Đã hủy' };
            case 'WALLET_DEPOSIT':
            case 'WALLET_WITHDRAW':
                return { icon: 'bi-coin', bg: '#FEF3C7', color: '#D97706', label: 'Ví tiền' };
            case 'PAYMENT_SUCCESS':
                return { icon: 'bi-credit-card-fill', bg: '#FDF2F8', color: '#DB2777', label: 'Thanh toán' };
            default:
                return { icon: 'bi-bell-fill', bg: '#E5EEE8', color: '#20382E', label: 'Hệ thống' };
        }
    }

    // Update all badge elements across current page
    function updateBadges(count) {
        currentUnreadCount = Math.max(0, count || 0);
        const badgeSelectors = [
            '#headerNotifBadge',
            '#sidebarNotifBadge',
            '#mobileNotifBadge',
            '#floralNotifBadge',
            '#ordersNotifBadge',
            '.notif-badge-pill'
        ];

        badgeSelectors.forEach(sel => {
            document.querySelectorAll(sel).forEach(badge => {
                if (currentUnreadCount > 0) {
                    badge.textContent = currentUnreadCount > 99 ? '99+' : currentUnreadCount;
                    badge.style.display = badge.tagName === 'SPAN' ? 'inline-block' : 'block';
                } else {
                    badge.textContent = '0';
                    badge.style.display = 'none';
                }
            });
        });

        // In-page counter if on /notifications or /manager/notifications
        const pageCountEl = document.getElementById('unreadCountDisplay');
        if (pageCountEl) {
            pageCountEl.textContent = currentUnreadCount;
        }

        // Toggle page-level "Mark all read" button
        const pageMarkAll = document.getElementById('pageMarkAllReadBtn');
        if (pageMarkAll) {
            pageMarkAll.style.display = currentUnreadCount > 0 ? 'inline-flex' : 'none';
        }
    }

    // Fetch initial count & user info
    async function fetchUnreadCount() {
        try {
            const res = await fetch('/api/notifications/unread-count');
            if (!res.ok) return;
            const data = await res.json();
            currentUserId = data.userId;
            isManagerUser = Boolean(data.isManager);
            updateBadges(data.unreadCount);

            if (currentUserId && !stompClient && !isConnecting) {
                initWebSocket(currentUserId);
            }
        } catch (e) {
            console.debug('[Notif] fetchUnreadCount skipped:', e.message);
        }
    }

    // Connect WebSocket via SockJS & STOMP
    function initWebSocket(userId) {
        if (!window.SockJS || !window.Stomp) {
            setTimeout(() => {
                if (window.SockJS && window.Stomp) initWebSocket(userId);
            }, 1000);
            return;
        }

        try {
            isConnecting = true;
            const socket = new window.SockJS('/ws');
            stompClient = window.Stomp.over(socket);
            stompClient.debug = null; // Tắt log STOMP console trong môi trường chạy thật

            stompClient.connect({}, function () {
                isConnecting = false;
                const destination = '/topic/user/' + userId + '/notifications';
                stompClient.subscribe(destination, function (message) {
                    try {
                        const notif = JSON.parse(message.body);
                        handleIncomingNotification(notif);
                    } catch (err) {
                        console.warn('[Notif] Parse error:', err);
                    }
                });
            }, function (error) {
                isConnecting = false;
                stompClient = null;
                // Tự động kết nối lại sau 6 giây
                setTimeout(() => {
                    if (currentUserId && !stompClient) initWebSocket(currentUserId);
                }, 6000);
            });
        } catch (err) {
            isConnecting = false;
            stompClient = null;
        }
    }

    // Handle incoming real-time notification
    function handleIncomingNotification(notif) {
        if (!notif) return;
        updateBadges(currentUnreadCount + 1);

        // Hiển thị Toast thông báo nổi trên góc phải
        showFloatingToast(notif);

        // Nếu đang ở trang xem thông báo (/notifications hoặc /manager/notifications), chèn trực tiếp thẻ mới
        prependNotificationToPage(notif);
    }

    // Floating Toast Notification
    function showFloatingToast(notif) {
        let container = document.getElementById('notif-toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'notif-toast-container';
            container.style.cssText = 'position:fixed;top:24px;right:24px;z-index:99999;display:flex;flex-direction:column;gap:12px;max-width:380px;width:calc(100% - 48px);pointer-events:none;';
            document.body.appendChild(container);
        }

        const visuals = getNotifVisuals(notif.type);
        const toast = document.createElement('div');
        toast.className = 'notif-toast-item shadow-lg';
        toast.style.cssText = 'pointer-events:auto;background:#FFFFFF;border-radius:14px;border:1px solid #E5EEE8;border-left:5px solid ' + visuals.color + ';padding:14px 16px;box-shadow:0 12px 32px rgba(32,56,46,0.12);transition:all 0.3s ease;transform:translateX(50px);opacity:0;';

        let targetUrl = '/notifications';
        if (notif.relatedOrderId) {
            const isManagerPath = window.location.pathname.startsWith('/manager');
            targetUrl = isManagerPath || isManagerUser ? '/manager/orders/' + notif.relatedOrderId : '/customer/orders/detail/' + notif.relatedOrderId;
        }

        toast.innerHTML = `
            <div style="display:flex;align-items:flex-start;gap:12px;">
                <div style="width:36px;height:36px;border-radius:10px;background:${visuals.bg};color:${visuals.color};display:flex;align-items:center;justify-content:center;font-size:18px;flex-shrink:0;">
                    <i class="bi ${visuals.icon}"></i>
                </div>
                <div style="flex-grow:1;min-width:0;">
                    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:2px;">
                        <strong style="font-size:14px;color:#20382E;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(notif.title || 'Thông báo mới')}</strong>
                        <button type="button" class="btn-close notif-toast-close" style="font-size:10px;padding:2px;cursor:pointer;"></button>
                    </div>
                    <p style="margin:0 0 6px 0;font-size:12.5px;color:#686E65;line-height:1.4;">${escapeHtml(notif.content || '')}</p>
                    <a href="${targetUrl}" class="notif-toast-link" style="font-size:12px;font-weight:700;color:${visuals.color};text-decoration:none;">Xem chi tiết &rarr;</a>
                </div>
            </div>
        `;

        container.appendChild(toast);
        requestAnimationFrame(() => {
            toast.style.transform = 'translateX(0)';
            toast.style.opacity = '1';
        });

        // Close on button
        toast.querySelector('.notif-toast-close').addEventListener('click', (e) => {
            e.stopPropagation();
            removeToast(toast);
        });

        // Auto remove after 6s
        setTimeout(() => removeToast(toast), 6000);
    }

    function removeToast(toast) {
        if (!toast || !toast.parentNode) return;
        toast.style.transform = 'translateX(50px)';
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }

    // Prepend to current active list if user is on notifications page
    function prependNotificationToPage(notif) {
        const listEl = document.getElementById('notificationsList');
        if (!listEl) return;

        const emptyAlert = document.getElementById('emptyNotifAlert');
        if (emptyAlert) emptyAlert.classList.add('d-none');
        listEl.classList.remove('d-none');

        const visuals = getNotifVisuals(notif.type);
        const isManagerPage = Boolean(document.querySelector('.manager-layout'));
        const timeStr = formatTimeAgo(notif.createdAt) || 'Vừa xong';

        let targetUrl = '';
        if (notif.relatedOrderId) {
            targetUrl = isManagerPage ? '/manager/orders/' + notif.relatedOrderId : '/customer/orders/detail/' + notif.relatedOrderId;
        }

        const card = document.createElement('div');
        card.id = 'notif_' + notif.id;
        card.className = (isManagerPage ? 'notif-card-manager unread notif-fade-in' : 'notif-card unread notif-fade-in') + ' mb-3';

        card.innerHTML = `
            <div class="d-flex align-items-start gap-3">
                <div class="notif-icon-circle" style="background-color: ${visuals.bg}; color: ${visuals.color};">
                    <i class="bi ${visuals.icon}"></i>
                </div>
                <div class="flex-grow-1">
                    <div class="d-flex justify-content-between align-items-baseline mb-1">
                        <h6 class="fw-bold text-dark mb-0">${escapeHtml(notif.title || '')}</h6>
                        <small class="text-muted">${timeStr}</small>
                    </div>
                    <p class="text-secondary small mb-2">${escapeHtml(notif.content || '')}</p>
                    <div class="d-flex align-items-center gap-3">
                        ${targetUrl ? `<a href="${targetUrl}" onclick="window.FlowerShopNotif?.readNotification(${notif.id})" class="btn btn-sm ${isManagerPage ? 'btn-outline-success' : 'btn-outline-danger'} rounded-pill px-3 fw-semibold" style="font-size: 12.5px;">Xem chi tiết <i class="bi bi-arrow-right ms-1"></i></a>` : ''}
                        <button onclick="window.FlowerShopNotif?.markOneReadPage(this, ${notif.id})" class="btn btn-link btn-sm text-muted p-0 text-decoration-none" style="font-size: 12.5px;">
                            <i class="bi bi-check2"></i> Đánh dấu đã đọc
                        </button>
                    </div>
                </div>
            </div>
        `;

        listEl.prepend(card);
    }

    // Load & Render bell dropdown list
    async function loadDropdownList(container) {
        if (!container) return;
        container.innerHTML = '<div class="p-3 text-center text-muted small"><span class="spinner-border spinner-border-sm me-2" role="status"></span>Đang tải thông báo...</div>';

        try {
            const res = await fetch('/api/notifications/latest');
            if (!res.ok || res.redirected) throw new Error('Không thể tải thông báo');
            const list = await res.json();
            if (!Array.isArray(list)) throw new Error('Danh sách thông báo không hợp lệ');

            if (!list || list.length === 0) {
                container.innerHTML = `
                    <div class="p-4 text-center text-muted small">
                        <i class="bi bi-bell-slash fs-4 d-block mb-1 text-secondary opacity-50"></i>
                        Không có thông báo mới nào
                    </div>
                `;
                return;
            }

            container.innerHTML = '';
            list.forEach(item => {
                const visuals = getNotifVisuals(item.type);
                const timeStr = formatTimeAgo(item.createdAt);
                const isManagerPath = window.location.pathname.startsWith('/manager');
                let targetUrl = (isManagerPath || isManagerUser ? '/manager/notifications' : '/notifications') + '#notif_' + encodeURIComponent(item.id);
                if (item.relatedOrderId) {
                    targetUrl = isManagerPath || isManagerUser ? '/manager/orders/' + item.relatedOrderId : '/customer/orders/detail/' + item.relatedOrderId;
                }

                const row = document.createElement('a');
                row.href = targetUrl;
                row.className = 'd-flex align-items-start gap-2.5 p-3 text-decoration-none border-bottom notif-dropdown-item' + (!item.isRead ? ' bg-light bg-opacity-75' : '');
                row.style.cssText = 'transition: background 0.15s ease; border-color: #F1EFEA !important;';

                row.innerHTML = `
                    <div style="width:34px;height:34px;border-radius:10px;background:${visuals.bg};color:${visuals.color};display:flex;align-items:center;justify-content:center;font-size:16px;flex-shrink:0;">
                        <i class="bi ${visuals.icon}"></i>
                    </div>
                    <div class="flex-grow-1 min-w-0">
                        <div class="d-flex justify-content-between align-items-start gap-2">
                            <span class="fw-bold text-dark text-truncate small mb-0" style="max-width: 200px;">${escapeHtml(item.title || '')}</span>
                            ${!item.isRead ? '<span class="badge bg-danger rounded-circle p-1" style="width:7px;height:7px;"></span>' : ''}
                        </div>
                        <p class="text-secondary small mb-1 text-truncate" style="max-width: 240px; font-size: 12px; margin-top: 2px;">${escapeHtml(item.content || '')}</p>
                        <span class="text-muted" style="font-size: 11px;">${timeStr}</span>
                    </div>
                `;

                row.addEventListener('click', (e) => {
                    if (!item.isRead) {
                        readNotification(item.id).catch(err => console.debug(err));
                    }
                });

                container.appendChild(row);
            });
        } catch (e) {
            const pageUrl = window.location.pathname.startsWith('/manager') || isManagerUser ? '/manager/notifications' : '/notifications';
            container.innerHTML = '<div class="p-3 text-center text-danger small">Không thể tải thông báo lúc này. <a href="' + pageUrl + '" class="d-block mt-2">Mở trang thông báo</a></div>';
        }
    }

    // API: Mark single notification read
    async function readNotification(id) {
        if (!id) return;
        const res = await fetch('/api/notifications/read/' + id, {
            method: 'POST',
            headers: getCsrfHeaders(),
            keepalive: true
        });
        if (!res.ok) throw new Error('Không thể đánh dấu thông báo đã đọc.');
        updateBadges(currentUnreadCount - 1);
    }

    // API: Mark all notifications read
    async function readAllNotifications() {
        const res = await fetch('/api/notifications/read-all', {
            method: 'POST',
            headers: getCsrfHeaders()
        });
        if (!res.ok) throw new Error('Không thể đánh dấu tất cả thông báo đã đọc.');
        updateBadges(0);
    }

    // In-page mark single helper
    async function markOneReadPage(btn, id) {
        if (!btn || btn.disabled) return;
        btn.disabled = true;
        try {
            await readNotification(id);
            const card = document.getElementById('notif_' + id);
            if (card) {
                card.classList.remove('unread');
                btn.remove();
            }
        } catch (err) {
            console.error(err);
        } finally {
            btn.disabled = false;
        }
    }

    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    // Bind dropdown events
    function setupDropdowns() {
        document.querySelectorAll('[data-bs-toggle="dropdown"]').forEach(trigger => {
            if (trigger.id === 'notifBellDropdown' || trigger.id === 'floralNotifDropdown' || trigger.closest('.notif-dropdown-wrapper')) {
                trigger.addEventListener('show.bs.dropdown', function () {
                    const parent = trigger.closest('.dropdown');
                    if (parent) {
                        const listContainer = parent.querySelector('.notif-dropdown-list, #headerNotifList');
                        if (listContainer) loadDropdownList(listContainer);
                    }
                });
            }
        });

        // Mark all read buttons
        document.querySelectorAll('#headerMarkAllReadBtn, .notif-mark-all-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.preventDefault();
                e.stopPropagation();
                btn.disabled = true;
                try {
                    await readAllNotifications();
                    document.querySelectorAll('.notif-dropdown-item .badge.bg-danger').forEach(dot => dot.remove());
                    document.querySelectorAll('.notif-dropdown-item.bg-light').forEach(item => item.classList.remove('bg-light', 'bg-opacity-75'));
                } catch (error) {
                    console.error(error);
                } finally {
                    btn.disabled = false;
                }
            });
        });
    }

    // Global namespace for external page calls
    window.FlowerShopNotif = {
        fetchUnreadCount,
        readNotification,
        readAllNotifications,
        updateBadges,
        markOneReadPage
    };

    // Auto-init on DOMContentLoaded
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            fetchUnreadCount();
            setupDropdowns();
        });
    } else {
        fetchUnreadCount();
        setupDropdowns();
    }
})();
