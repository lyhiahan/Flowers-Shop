(() => {
    'use strict';
    const form = document.getElementById('profileForm');
    if (!form) return;
    const error = document.getElementById('profileImageError');
    const allowedTypes = new Set(['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif']);
    const previews = new Map();
    form.querySelectorAll('[data-profile-image-trigger]').forEach(button => {
        button.addEventListener('click', () => document.getElementById(button.dataset.profileImageTrigger)?.click());
    });
    form.querySelectorAll('input[data-profile-image]').forEach(input => {
        const preview = document.getElementById(input.dataset.profileImage);
        if (!preview) return;
        const original = preview.getAttribute('src');
        input.addEventListener('change', () => {
            const file = input.files[0];
            if (!file) return;
            const previous = previews.get(input);
            if (previous) URL.revokeObjectURL(previous);
            previews.delete(input);
            const invalid = !allowedTypes.has(file.type) ? 'Chỉ chấp nhận ảnh JPG, PNG, WEBP hoặc GIF.' :
                file.size > 10 * 1024 * 1024 ? 'Ảnh vượt quá dung lượng tối đa 10 MB.' : '';
            const filename = input.id === 'avatarFileInput' ? document.getElementById('avatarFileName') : null;
            if (filename) filename.textContent = invalid ? '' : file.name;
            error.hidden = !invalid;
            error.textContent = invalid;
            if (invalid) {
                input.value = '';
                preview.src = original;
                return;
            }
            const url = URL.createObjectURL(file);
            previews.set(input, url);
            preview.src = url;
        });
    });
    window.addEventListener('pagehide', () => {
        previews.forEach(url => URL.revokeObjectURL(url));
        previews.clear();
    });
})();
