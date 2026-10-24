(function () {
    'use strict';

    const form = document.getElementById('globalSearchForm');
    const scope = document.getElementById('searchScopeSelect');
    if (!form || !scope) return;

    const pathname = window.location.pathname;
    if (pathname === '/stores' || pathname.startsWith('/stores/')) scope.value = 'stores';
    else if (pathname === '/users' || pathname.startsWith('/users/')) scope.value = 'users';

    form.addEventListener('submit', function (event) {
        const input = form.querySelector('input[name="keyword"]');
        const keyword = input ? input.value.trim() : '';
        if (input) input.value = keyword;
        if (scope.value === 'users' || scope.value === 'stores') {
            event.preventDefault();
            window.location.href = '/' + scope.value + (keyword ? '?keyword=' + encodeURIComponent(keyword) : '');
        }
    });
})();
