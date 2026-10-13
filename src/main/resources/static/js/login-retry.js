(function () {
    'use strict';
    const form = document.querySelector('[data-login-retry]');
    if (!form || form.dataset.loginFailed !== 'true') return;
    const username = document.getElementById('usernameInput');
    const password = document.getElementById('passwordInput');
    if (!username || !password) return;
    const attempted = username.dataset.attemptedUsername || '';
    let edited = false;

    // Stop restoring as soon as the person interacts, so retries never overwrite typing/demo selection.
    for (const event of ['pointerdown', 'keydown', 'beforeinput']) {
        document.addEventListener(event, () => { edited = true; }, { once: true, capture: true });
    }
    function restore() {
        if (edited) return;
        username.value = attempted;
        password.value = '';
    }
    restore();
    window.addEventListener('load', restore);
    window.addEventListener('pageshow', restore);
    // Autofill can run shortly after parsing. Reapply the submitted value only before user interaction.
    setTimeout(restore, 150);
    setTimeout(restore, 600);
}());
