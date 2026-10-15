(() => {
    'use strict';
    const root = document.getElementById('flowerChat');
    if (!root) return;
    const el = id => document.getElementById(id);
    const panel = el('chatPanel'), text = el('chatText'), image = el('chatImage');
    let context, current, socket, connected = false, busy = false, buffer = '', retry = 0;
    let reconnectTimer, heartbeat, pending, connectTimer, stopped = false, inboxPage = 0, unseen = 0, historyVersion = 0;
    const conversations = new Map();
    let openingContact = false;
    let questionProduct = null;
    let mailbox = null, inboxVersion = 0;
    let filtersOpen = false;
    const channelButtons = [...document.querySelectorAll('[data-chat-channel]')];
    function monitorMode() { return context?.role === 'ADMIN' && (mailbox === 'STORE' || current?.kind === 'STORE'); }
    function scoped(path) {
        return context?.role === 'MANAGER' ? path + (path.includes('?') ? '&' : '?') + 'storeId=' + context.storeId : path;
    }
    function visible(conversation) {
        if (context?.role === 'ADMIN') return conversation.kind === mailbox;
        return context?.role !== 'MANAGER' || (conversation.kind === mailbox && Number(conversation.storeId) === Number(context.storeId)
            && (conversation.kind !== 'STORE' || Number(conversation.customerId) !== Number(context.userId)));
    }
    function status(message, reconnect = false) { el('chatStatus').textContent = message; el('chatStatus').title = message; el('chatReconnect').hidden = !(reconnect || stopped); }
    async function api(path, options = {}) {
        const headers = {Accept: 'application/json', 'X-Requested-With': 'XMLHttpRequest', ...(options.headers || {})};
        if (options.method && options.method !== 'GET') headers[root.dataset.csrfHeader] = root.dataset.csrfToken;
        const response = await fetch('/api/chat' + path, {...options, headers, credentials: 'same-origin'});
        if (response.redirected || response.status === 401) {
            stopConnection('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
            throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
        }
        let data;
        try { data = await response.json(); } catch (_) {
            if (response.status === 403) stopConnection('Phiên chat không còn quyền truy cập. Vui lòng đăng nhập lại.');
            throw new Error(response.status === 403 ? 'Phiên chat không còn quyền truy cập. Vui lòng đăng nhập lại.' : 'Không thể tải chat. Vui lòng thử lại.');
        }
        if (!response.ok) {
            const message = data.error || data.message || 'Không thể thực hiện thao tác này.';
            if (response.status === 403) stopConnection(message);
            throw new Error(message);
        }
        return data;
    }
    function controls() {
        const disabled = !current || !connected || busy || openingContact || monitorMode();
        text.disabled = image.disabled = el('chatSend').disabled = disabled;
        el('chatRemoveImage').disabled = busy;
        el('chatRemoveProduct').disabled = busy || openingContact;
        el('chatSendForm').hidden = monitorMode();
        el('chatMonitorNotice').hidden = !monitorMode();
    }
    function title(conversation) {
        if (context.role === 'ADMIN' && conversation.kind === 'ADMIN') return 'CSKH · ' + (conversation.customerName || 'Khách #' + conversation.customerId);
        let value = conversation.kind === 'ADMIN' ? 'Trò chuyện với hệ thống · CSKH toàn chuỗi'
            : conversation.kind === 'BRANCH_SUPPORT' ? 'Hỗ trợ nội bộ · ' + conversation.storeName : conversation.storeName;
        if (conversation.kind !== 'BRANCH_SUPPORT' && context.role !== 'USER' && conversation.customerId !== context.userId) value += ' · ' + conversation.customerName;
        return value;
    }
    function inboxTitle(conversation) {
        if (context.role !== 'USER') return conversation.kind === 'BRANCH_SUPPORT' ? conversation.storeName
            : conversation.customerName || 'Khách #' + conversation.customerId;
        return conversation.kind === 'ADMIN' ? 'Trò chuyện với hệ thống' : conversation.storeName;
    }
    function filterValues() {
        return {storeId: mailbox === 'ADMIN' ? '' : el('chatFilterStore').value.trim(),
            keyword: el('chatFilterKeyword').value.trim(), from: el('chatFilterFrom').value, to: el('chatFilterTo').value};
    }
    function updateFilters() {
        const admin = context?.role === 'ADMIN';
        el('chatFilterToggle').hidden = !admin;
        el('chatFilters').hidden = !admin || !filtersOpen;
        el('chatFilterStoreField').hidden = mailbox === 'ADMIN';
        el('chatFilterToggle').setAttribute('aria-expanded', String(admin && filtersOpen));
        el('chatFilterBadge').hidden = !admin || !Object.values(filterValues()).some(Boolean);
    }
    function timestamp(value) {
        if (!Array.isArray(value)) return typeof value === 'string' ? value : '';
        const [year, month, day, hour = 0, minute = 0, second = 0, nano = 0] = value;
        return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}T${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}.${String(nano).padStart(9, '0')}`;
    }
    function upsert(conversation) {
        if (!visible(conversation)) return;
        conversation = {...conversation, updatedAt: timestamp(conversation.updatedAt)};
        const previous = conversations.get(conversation.id);
        if (!previous || previous.updatedAt <= conversation.updatedAt) conversations.set(conversation.id, conversation);
    }
    function renderInbox() {
        el('chatConversations').replaceChildren();
        [...conversations.values()].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt) || b.id - a.id).forEach(conversation => {
            const button = document.createElement('button'); button.type = 'button';
            button.setAttribute('aria-current', String(current?.id === conversation.id));
            button.title = title(conversation);
            const strong = document.createElement('strong'); strong.textContent = inboxTitle(conversation);
            const preview = document.createElement('small'); preview.textContent = conversation.lastMessage || 'Chưa có tin nhắn';
            if (context.role === 'ADMIN' && conversation.kind === 'STORE') {
                const branch = document.createElement('small'); branch.className = 'chat-conversation-scope'; branch.textContent = conversation.storeName;
                button.append(strong, branch, preview);
            } else { button.append(strong, preview); }
            button.addEventListener('click', () => select(conversation));
            el('chatConversations').append(button);
        });
    }
    async function loadInbox(page = 0) {
        const version = inboxVersion;
        const params = new URLSearchParams({page: String(page)});
        if (mailbox) params.set('kind', mailbox);
        if (context?.role === 'ADMIN') {
            Object.entries(filterValues()).forEach(([key, value]) => { if (value) params.set(key, value); });
        }
        const data = await api(scoped('/conversations?' + params));
        if (version !== inboxVersion) return;
        if (page === 0) conversations.clear();
        data.conversations.forEach(upsert); inboxPage = page;
        el('chatMoreConversations').hidden = !data.hasMore; renderInbox();
    }
    function productCard(product) {
        if (!product || !Number.isSafeInteger(Number(product.id)) || Number(product.id) <= 0) return null;
        const card = document.createElement('a'); card.className = 'chat-product-card';
        card.href = '/products/' + Number(product.id); card.target = '_blank'; card.rel = 'noopener noreferrer';
        const url = product.imageUrl;
        if (typeof url === 'string' && (url.startsWith('https://') || /^\/(images|upload)\//.test(url))) {
            const thumbnail = document.createElement('img'); thumbnail.src = url; thumbnail.alt = product.name || 'Sản phẩm'; card.append(thumbnail);
        }
        const description = document.createElement('span');
        const name = document.createElement('strong'); name.textContent = product.name || 'Sản phẩm #' + product.id;
        const price = document.createElement('small');
        price.textContent = Number.isFinite(Number(product.price)) ? Number(product.price).toLocaleString('vi-VN') + ' đ' : '';
        description.append(name, price); card.append(description); return card;
    }
    function setQuestionProduct(product) {
        questionProduct = product || null;
        const holder = el('chatQuestionProductCard'); holder.replaceChildren();
        const card = productCard(questionProduct); if (card) holder.append(card);
        el('chatQuestionProduct').hidden = !card;
    }
    function bubble(message) {
        const log = el('chatMessages');
        if (log.querySelector('[data-message-id="' + message.id + '"]')) return;
        const item = document.createElement('div'); item.className = 'chat-message'; item.dataset.messageId = message.id;
        if (monitorMode() && current?.kind === 'STORE') {
            const customerMessage = Number(message.senderId) === Number(current.customerId);
            item.classList.add(customerMessage ? 'customer-side' : 'branch-side');
            if (customerMessage) item.classList.add('mine');
        } else if (message.senderId === context.userId) item.classList.add('mine');
        const caption = document.createElement('small'); caption.textContent = message.senderName + ' · ' + new Date(timestamp(message.createdAt)).toLocaleString('vi-VN'); item.append(caption);
        const product = productCard(message.product); if (product) item.append(product);
        if (message.content) { const body = document.createElement('p'); body.textContent = message.content; item.append(body); }
        if (message.imageUrl && message.imageUrl.startsWith('https://')) {
            const link = document.createElement('a'); link.href = message.imageUrl; link.target = '_blank'; link.rel = 'noopener noreferrer';
            const img = document.createElement('img'); img.src = message.imageUrl; img.alt = 'Ảnh trong cuộc trò chuyện'; img.loading = 'lazy'; link.append(img); item.append(link);
        }
        const next = [...log.children].find(child => Number(child.dataset.messageId) > message.id);
        log.insertBefore(item, next || null);
    }
    async function history(before) {
        if (!current) return;
        const id = current.id, version = historyVersion;
        const data = await api(scoped('/conversations/' + id + '/messages' + (before ? '?before=' + before : '')));
        if (version !== historyVersion || current?.id !== id) return;
        const log = el('chatMessages'), oldHeight = log.scrollHeight;
        data.messages.forEach(bubble); el('chatOlder').hidden = !data.hasMore;
        if (before) log.scrollTop += log.scrollHeight - oldHeight;
        else log.scrollTop = log.scrollHeight;
    }
    async function select(conversation) {
        if (!visible(conversation)) { status('Hội thoại không thuộc chi nhánh đang chọn.'); return; }
        if (busy) { status('Vui lòng đợi gửi xong tin nhắn hiện tại.'); return; }
        current = conversation; historyVersion++; pending = null;
        setQuestionProduct(null);
        text.value = ''; image.value = ''; fileName();
        el('chatCurrentTitle').textContent = title(conversation); el('chatMessages').replaceChildren();
        el('chatOlder').hidden = true; controls(); renderInbox();
        try { await history(); } catch (error) { status(error.message); }
    }
    function resizeComposer() {
        text.style.height = 'auto';
        text.style.height = Math.max(42, Math.min(text.scrollHeight, 112)) + 'px';
        text.style.overflowY = text.scrollHeight > 112 ? 'auto' : 'hidden';
    }
    function fileName() {
        const file = image.files[0]; el('chatFileName').textContent = file?.name || ''; el('chatRemoveImage').hidden = !file;
        el('chatAttachmentBar').hidden = !file; resizeComposer();
    }
    function ack(message) {
        if (pending && String(message.senderId) === String(context.userId) && message.clientMessageId === pending.id) {
            clearTimeout(pending.timer); pending = null; busy = false; text.value = ''; image.value = ''; fileName(); controls();
            setQuestionProduct(null);
            status('Đã gửi tin nhắn.');
        }
    }
    function received(event) {
        if (event.type !== 'MESSAGE' || !event.message || !event.conversation || !visible(event.conversation)) return;
        ack(event.message);
        // A live event may belong to the right mailbox but not the active filters.
        // Ask the backend to reapply branch/name/date filters instead of inserting
        // an unfiltered event into the visible inbox (username isn't in the event).
        const filtered = context.role === 'ADMIN' && Object.values(filterValues()).some(Boolean);
        if (filtered) loadInbox().catch(error => status(error.message));
        else { upsert(event.conversation); renderInbox(); }
        if (event.message.conversationId === current?.id) { bubble(event.message); el('chatMessages').scrollTop = el('chatMessages').scrollHeight; }
        if (panel.hidden && event.message.senderId !== context.userId) { unseen++; el('chatBadge').textContent = unseen; el('chatBadge').hidden = false; }
    }
    function frame(command, headers = {}, body = '') {
        if (!socket || socket.readyState !== WebSocket.OPEN) throw new Error('Chat chưa kết nối. Vui lòng thử lại.');
        socket.send(command + '\n' + Object.entries(headers).map(([key, value]) => key + ':' + value).join('\n') + '\n\n' + body + '\0');
    }
    function stopConnection(message) {
        stopped = true; connected = false; busy = false;
        clearTimeout(reconnectTimer); clearTimeout(connectTimer); clearInterval(heartbeat);
        if (pending) clearTimeout(pending.timer);
        const previous = socket; socket = null; previous?.close(); controls(); status(message, true);
    }
    function connect() {
        if (stopped) return;
        if (socket && [WebSocket.OPEN, WebSocket.CONNECTING].includes(socket.readyState)) return;
        clearTimeout(reconnectTimer); connected = false; controls(); buffer = ''; status('Đang kết nối chat…');
        const connection = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws-chat', ['v12.stomp']);
        socket = connection;
        let failure;
        connectTimer = setTimeout(() => {
            if (socket !== connection || connected) return;
            failure = 'Chat không phản hồi sau 10 giây. Vui lòng kết nối lại.';
            connection.close();
        }, 10000);
        connection.onopen = () => {
            if (socket === connection) frame('CONNECT', {'accept-version': '1.2', host: location.host, 'heart-beat': '20000,0',
                ...(context.role === 'MANAGER' ? {'chat-store-id': String(context.storeId)} : {})});
        };
        connection.onmessage = event => {
            if (socket !== connection) return;
            buffer += event.data;
            if (buffer.length > 131072) { socket.close(); status('Dữ liệu chat không hợp lệ.'); return; }
            let end;
            while ((end = buffer.indexOf('\0')) >= 0) {
                const raw = buffer.slice(0, end).replace(/^\s+/, ''); buffer = buffer.slice(end + 1);
                const split = raw.indexOf('\n\n'); if (split < 0) continue;
                const lines = raw.slice(0, split).split('\n'), command = lines.shift(), headers = {};
                lines.forEach(line => { const colon = line.indexOf(':'); if (colon > 0) headers[line.slice(0, colon)] = line.slice(colon + 1); });
                if (command === 'CONNECTED') {
                    clearTimeout(connectTimer);
                    connected = true; retry = 0; frame('SUBSCRIBE', {id: 'chat-messages', destination: context.role === 'MANAGER'
                        ? '/user/queue/chat.store.' + context.storeId : '/user/queue/chat', ack: 'auto'});
                    frame('SUBSCRIBE', {id: 'chat-errors', destination: '/user/queue/chat.errors', ack: 'auto'});
                    frame('SUBSCRIBE', {id: 'chat-acks', destination: '/user/queue/chat.acks', ack: 'auto'});
                    if (context.role === 'MANAGER') frame('SUBSCRIBE', {id: 'chat-support', destination: '/user/queue/chat.support.' + context.storeId, ack: 'auto'});
                    clearInterval(heartbeat); heartbeat = setInterval(() => { if (socket.readyState === WebSocket.OPEN) socket.send('\n'); }, 20000);
                    status('Chat đã kết nối.'); controls(); loadInbox().catch(error => status(error.message));
                    if (current) history().catch(error => status(error.message));
                } else if (command === 'MESSAGE') {
                    try {
                        const data = JSON.parse(raw.slice(split + 2));
                        if (headers.subscription === 'chat-acks') {
                            ack(data);
                            if (data.conversationId === current?.id) { bubble(data); el('chatMessages').scrollTop = el('chatMessages').scrollHeight; }
                        }
                        else if (headers.subscription === 'chat-errors') { if (pending) clearTimeout(pending.timer); busy = false; controls(); status(data.error || 'Không thể gửi tin nhắn.'); }
                        else received(data);
                    } catch (_) { status('Không thể đọc tin nhắn nhận được.'); }
                } else if (command === 'ERROR') {
                    stopConnection('Server từ chối phiên chat. Vui lòng đăng nhập lại hoặc kiểm tra quyền tài khoản.');
                    return;
                }
            }
        };
        connection.onclose = () => {
            if (socket !== connection) return;
            socket = null; clearTimeout(connectTimer);
            connected = false; busy = false; clearInterval(heartbeat); if (pending) clearTimeout(pending.timer); controls();
            status(failure || 'Chat mất kết nối. Tin đã lưu vẫn được giữ lại.', true);
            if (!stopped && retry < 6) reconnectTimer = setTimeout(async () => {
                try { await api('/context'); connect(); } catch (error) { status(error.message, true); }
            }, Math.min(1000 * 2 ** retry++, 15000));
        };
        connection.onerror = () => {
            if (socket !== connection) return;
            failure = 'Chưa kết nối được chat. Vui lòng kiểm tra kết nối hoặc phiên đăng nhập.';
            status(failure, true);
        };
    }
    async function ensureContext() {
            if (!context) {
                context = await api('/context');
                mailbox = context.role === 'ADMIN' ? 'ADMIN' : context.role === 'MANAGER' ? 'STORE' : null;
                el('chatBranchSupportButton').hidden = context.role !== 'MANAGER';
                el('chatChannelTabs').hidden = context.role === 'USER';
                el('chatToolbar').hidden = context.role === 'USER';
                updateFilters();
                el('chatScope').hidden = context.role === 'ADMIN';
                channelButtons.forEach(button => {
                    button.hidden = context.role === 'USER' || (context.role === 'MANAGER' && button.dataset.chatChannel === 'ADMIN');
                    if (context.role === 'ADMIN' && button.dataset.chatChannel === 'STORE') button.textContent = 'Giám sát chỉ đọc';
                    if (context.role === 'MANAGER' && button.dataset.chatChannel === 'BRANCH_SUPPORT') button.textContent = 'Hỗ trợ từ admin';
                    button.setAttribute('aria-pressed', String(button.dataset.chatChannel === mailbox));
                });
                el('chatHeading').textContent = context.role === 'MANAGER' ? 'Hộp thư khách hàng · ' + context.storeName : 'Tin nhắn';
                el('chatScope').textContent = context.role === 'MANAGER' ? 'Chỉ nhận tin của ' + context.storeName
                    : context.role === 'ADMIN' ? 'Hai kênh hỗ trợ được trả lời; giám sát khách–chi nhánh chỉ đọc và có nhật ký.'
                    : 'Đây là tất cả lịch sử trò chuyện của bạn. Hỗ trợ toàn chuỗi: Tài khoản → Trò chuyện với hệ thống. Hỏi sản phẩm: bấm Nhắn tin với cửa hàng trên chi tiết sản phẩm. Admin có thể giám sát chỉ đọc chat chi nhánh.';
            }
    }
    async function openPanel() {
        panel.hidden = false; el('chatLauncher').setAttribute('aria-expanded', 'true'); unseen = 0; el('chatBadge').hidden = true;
        try {
            await ensureContext(); await loadInbox(); connect();
            resizeComposer();
        } catch (error) { status(error.message, true); }
    }
    const launcherEl = el('chatLauncher');
    let hasDragged = false;
    if (launcherEl && typeof window !== 'undefined' && typeof launcherEl.getBoundingClientRect === 'function') {
        let isDown = false, startX = 0, startY = 0, origX = 0, origY = 0;
        try {
            const saved = window.sessionStorage?.getItem('flowerChat_pos');
            if (saved) {
                const pos = JSON.parse(saved);
                if (pos && typeof pos.top === 'number' && typeof pos.left === 'number') {
                    const maxLeft = Math.max(10, (window.innerWidth || 800) - 120);
                    const maxTop = Math.max(10, (window.innerHeight || 600) - 50);
                    launcherEl.style.left = Math.min(Math.max(10, pos.left), maxLeft) + 'px';
                    launcherEl.style.top = Math.min(Math.max(10, pos.top), maxTop) + 'px';
                    launcherEl.style.right = 'auto';
                    launcherEl.style.bottom = 'auto';
                }
            }
        } catch (_) {}

        const onPointerDown = e => {
            if (e.button && e.button !== 0) return;
            isDown = true;
            hasDragged = false;
            const clientX = e.touches ? e.touches[0].clientX : e.clientX;
            const clientY = e.touches ? e.touches[0].clientY : e.clientY;
            startX = clientX;
            startY = clientY;
            const rect = launcherEl.getBoundingClientRect();
            origX = rect.left;
            origY = rect.top;
        };

        const onPointerMove = e => {
            if (!isDown) return;
            const clientX = e.touches ? e.touches[0].clientX : e.clientX;
            const clientY = e.touches ? e.touches[0].clientY : e.clientY;
            const dx = clientX - startX;
            const dy = clientY - startY;
            if (!hasDragged && Math.hypot(dx, dy) > 6) {
                hasDragged = true;
                launcherEl.classList?.add?.('is-dragging');
            }
            if (hasDragged) {
                e.preventDefault?.();
                const width = launcherEl.offsetWidth || 120;
                const height = launcherEl.offsetHeight || 44;
                const newLeft = Math.max(10, Math.min((window.innerWidth || 800) - width - 10, origX + dx));
                const newTop = Math.max(10, Math.min((window.innerHeight || 600) - height - 10, origY + dy));
                launcherEl.style.left = newLeft + 'px';
                launcherEl.style.top = newTop + 'px';
                launcherEl.style.right = 'auto';
                launcherEl.style.bottom = 'auto';
            }
        };

        const onPointerUp = () => {
            if (!isDown) return;
            isDown = false;
            launcherEl.classList?.remove?.('is-dragging');
            if (hasDragged) {
                try {
                    const rect = launcherEl.getBoundingClientRect();
                    window.sessionStorage?.setItem('flowerChat_pos', JSON.stringify({left: rect.left, top: rect.top}));
                } catch (_) {}
            }
        };

        launcherEl.addEventListener('mousedown', onPointerDown);
        window.addEventListener('mousemove', onPointerMove, {passive: false});
        window.addEventListener('mouseup', onPointerUp);

        launcherEl.addEventListener('touchstart', onPointerDown, {passive: true});
        window.addEventListener('touchmove', onPointerMove, {passive: false});
        window.addEventListener('touchend', onPointerUp);
    }

    launcherEl?.addEventListener('click', async () => {
        if (hasDragged) {
            hasDragged = false;
            return;
        }
        if (busy || openingContact) { status('Vui lòng đợi thao tác chat hiện tại hoàn tất.'); return; }
        const productContact = document.querySelector('[data-chat-detail-contact]');
        if (productContact && !productContact.disabled) { productContact.click(); return; }
        if (context?.role === 'USER') {
            current = null; historyVersion++; pending = null; setQuestionProduct(null);
            text.value = ''; image.value = ''; fileName(); el('chatMessages').replaceChildren();
            el('chatCurrentTitle').textContent = 'Chọn hội thoại để xem tin nhắn'; el('chatOlder').hidden = true; controls();
        }
        await openPanel();
    });
    el('chatClose').addEventListener('click', () => { panel.hidden = true; el('chatLauncher').setAttribute('aria-expanded', 'false'); el('chatLauncher').focus(); });
    el('chatReconnect').addEventListener('click', async () => {
        retry = 0; stopped = false;
        try { await api('/context'); await openPanel(); } catch (error) { status(error.message, true); }
    });
    el('chatRefresh').addEventListener('click', async () => {
        try { await loadInbox(); if (current) await history(); } catch (error) { status(error.message); }
    });
    async function switchMailbox(kind) {
        if (busy || openingContact) { status('Vui lòng đợi thao tác hiện tại hoàn tất.'); return; }
        mailbox = kind; inboxVersion++; historyVersion++; current = null; pending = null;
        setQuestionProduct(null);
        text.value = ''; image.value = ''; fileName(); conversations.clear(); renderInbox(); el('chatMessages').replaceChildren();
        el('chatCurrentTitle').textContent = 'Chọn hội thoại để xem tin nhắn'; el('chatOlder').hidden = true;
        channelButtons.forEach(button => button.setAttribute('aria-pressed', String(button.dataset.chatChannel === kind)));
        updateFilters();
        controls();
        try { await loadInbox(); } catch (error) { status(error.message); }
    }
    channelButtons.forEach(button => button.addEventListener('click', () => switchMailbox(button.dataset.chatChannel)));
    el('chatFilterToggle').addEventListener('click', () => { if (context?.role === 'ADMIN') { filtersOpen = !filtersOpen; updateFilters(); } });
    el('chatClearFilters').addEventListener('click', async () => {
        if (busy || openingContact) { status('Vui lòng đợi thao tác hiện tại hoàn tất.'); return; }
        ['chatFilterStore','chatFilterKeyword','chatFilterFrom','chatFilterTo'].forEach(id => { el(id).value = ''; });
        await switchMailbox(mailbox);
    });
    el('chatFilters').addEventListener('submit', async event => {
        event.preventDefault();
        if (el('chatFilterFrom').value && el('chatFilterTo').value && el('chatFilterFrom').value > el('chatFilterTo').value) {
            status('Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.'); return;
        }
        await switchMailbox(mailbox);
    });
    el('chatBranchSupportButton').addEventListener('click', async event => {
        if (busy || openingContact) { status('Vui lòng đợi thao tác hiện tại hoàn tất.'); return; }
        const button = event.currentTarget; button.disabled = true;
        try {
            await switchMailbox('BRANCH_SUPPORT');
            const conversation = await api('/conversations', {method: 'POST', headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({kind: 'BRANCH_SUPPORT', storeId: context.storeId})});
            upsert(conversation); await select(conversation);
        } catch (error) { status(error.message); } finally { button.disabled = false; }
    });
    el('chatMoreConversations').addEventListener('click', () => loadInbox(inboxPage + 1).catch(error => status(error.message)));
    el('chatOlder').addEventListener('click', () => { const first = el('chatMessages').firstElementChild; if (first) history(first.dataset.messageId).catch(error => status(error.message)); });
    document.addEventListener('click', async event => {
        const button = event.target.closest('[data-chat-system], [data-chat-product-id], [data-chat-order-id]');
        if (!button || button.disabled) return;
        if (busy || openingContact) { status('Vui lòng đợi thao tác chat hiện tại hoàn tất.'); return; }
        openingContact = true; controls();
        event.preventDefault(); button.disabled = true;
        try {
            await openPanel();
            if (context?.role !== 'USER') throw new Error('Chỉ khách hàng được liên hệ cửa hàng.');
            if (button.hasAttribute('data-chat-system')) {
                const conversation = await api('/conversations', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({kind: 'ADMIN'})});
                upsert(conversation); await select(conversation); text.focus(); return;
            }
            const result = await api('/conversations/contact', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({
                productId: button.dataset.chatProductId ? Number(button.dataset.chatProductId) : null,
                orderId: button.dataset.chatOrderId ? Number(button.dataset.chatOrderId) : null,
                storeId: button.dataset.chatStoreId ? Number(button.dataset.chatStoreId) : null
            })});
            upsert(result.conversation); await select(result.conversation);
            setQuestionProduct(result.product);
            text.value = result.suggestedMessage || ''; resizeComposer(); text.focus();
        } catch (error) { status(error.message); } finally { button.disabled = false; openingContact = false; controls(); }
    });
    image.addEventListener('change', () => { const file = image.files[0]; if (file && (file.size > 10 * 1024 * 1024 || !['image/jpeg','image/png','image/webp','image/gif'].includes(file.type))) { image.value = ''; status('Chọn ảnh JPG, PNG, WEBP hoặc GIF, tối đa 10 MB.'); } fileName(); });
    el('chatRemoveImage').addEventListener('click', () => { image.value = ''; fileName(); });
    el('chatRemoveProduct').addEventListener('click', () => { if (!busy) setQuestionProduct(null); });
    el('chatSendForm').addEventListener('submit', async event => {
        event.preventDefault(); if (!current || !connected || busy || monitorMode()) return;
        const content = text.value.trim(), file = image.files[0];
        if (!content && !file && !questionProduct) { status('Vui lòng nhập tin nhắn hoặc chọn ảnh.'); text.focus(); return; }
        const productId = questionProduct?.id || null;
        if (!pending || pending.text !== content || pending.file !== file || pending.productId !== productId) pending = {id: crypto.randomUUID(), text: content, file, productId};
        const attempt = pending, conversationId = current.id;
        busy = true; controls(); status(file ? 'Đang tải ảnh lên Cloudinary…' : 'Đang gửi tin nhắn…');
        try {
            if (file) {
                const data = new FormData(); data.set('file', file); data.set('content', content); data.set('clientMessageId', attempt.id);
                if (attempt.productId) data.set('productId', attempt.productId);
                const message = await api(scoped('/conversations/' + conversationId + '/images'), {method: 'POST', body: data}); ack(message);
                if (message.conversationId === current?.id) { bubble(message); el('chatMessages').scrollTop = el('chatMessages').scrollHeight; }
                loadInbox().catch(() => {});
            } else {
                frame('SEND', {destination: '/app/chat.send', 'content-type': 'application/json'}, JSON.stringify({conversationId, content, clientMessageId: attempt.id, productId: attempt.productId}));
                attempt.timer = setTimeout(() => {
                    if (pending?.id !== attempt.id) return;
                    busy = false; controls(); status('Chưa nhận được xác nhận. Bấm Gửi để thử lại, tin nhắn sẽ không bị lưu trùng.');
                }, 15000);
            }
        } catch (error) {
            if (pending?.id === attempt.id) { clearTimeout(attempt.timer); busy = false; controls(); status(error.message); }
        }
    });
    text.addEventListener('keydown', event => { if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) { event.preventDefault(); el('chatSendForm').requestSubmit(); } });
    text.addEventListener('input', resizeComposer);
    window.addEventListener('resize', resizeComposer);
    const navigationToggle = el('chatNavigationToggle'), navigation = el('chatAdminNavigation');
    function closeNavigation() { navigation?.classList.remove('is-open'); navigationToggle?.setAttribute('aria-expanded','false'); navigationToggle?.focus(); }
    navigationToggle?.addEventListener('click', () => {
        const open = !navigation.classList.contains('is-open'); navigation.classList.toggle('is-open', open);
        navigationToggle.setAttribute('aria-expanded', String(open));
    });
    el('chatNavigationClose')?.addEventListener('click', closeNavigation);
    document.addEventListener('keydown', event => {
        if (event.key !== 'Escape') return;
        if (navigation?.classList.contains('is-open')) closeNavigation();
        else if (!panel.hidden && !root.classList?.contains('chat-workbench')) el('chatClose').click();
    });
    window.addEventListener('beforeunload', () => { stopped = true; clearTimeout(reconnectTimer); clearTimeout(connectTimer); clearInterval(heartbeat); socket?.close(); });
    resizeComposer();
    ensureContext().then(() => { connect(); if (root.classList?.contains('chat-workbench')) openPanel(); }).catch(error => status(error.message, true));
})();
