(function () {
    'use strict';
    const price = document.getElementById('productPrice');
    const promotion = document.getElementById('productPromotion');
    if (!price || !promotion) return;
    function validatePromotion() {
        const amount = Number(promotion.value);
        const regular = Number(price.value);
        promotion.setCustomValidity(promotion.value !== '' &&
            (!Number.isInteger(amount) || amount < 1 || amount > regular)
            ? 'Giá khuyến mãi phải là số đồng nguyên, từ 1đ và không lớn hơn giá bán. Để trống nếu không áp dụng.' : '');
    }
    price.addEventListener('input', validatePromotion);
    promotion.addEventListener('input', validatePromotion);
    validatePromotion();
})();
