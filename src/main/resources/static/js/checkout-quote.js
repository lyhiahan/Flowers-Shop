(function () {
    const form = document.getElementById('checkoutForm');
    const button = document.getElementById('btnPlaceOrder');
    const status = document.getElementById('quoteStatus');
    let timer, request, sequence = 0, ready = false;
    const money = value => new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
    window.updateCheckoutTotal = function () {
        clearTimeout(timer);
        const current = ++sequence;
        ready = false;
        window.updateWalletAvailability?.(null, false);
        const isPickup = !!form.querySelector('#methodPickup')?.checked;
        const shippingFeeEl = document.getElementById('displayShippingFee');
        if (shippingFeeEl) {
            if (isPickup) {
                shippingFeeEl.textContent = '0 ₫';
                shippingFeeEl.classList.remove('text-primary');
            } else if (!shippingFeeEl.textContent || shippingFeeEl.textContent.trim() === 'Chưa xác định') {
                const zone = form.querySelector('#deliveryZoneSelect')?.value;
                shippingFeeEl.textContent = (zone === 'FAR' ? '40.000 ₫' : '25.000 ₫');
            }
        }
        request?.abort();
        timer = setTimeout(async () => {
            request = new AbortController();
            try {
                const csrf = form.querySelector?.('input[name="_csrf"]')?.value;
                const headers = { 'X-Requested-With': 'XMLHttpRequest' };
                if (csrf) headers['X-CSRF-TOKEN'] = csrf;
                const response = await fetch(form.dataset.quoteUrl, {
                    method: 'POST', body: new FormData(form), signal: request.signal,
                    headers
                });
                if (response.redirected) throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
                let result;
                try {
                    result = await response.json();
                } catch (_) {
                    result = { message: 'Lỗi máy chủ khi tính tổng tiền. Vui lòng thử lại sau.' };
                }
                if (!response.ok) throw new Error(result.message || 'Không tính được số tiền. Vui lòng thử lại.');
                if (current !== sequence) return;
                document.getElementById('displaySubtotal').textContent = money(result.subtotal);
                if (shippingFeeEl) {
                    if (isPickup) {
                        shippingFeeEl.textContent = '0 ₫';
                        shippingFeeEl.classList.remove('text-primary');
                    } else {
                        shippingFeeEl.textContent = money(result.shippingFee);
                    }
                }
                document.getElementById('displayDiscount').textContent = '-' + money(result.discount);
                document.getElementById('displayGrandTotal').textContent = money(result.total);
                const deliveryName = document.getElementById('assignedDeliveryName');
                if (deliveryName && form.querySelector('#methodDelivery')?.checked) {
                    deliveryName.textContent = result.deliveryUnitName || 'Chưa cấu hình đơn vị vận chuyển cho vùng này.';
                }
                status.textContent = '';
                ready = true;
                button.disabled = button.dataset.pickupStockBlocked === 'true';
                window.updateWalletAvailability?.(result.total, true);
            } catch (error) {
                if (error.name === 'AbortError' || current !== sequence) return;
                status.textContent = error.message;
                const isPickupNow = !!form.querySelector('#methodPickup')?.checked;
                if (isPickupNow) {
                    if (shippingFeeEl) {
                        shippingFeeEl.textContent = '0 ₫';
                        shippingFeeEl.classList.remove('text-primary');
                    }
                    const grandTotalEl = document.getElementById('displayGrandTotal');
                    const baseTotal = grandTotalEl?.dataset?.baseTotal;
                    if (baseTotal && Number(baseTotal) > 0) {
                        grandTotalEl.textContent = money(Number(baseTotal));
                    }
                } else {
                    document.getElementById('displayGrandTotal').textContent = 'Chưa xác định';
                    if (shippingFeeEl) {
                        shippingFeeEl.textContent = 'Chưa xác định';
                    }
                }
                const deliveryName = document.getElementById('assignedDeliveryName');
                if (deliveryName && form.querySelector('#methodDelivery')?.checked) {
                    deliveryName.textContent = 'Chưa chọn được đơn vị vận chuyển phù hợp.';
                }
                button.disabled = button.dataset.pickupStockBlocked === 'true';
                window.updateWalletAvailability?.(null, false);
            }
        }, 50);
    };
    form.addEventListener('input', event => {
        if (['voucherCode', 'recipientAddress'].includes(event.target.name)) {
            if (event.target.name === 'recipientAddress' && typeof window.autoDetectDeliveryZone === 'function') {
                window.autoDetectDeliveryZone();
            }
            updateCheckoutTotal();
        }
    });
    form.addEventListener('change', event => {
        if (['shippingMethod', 'pickupStoreId', 'deliveryZone', 'voucherCode'].includes(event.target.name)) {
            if (event.target.name === 'shippingMethod' && typeof window.autoDetectDeliveryZone === 'function') {
                window.autoDetectDeliveryZone();
            }
            updateCheckoutTotal();
        }
    });
    form.addEventListener('submit', event => {
        if (event.defaultPrevented) return;
        if (!ready) {
            event.preventDefault();
            const message = status.textContent && status.textContent !== 'Đang cập nhật số tiền…'
                ? status.textContent : 'Vui lòng chờ hệ thống xác nhận phí giao hàng rồi thử lại.';
            if (typeof window.showGlobalAlertModal === 'function') {
                window.showGlobalAlertModal({ title: 'Chưa thể đặt hàng', message, isError: true });
            } else {
                const error = document.getElementById('checkoutValidationError');
                if (error) { error.textContent = message; error.classList.remove('d-none'); }
            }
            updateCheckoutTotal();
            return;
        }
        if (typeof window.syncPaymentSubmitState === 'function') {
            window.syncPaymentSubmitState();
            if (button.disabled) { event.preventDefault(); return; }
        }
        button.disabled = true;
        button.textContent = 'Đang đặt hàng…';
    });
    document.addEventListener('DOMContentLoaded', () => {
        toggleShippingMethod();
    });
})();
