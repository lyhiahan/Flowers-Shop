/**
 * Tự động việt hóa thông báo lỗi HTML5 Validation (tooltip trình duyệt)
 * Chuyển các thông báo mặc định của trình duyệt ("Please fill out this field", ...) 
 * sang Tiếng Việt chuẩn: "Vui lòng điền..."
 */
(function () {
    function getFieldFriendlyName(input) {
        // 1. Kiểm tra thuộc tính data-label hoặc data-name
        if (input.getAttribute('data-label')) return input.getAttribute('data-label');
        if (input.getAttribute('data-name')) return input.getAttribute('data-name');

        // 2. Tìm theo label liên kết qua id (for="...")
        if (input.id) {
            var labelFor = document.querySelector('label[for="' + input.id + '"]');
            if (labelFor && labelFor.innerText.trim()) {
                return cleanLabel(labelFor.innerText);
            }
        }

        // 3. Tìm label nằm cùng khối thẻ chứa (col, mb-3, form-group)
        var parentBlock = input.closest('.col-md-6, .col-md-4, .col-md-3, .col-12, .mb-3, .mb-4, .form-group, td');
        if (parentBlock) {
            var label = parentBlock.querySelector('label');
            if (label && label.innerText.trim()) {
                return cleanLabel(label.innerText);
            }
        }

        // 4. Nếu không có label, lấy từ placeholder
        if (input.placeholder && input.placeholder.trim()) {
            var ph = input.placeholder.trim();
            // Lược bỏ VD: hoặc ví dụ:
            ph = ph.replace(/^(VD|Ví dụ|Eg)\s*:\s*/i, '');
            ph = ph.split(/[,;(]/)[0].trim();
            if (ph.length > 0 && ph.length <= 40) {
                return ph;
            }
        }

        // 5. Thử suy đoán theo thuộc tính name
        if (input.name) {
            var map = {
                name: 'tên',
                fullName: 'họ và tên',
                username: 'tên đăng nhập',
                password: 'mật khẩu',
                email: 'email',
                phone: 'số điện thoại',
                address: 'địa chỉ',
                minPoint: 'doanh số tối thiểu',
                discount: 'mức chiết khấu / giảm giá',
                price: 'giá',
                priceBuy: 'giá bán',
                importPrice: 'giá nhập',
                quantity: 'số lượng',
                code: 'mã',
                title: 'tiêu đề',
                content: 'nội dung',
                description: 'mô tả',
                startDate: 'ngày bắt đầu',
                endDate: 'ngày kết thúc'
            };
            if (map[input.name]) return map[input.name];
        }

        return '';
    }

    function cleanLabel(text) {
        return text
            .replace(/[*:\(\)]/g, '')
            .replace(/\s+/g, ' ')
            .replace(/VNĐ|₫|%|\/kỳ/gi, '')
            .trim();
    }

    // Bắt sự kiện invalid ở giai đoạn capture
    document.addEventListener('invalid', function (e) {
        var input = e.target;
        if (!input || !input.validity) return;

        var name = getFieldFriendlyName(input);
        var lowerName = name ? name.toLowerCase() : '';

        if (input.validity.valueMissing) {
            if (input.tagName === 'SELECT') {
                input.setCustomValidity(lowerName ? 'Vui lòng chọn ' + lowerName + '!' : 'Vui lòng chọn một mục trong danh sách!');
            } else if (input.type === 'checkbox') {
                input.setCustomValidity('Vui lòng tích chọn để tiếp tục!');
            } else if (input.type === 'radio') {
                input.setCustomValidity('Vui lòng chọn một lựa chọn!');
            } else if (input.type === 'file') {
                input.setCustomValidity(lowerName ? 'Vui lòng chọn tệp ' + lowerName + '!' : 'Vui lòng chọn tệp tải lên!');
            } else {
                input.setCustomValidity(lowerName ? 'Vui lòng điền ' + lowerName + '!' : 'Vui lòng điền thông tin vào trường này!');
            }
        } else if (input.validity.typeMismatch) {
            if (input.type === 'email') {
                input.setCustomValidity('Vui lòng điền địa chỉ email hợp lệ (VD: tenban@domain.com)!');
            } else if (input.type === 'url') {
                input.setCustomValidity('Vui lòng điền đường dẫn URL hợp lệ (VD: https://...)!');
            } else {
                input.setCustomValidity('Vui lòng điền đúng định dạng dữ liệu!');
            }
        } else if (input.validity.rangeUnderflow) {
            input.setCustomValidity('Vui lòng điền giá trị lớn hơn hoặc bằng ' + (input.min || 0) + '!');
        } else if (input.validity.rangeOverflow) {
            input.setCustomValidity('Vui lòng điền giá trị nhỏ hơn hoặc bằng ' + input.max + '!');
        } else if (input.validity.stepMismatch) {
            input.setCustomValidity('Vui lòng điền giá trị theo bước nhảy hợp lệ!');
        } else if (input.validity.tooShort) {
            input.setCustomValidity('Vui lòng điền tối thiểu ' + input.minLength + ' ký tự!');
        } else if (input.validity.tooLong) {
            input.setCustomValidity('Vui lòng điền không quá ' + input.maxLength + ' ký tự!');
        } else if (input.validity.patternMismatch) {
            input.setCustomValidity(input.title ? input.title : 'Vui lòng điền dữ liệu đúng định dạng yêu cầu!');
        }
    }, true);

    // Xóa lỗi ngay khi người dùng gõ phím hoặc thay đổi giá trị
    document.addEventListener('input', function (e) {
        if (e.target && e.target.setCustomValidity) {
            e.target.setCustomValidity('');
        }
    }, true);

    document.addEventListener('change', function (e) {
        if (e.target && e.target.setCustomValidity) {
            e.target.setCustomValidity('');
        }
    }, true);
})();
