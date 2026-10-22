/**
 * Admin Layout System JavaScript - Hoa Tươi Sài Gòn
 * Manages mobile drawer offcanvas, accessibility attributes, Escape key and backdrop dismissal.
 */
(function () {
    'use strict';

    function initAdminMobileDrawer() {
        // Chat page handles its own specialized mobile drawer inside chat-widget.js
        if (document.body.classList.contains('admin-chat-page')) {
            return;
        }

        const toggleBtn = document.getElementById('adminMenuToggleBtn');
        const closeBtn = document.getElementById('adminMenuCloseBtn');
        let backdrop = document.getElementById('adminDrawerBackdrop');

        if (!backdrop) {
            backdrop = document.createElement('div');
            backdrop.id = 'adminDrawerBackdrop';
            backdrop.className = 'admin-drawer-backdrop';
            document.body.appendChild(backdrop);
        }

        function openDrawer() {
            document.body.classList.add('admin-menu-open');
            toggleBtn?.setAttribute('aria-expanded', 'true');
            closeBtn?.focus();
        }

        function closeDrawer() {
            document.body.classList.remove('admin-menu-open');
            toggleBtn?.setAttribute('aria-expanded', 'false');
            toggleBtn?.focus();
        }

        toggleBtn?.addEventListener('click', (e) => {
            e.stopPropagation();
            if (document.body.classList.contains('admin-menu-open')) {
                closeDrawer();
            } else {
                openDrawer();
            }
        });

        closeBtn?.addEventListener('click', (e) => {
            e.stopPropagation();
            closeDrawer();
        });

        backdrop.addEventListener('click', closeDrawer);

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && document.body.classList.contains('admin-menu-open')) {
                closeDrawer();
            }
        });

        // Close drawer if window is resized above 991px
        window.addEventListener('resize', () => {
            if (window.innerWidth >= 992 && document.body.classList.contains('admin-menu-open')) {
                document.body.classList.remove('admin-menu-open');
                toggleBtn?.setAttribute('aria-expanded', 'false');
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAdminMobileDrawer);
    } else {
        initAdminMobileDrawer();
    }
})();
