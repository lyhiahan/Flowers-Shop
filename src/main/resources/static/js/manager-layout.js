// Accessible mobile drawer shared by every manager page.
(() => {
    const toggle = document.getElementById('managerMenuToggle');
    const sidebar = document.getElementById('managerSidebar');
    const overlay = document.getElementById('managerMenuOverlay');
    const close = document.getElementById('managerMenuClose');
    const main = document.getElementById('managerMain');
    if (!toggle || !sidebar || !overlay || !close) return;
    const mobile = window.matchMedia('(max-width: 991.98px)');
    function setOpen(open, restoreFocus = true) {
        document.body.classList.toggle('manager-menu-open', open);
        toggle.setAttribute('aria-expanded', String(open));
        overlay.hidden = !open;
        if (main) main.inert = open;
        if (open) close.focus();
        else if (restoreFocus) toggle.focus();
    }
    toggle.addEventListener('click', () => setOpen(toggle.getAttribute('aria-expanded') !== 'true'));
    close.addEventListener('click', () => setOpen(false));
    overlay.addEventListener('click', () => setOpen(false));
    sidebar.addEventListener('click', event => {
        if (mobile.matches && event.target.closest('a, [data-bs-toggle="modal"]')) setOpen(false);
    });
    document.addEventListener('keydown', event => {
        if (toggle.getAttribute('aria-expanded') !== 'true') return;
        if (event.key === 'Escape') { event.preventDefault(); setOpen(false); }
        if (event.key === 'Tab') {
            const controls = Array.from(sidebar.querySelectorAll('a[href], button:not([disabled])'));
            const first = controls[0], last = controls[controls.length - 1];
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
            else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
        }
    });
    mobile.addEventListener('change', () => setOpen(false, false));
})();
