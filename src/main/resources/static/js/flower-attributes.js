(function (root) {
    'use strict';
    function parse(raw) {
        if (raw.length > 1500) throw new Error('Nội dung thuộc tính quá dài.');
        const result = [], seen = new Set();
        raw.split(/[,;\r\n]+/).forEach(function (part) {
            const value = part.normalize('NFC').trim().replace(/\s+/g, ' ');
            if (!value) return;
            if (value.length > 32 || /[\x00-\x1f\x7f-\x9f]/.test(value))
                throw new Error('Mỗi giá trị tối đa 32 ký tự và không chứa ký tự điều khiển.');
            const key = value.toLocaleLowerCase('vi');
            if (!seen.has(key)) { result.push(value); seen.add(key); }
        });
        if (result.length > 15) throw new Error('Mỗi nhóm tối đa 15 giá trị.');
        return result;
    }

    function enhance(source, document) {
        if (source.dataset.enhanced) return;
        let selected;
        try { selected = parse(source.value); } catch (_) { return; } // Keep editable fallback, never discard legacy text.
        source.dataset.enhanced = 'true';
        const editor = document.createElement('div'); editor.className = 'flower-tag-editor';
        const row = document.createElement('div'); row.className = 'flower-tag-row';
        const surface = document.createElement('div'); surface.className = 'flower-tag-surface';
        const tags = document.createElement('ul'); tags.className = 'flower-tags';
        tags.setAttribute('aria-label', source.dataset.label + ' đã chọn');
        const controls = document.createElement('div'); controls.className = 'flower-tag-controls';
        const input = document.createElement('input'); input.type = 'text'; input.className = 'flower-tag-input';
        input.placeholder = source.placeholder; input.maxLength = 1500;
        input.setAttribute('autocomplete', 'off');
        const fieldId = source.id;
        source.id = fieldId + '-source'; input.id = fieldId;
        input.setAttribute('aria-describedby', fieldId + '-error');
        const add = document.createElement('button'); add.type = 'button'; add.className = 'flower-tag-add';
        add.textContent = 'Thêm'; add.setAttribute('aria-label', 'Thêm ' + source.dataset.label.toLocaleLowerCase('vi'));
        const picker = document.createElement('button'); picker.type = 'button'; picker.className = 'flower-tag-picker';
        picker.textContent = 'Chọn có sẵn'; picker.setAttribute('aria-expanded', 'false');
        picker.setAttribute('aria-label', 'Chọn ' + source.dataset.label.toLocaleLowerCase('vi') + ' có sẵn');
        const choices = document.createElement('div'); choices.className = 'flower-tag-choices';
        choices.id = fieldId + '-choices'; choices.hidden = true;
        choices.setAttribute('role', 'group'); choices.setAttribute('aria-label', source.dataset.label + ' có sẵn');
        picker.setAttribute('aria-controls', choices.id);
        const datalist = document.getElementById ? document.getElementById(source.dataset.suggestions) : null;
        const suggestions = datalist ? Array.from(datalist.querySelectorAll('option')).map(option => option.value) : [];
        const error = document.createElement('div'); error.className = 'flower-tag-error'; error.id = fieldId + '-error';
        error.setAttribute('aria-live', 'polite');
        surface.append(tags, input); controls.append(picker, add); row.append(surface, controls);
        editor.append(row, choices, error); source.after(editor); source.hidden = true;

        function key(value) { return value.normalize('NFC').trim().toLocaleLowerCase('vi'); }
        function searchKey(value) { return key(value).normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd'); }
        function setOpen(open) {
            choices.hidden = !open; picker.setAttribute('aria-expanded', String(open));
            if (open) renderChoices();
        }
        function renderChoices(focusValue) {
            choices.replaceChildren();
            const filtered = suggestions.filter(value => searchKey(value).includes(searchKey(input.value)));
            if (!filtered.length) {
                const empty = document.createElement('span'); empty.className = 'flower-choice-empty';
                empty.textContent = suggestions.length ? 'Không có kết quả phù hợp.' : 'Chưa có giá trị có sẵn.';
                choices.append(empty);
            }
            filtered.forEach(function (value) {
                const choice = document.createElement('button'); choice.type = 'button'; choice.className = 'flower-tag-choice';
                choice.textContent = value;
                choice.setAttribute('aria-pressed', String(selected.some(item => key(item) === key(value))));
                choice.addEventListener('click', function () {
                    const index = selected.findIndex(item => key(item) === key(value));
                    try {
                        if (index >= 0) selected.splice(index, 1);
                        else selected = parse(selected.join('\n') + '\n' + value);
                        input.value = ''; message(''); render(); renderChoices(value);
                    } catch (failure) { message(failure.message); }
                });
                choices.append(choice);
                if (focusValue === value) choice.focus();
            });
        }

        function message(text) {
            error.textContent = text; input.setCustomValidity(text);
            input.setAttribute('aria-invalid', text ? 'true' : 'false');
        }
        function render() {
            tags.replaceChildren(); source.value = selected.join('\n');
            add.disabled = !input.value.trim();
            selected.forEach(function (value, index) {
                const item = document.createElement('li'); item.className = 'flower-tag';
                const label = document.createElement('span'); label.textContent = value;
                const remove = document.createElement('button'); remove.type = 'button'; remove.className = 'flower-tag-remove';
                remove.textContent = '×'; remove.setAttribute('aria-label', 'Bỏ ' + value);
                remove.addEventListener('click', function () { selected.splice(index, 1); message(''); render(); input.focus(); });
                item.append(label, remove); tags.append(item);
            });
            if (!choices.hidden) renderChoices();
        }
        function commit() {
            try {
                selected = parse(selected.join('\n') + '\n' + input.value);
                input.value = ''; message(''); render(); return true;
            } catch (failure) { message(failure.message); return false; }
        }
        input.addEventListener('keydown', function (event) {
            if (event.key === 'Enter' && !event.isComposing) { event.preventDefault(); commit(); }
        });
        input.addEventListener('input', function () {
            message(''); add.disabled = !input.value.trim();
            if (!choices.hidden) renderChoices();
        });
        input.addEventListener('blur', function (event) {
            if (event && event.relatedTarget && editor.contains(event.relatedTarget)) return;
            if (input.value.trim()) commit();
        });
        add.addEventListener('click', function () { commit(); input.focus(); });
        picker.addEventListener('click', function () { setOpen(choices.hidden); });
        editor.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && !choices.hidden) { event.preventDefault(); setOpen(false); picker.focus(); }
        });
        if (document.addEventListener) document.addEventListener('click', function (event) {
            if (!editor.contains(event.target)) setOpen(false);
        });
        if (source.form) source.form.addEventListener('submit', function (event) {
            if (!commit()) { event.preventDefault(); event.stopImmediatePropagation(); input.focus(); input.reportValidity(); }
        }, true);
        render();
    }
    if (typeof module !== 'undefined' && module.exports) module.exports = { parse: parse, enhance: enhance };
    if (root.document) root.document.querySelectorAll('.flower-attribute-source').forEach(function (source) {
        enhance(source, root.document);
    });
})(typeof window === 'undefined' ? globalThis : window);
