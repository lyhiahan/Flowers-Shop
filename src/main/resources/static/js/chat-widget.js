(() => {
    'use strict';
    const root = document.getElementById('flowerChat');
    if (!root) return;
    const el = id => document.getElementById(id);
    const panel = el('chatPanel'), text = el('chatText'), image = el('chatImage');
    let context, current, socket, connected = false, busy = false, buffer = '', retry = 0;
    let reconnectTimer, heartbeat, pending, inboxPage = 0, unseen = 0, historyVersion = 0;
    const conversations = new Map();
    function status(message, reconnect = false) { el('chatStatus').textContent = message; el('chatReconnect').hidden = !reconnect; }
    async function api(path, options = {}) {
        const headers = {Accept: 'application/json', 'X-Requested-With': 'XMLHttpRequest', ...(options.headers || {})};
        if (options.method && options.method !== 'GET') headers[root.dataset.csrfHeader] = root.dataset.csrfToken;
        const response = await fetch('/api/chat' + path, {...options, headers, credentials: 'same-origin'});
        if (response.redirected) throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
        let data;
        try { data = await response.json(); } catch (_) { throw new Error('Không thể tải chat. Vui lòng thử lại.'); }
        if (!response.ok) throw new Error(data.error || 'Không thể thực hiện thao tác này.');
        return data;
    }
    function controls() {
        const disabled = !current || !connected || busy;
        text.disabled = image.disabled = el('chatSend').disabled = disabled;
        el('chatRemoveImage').disabled = busy;
    }
    function title(conversation) {
        let value = conversation.kind === 'ADMIN' ? 'Admin hỗ trợ' : conversation.storeName;
        if (context.role !== 'USER' && conversation.customerId !== context.userId) value += ' · ' + conversation.customerName;
        return value;
    }
    function upsert(conversation) {
        const previous = conversations.get(conversation.id);
        if (!previous || previous.updatedAt <= conversation.updatedAt) conversations.set(conversation.id, conversation);
    }
    function renderInbox() {
        el('chatConversations').replaceChildren();
        [...conversations.values()].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt) || b.id - a.id).forEach(conversation => {
            const button = document.createElement('button'); button.type = 'button';
            button.setAttribute('aria-current', String(current?.id === conversation.id));
            const strong = document.createElement('strong'); strong.textContent = title(conversation);
            const preview = document.createElement('small'); preview.textContent = conversation.lastMessage || 'Chưa có tin nhắn';
            button.append(strong, preview); button.addEventListener('click', () => select(conversation));
            el('chatConversations').append(button);
        });
    }
    async function loadInbox(page = 0) {
        const data = await api('/conversations?page=' + page);
        data.conversations.forEach(upsert); inboxPage = page;
        el('chatMoreConversations').hidden = !data.hasMore; renderInbox();
    }
    function bubble(message) {
        const log = el('chatMessages');
        if (log.querySelector('[data-message-id="' + message.id + '"]')) return;
        const item = document.createElement('div'); item.className = 'chat-message'; item.dataset.messageId = message.id;
        if (message.senderId === context.userId) item.classList.add('mine');
        const caption = document.createElement('small'); caption.textContent = message.senderName + ' · ' + new Date(message.createdAt).toLocaleString('vi-VN'); item.append(caption);
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
        const data = await api('/conversations/' + id + '/messages' + (before ? '?before=' + before : ''));
        if (version !== historyVersion || current?.id !== id) return;
        const log = el('chatMessages'), oldHeight = log.scrollHeight;
        data.messages.forEach(bubble); el('chatOlder').hidden = !data.hasMore;
        if (before) log.scrollTop += log.scrollHeight - oldHeight;
        else log.scrollTop = log.scrollHeight;
    }
    async function select(conversation) {
        if (busy) { status('Vui lòng đợi gửi xong tin nhắn hiện tại.'); return; }
        current = conversation; historyVersion++; pending = null;
        text.value = ''; image.value = ''; fileName();
        el('chatCurrentTitle').textContent = title(conversation); el('chatMessages').replaceChildren();
        el('chatOlder').hidden = true; controls(); renderInbox();
        try { await history(); } catch (error) { status(error.message); }
    }
    function fileName() { const file = image.files[0]; el('chatFileName').textContent = file?.name || ''; el('chatRemoveImage').hidden = !file; }
    function ack(message) {
        if (pending && message.senderId === context.userId && message.clientMessageId === pending.id) {
            clearTimeout(pending.timer); pending = null; busy = false; text.value = ''; image.value = ''; fileName(); controls();
            status('Đã gửi tin nhắn.');
        }
    }
    function received(event) {
        if (event.type !== 'MESSAGE' || !event.message || !event.conversation) return;
        upsert(event.conversation); renderInbox(); ack(event.message);
        if (event.message.conversationId === current?.id) { bubble(event.message); el('chatMessages').scrollTop = el('chatMessages').scrollHeight; }
        if (panel.hidden && event.message.senderId !== context.userId) { unseen++; el('chatBadge').textContent = unseen; el('chatBadge').hidden = false; }
    }
    function frame(command, headers = {}, body = '') {
        if (!socket || socket.readyState !== WebSocket.OPEN) throw new Error('Chat chưa kết nối. Vui lòng thử lại.');
        socket.send(command + '\n' + Object.entries(headers).map(([key, value]) => key + ':' + value).join('\n') + '\n\n' + body + '\0');
    }
    function connect() {
        if (socket && [WebSocket.OPEN, WebSocket.CONNECTING].includes(socket.readyState)) return;
        clearTimeout(reconnectTimer); connected = false; controls(); buffer = ''; status('Đang kết nối chat…');
        socket = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws-chat');
        socket.onopen = () => frame('CONNECT', {'accept-version': '1.2', host: location.host, 'heart-beat': '20000,0'});
        socket.onmessage = event => {
            buffer += event.data;
            if (buffer.length > 131072) { socket.close(); status('Dữ liệu chat không hợp lệ.'); return; }
            let end;
            while ((end = buffer.indexOf('\0')) >= 0) {
                const raw = buffer.slice(0, end).replace(/^\s+/, ''); buffer = buffer.slice(end + 1);
                const split = raw.indexOf('\n\n'); if (split < 0) continue;
                const lines = raw.slice(0, split).split('\n'), command = lines.shift(), headers = {};
                lines.forEach(line => { const colon = line.indexOf(':'); if (colon > 0) headers[line.slice(0, colon)] = line.slice(colon + 1); });
                if (command === 'CONNECTED') {
                    connected = true; retry = 0; frame('SUBSCRIBE', {id: 'chat-messages', destination: '/user/queue/chat', ack: 'auto'});
                    frame('SUBSCRIBE', {id: 'chat-errors', destination: '/user/queue/chat.errors', ack: 'auto'});
                    clearInterval(heartbeat); heartbeat = setInterval(() => { if (socket.readyState === WebSocket.OPEN) socket.send('\n'); }, 20000);
                    status('Chat đã kết nối.'); controls(); loadInbox().catch(error => status(error.message));
                    if (current) history().catch(error => status(error.message));
                } else if (command === 'MESSAGE') {
                    try {
                        const data = JSON.parse(raw.slice(split + 2));
                        if (headers.subscription === 'chat-errors') { if (pending) clearTimeout(pending.timer); busy = false; controls(); status(data.error || 'Không thể gửi tin nhắn.'); }
                        else received(data);
                    } catch (_) { status('Không thể đọc tin nhắn nhận được.'); }
                } else if (command === 'ERROR') status('Phiên chat bị từ chối. Vui lòng đăng nhập lại.', true);
            }
        };
        socket.onclose = () => {
            connected = false; busy = false; clearInterval(heartbeat); if (pending) clearTimeout(pending.timer); controls();
            status('Chat mất kết nối. Tin đã lưu vẫn được giữ lại.', true);
            if (retry < 6) reconnectTimer = setTimeout(connect, Math.min(1000 * 2 ** retry++, 15000));
        };
        socket.onerror = () => status('Chưa kết nối được chat.', true);
    }
    async function ensureContext() {
            if (!context) {
                context = await api('/context');
                if (context.role !== 'ADMIN') {
                    el('chatStartForm').hidden = false;
                    const support = document.createElement('option'); support.value = 'ADMIN'; support.textContent = 'Admin hỗ trợ'; el('chatDestination').append(support);
                    context.stores.forEach(store => { const option = document.createElement('option'); option.value = 'STORE:' + store.id; option.textContent = store.name; el('chatDestination').append(option); });
                }
            }
    }
    async function openPanel() {
        panel.hidden = false; el('chatLauncher').setAttribute('aria-expanded', 'true'); unseen = 0; el('chatBadge').hidden = true;
        try {
            await ensureContext(); await loadInbox(); connect();
        } catch (error) { status(error.message, true); }
    }
    el('chatLauncher').addEventListener('click', openPanel);
    el('chatClose').addEventListener('click', () => { panel.hidden = true; el('chatLauncher').setAttribute('aria-expanded', 'false'); el('chatLauncher').focus(); });
    el('chatReconnect').addEventListener('click', () => { retry = 0; openPanel(); });
    el('chatRefresh').addEventListener('click', () => loadInbox().catch(error => status(error.message)));
    el('chatMoreConversations').addEventListener('click', () => loadInbox(inboxPage + 1).catch(error => status(error.message)));
    el('chatOlder').addEventListener('click', () => { const first = el('chatMessages').firstElementChild; if (first) history(first.dataset.messageId).catch(error => status(error.message)); });
    el('chatStartForm').addEventListener('submit', async event => {
        event.preventDefault(); const value = el('chatDestination').value; if (!value) return;
        const button = event.currentTarget.querySelector('button'); button.disabled = true;
        try { const conversation = await api('/conversations', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({kind: value === 'ADMIN' ? 'ADMIN' : 'STORE', storeId: value === 'ADMIN' ? null : Number(value.split(':')[1])})}); upsert(conversation); await select(conversation); }
        catch (error) { status(error.message); } finally { button.disabled = false; }
    });
    image.addEventListener('change', () => { const file = image.files[0]; if (file && (file.size > 10 * 1024 * 1024 || !['image/jpeg','image/png','image/webp','image/gif'].includes(file.type))) { image.value = ''; status('Chọn ảnh JPG, PNG, WEBP hoặc GIF, tối đa 10 MB.'); } fileName(); });
    el('chatRemoveImage').addEventListener('click', () => { image.value = ''; fileName(); });
    el('chatSendForm').addEventListener('submit', async event => {
        event.preventDefault(); if (!current || !connected || busy) return;
        const content = text.value.trim(), file = image.files[0];
        if (!content && !file) { status('Vui lòng nhập tin nhắn hoặc chọn ảnh.'); text.focus(); return; }
        if (!pending || pending.text !== content || pending.file !== file) pending = {id: crypto.randomUUID(), text: content, file};
        busy = true; controls(); status(file ? 'Đang tải ảnh lên Cloudinary…' : 'Đang gửi tin nhắn…');
        try {
            if (file) {
                const data = new FormData(); data.set('file', file); data.set('content', content); data.set('clientMessageId', pending.id);
                const message = await api('/conversations/' + current.id + '/images', {method: 'POST', body: data}); ack(message);
                bubble(message); el('chatMessages').scrollTop = el('chatMessages').scrollHeight; loadInbox().catch(() => {});
            } else {
                frame('SEND', {destination: '/app/chat.send', 'content-type': 'application/json'}, JSON.stringify({conversationId: current.id, content, clientMessageId: pending.id}));
                pending.timer = setTimeout(() => { busy = false; controls(); status('Chưa nhận được xác nhận. Bấm Gửi để thử lại, tin nhắn sẽ không bị lưu trùng.'); }, 15000);
            }
        } catch (error) { busy = false; controls(); status(error.message); }
    });
    text.addEventListener('keydown', event => { if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) { event.preventDefault(); el('chatSendForm').requestSubmit(); } });
    document.addEventListener('keydown', event => { if (event.key === 'Escape' && !panel.hidden) el('chatClose').click(); });
    window.addEventListener('beforeunload', () => { clearTimeout(reconnectTimer); clearInterval(heartbeat); socket?.close(); });
    ensureContext().then(connect).catch(error => status(error.message, true));
})();
