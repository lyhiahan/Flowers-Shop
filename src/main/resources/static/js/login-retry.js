(function () {
    'use strict';
    const form = document.querySelector('[data-login-retry]');
    if (!form) return;
    const username = document.getElementById('usernameInput');
    const password = document.getElementById('passwordInput');
    if (!username || !password) return;
    const attempted = form.dataset.loginFailed === 'true'
        ? (username.dataset.attemptedUsername || '') : '';
    let edited = false;

    // Stop clearing as soon as the person interacts, including choosing a demo account.
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
    // Browsers may autofill shortly after parsing despite autocomplete="off".
    setTimeout(restore, 150);
    setTimeout(restore, 600);
}());
