(() => {
    'use strict';
    const toggle = document.getElementById('staffMenuToggle');
    const close = document.getElementById('staffMenuClose');
    const backdrop = document.getElementById('staffMenuBackdrop');
    function closeMenu(focus = true) {
        document.body.classList.remove('staff-menu-open');
        toggle?.setAttribute('aria-expanded', 'false');
        if (backdrop) backdrop.hidden = true;
        if (focus) toggle?.focus();
    }
    toggle?.addEventListener('click', () => {
        if (document.body.classList.contains('staff-menu-open')) return closeMenu();
        document.body.classList.add('staff-menu-open');
        toggle.setAttribute('aria-expanded', 'true');
        if (backdrop) backdrop.hidden = false;
        close?.focus();
    });
    close?.addEventListener('click', () => closeMenu());
    backdrop?.addEventListener('click', () => closeMenu());
    document.addEventListener('keydown', event => {
        if (event.key === 'Escape' && document.body.classList.contains('staff-menu-open')) closeMenu();
    });
    window.addEventListener('resize', () => { if (window.innerWidth >= 992) closeMenu(false); });
    const storeId = document.body.dataset.staffStoreId;
    const banner = document.getElementById('staffOrderUpdates');
    if (!storeId || !banner || typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return;
    const socket = new SockJS('/ws');
    const client = Stomp.over(socket);
    client.debug = null;
    client.connect({}, () => {
        client.subscribe('/topic/store/' + storeId + '/orders', () => { banner.hidden = false; });
    }, () => {});
    window.addEventListener('beforeunload', () => { if (client.connected) client.disconnect(); });
})();
