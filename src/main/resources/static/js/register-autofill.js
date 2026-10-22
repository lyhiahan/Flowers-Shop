(() => {
    const form = document.getElementById('registerForm');
    if (!form || form.dataset.freshRegistration !== 'true') return;

    const credentials = [
        document.getElementById('usernameInput'),
        document.getElementById('passwordInput')
    ].filter(Boolean);
    let userInteracted = false;

    // Password managers may fill fields after DOMContentLoaded. Never clear a
    // field once the visitor has started using the form.
    for (const eventName of ['focusin', 'pointerdown', 'keydown', 'beforeinput']) {
        form.addEventListener(eventName, () => { userInteracted = true; }, {once: true});
    }

    function clearSavedCredentials() {
        if (userInteracted) return;
        for (const input of credentials) {
            if (document.activeElement !== input) input.value = '';
        }
    }

    clearSavedCredentials();
    document.addEventListener('DOMContentLoaded', clearSavedCredentials, {once: true});
    window.addEventListener('load', () => {
        clearSavedCredentials();
        for (const delay of [50, 200, 500, 1200]) {
            setTimeout(clearSavedCredentials, delay);
        }
    }, {once: true});
    window.addEventListener('pageshow', event => {
        if (!event.persisted) clearSavedCredentials();
    });
})();
