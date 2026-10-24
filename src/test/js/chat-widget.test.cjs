const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function widget(options = {}) {
    const nodes = new Map(), sockets = [], timers = new Map();
    const requests = [], documentEvents = {}, windowEvents = {};
    let next = 0, denied = false, reads = 0;
    const node = id => {
        if (!nodes.has(id)) nodes.set(id, {
            dataset: { csrfHeader: 'X-CSRF-TOKEN', csrfToken: 'token' }, hidden: true, disabled: true,
            value: '', files: [], children: [], events: {}, textContent: '',
            classList: { values: new Set(), add(value) { this.values.add(value); }, remove(value) { this.values.delete(value); },
                contains(value) { return this.values.has(value); }, toggle(value, force) { const enabled=force ?? !this.values.has(value); if(enabled)this.values.add(value);else this.values.delete(value);return enabled; } },
            scrollHeight: 0, style: {}, attributes: {},
            hasAttribute(name) { return name==='data-chat-system' && this.dataset.chatSystem!==undefined; },
            insertBefore(item, nextItem) { const index=this.children.indexOf(nextItem); if(index<0)this.children.push(item);else this.children.splice(index,0,item); },
            addEventListener(name, callback) { this.events[name] = callback; },
            setAttribute(name,value) { this.attributes[name]=value; }, replaceChildren() { this.children = []; }, append(...items) { this.children.push(...items); }, focus() {}, querySelector() { return null; },
            click() { return this.events.click?.({currentTarget:this}); }
        });
        return nodes.get(id);
    };
    class Socket {
        static OPEN = 1; static CONNECTING = 0;
        constructor(url, protocols) { this.url = url; this.protocols = protocols; this.readyState = 0; sockets.push(this); }
        send(value) { this.lastFrame = value; (this.frames ||= []).push(value); }
        close() { this.readyState = 3; this.onclose?.(); }
        open() { this.readyState = 1; this.onopen?.(); }
        message(data) { this.onmessage?.({ data }); }
    }
    const context = {
        document: { getElementById: node, createElement: tag => node(tag + ':' + ++next), addEventListener(name, callback) { documentEvents[name] = callback; },
            querySelector: () => options.detailProduct ? node('detailContact') : null,
            querySelectorAll: () => ['ADMIN','BRANCH_SUPPORT','STORE'].map(kind => { const button=node('channel'+kind); button.dataset.chatChannel=kind; return button; }) },
        URLSearchParams,
        crypto: require('node:crypto').webcrypto,
        window: { addEventListener(name,callback) { windowEvents[name]=callback; } }, location: { protocol: 'http:', host: 'localhost:8080' },
        WebSocket: Socket,
        setTimeout(callback, delay) { const id = ++next; timers.set(id, { callback, delay }); return id; },
        clearTimeout(id) { timers.delete(id); }, setInterval() { return 99; }, clearInterval() {},
        fetch: async (url, request = {}) => {
            requests.push({url, request});
            reads++;
            const provided = options.respond ? await options.respond(url, request) : null;
            return { ok: !denied, status: denied ? 403 : 200, redirected: false,
                json: async () => denied ? { message: 'Tài khoản đã bị khóa' } : provided || (
                    url.endsWith('/contact') ? { conversation: options.contact || {id: 7, kind: 'STORE', storeId: 5, storeName: 'Quận 7', customerId: 1}, suggestedMessage: 'Tôi muốn hỏi về mẫu hoa #26', product: {id:26,name:'Lan hồ điệp',imageUrl:'https://example.test/orchid.jpg',price:2300000} }
                    : url.endsWith('/conversations') && request.method === 'POST' ? {id: 11, kind: JSON.parse(request.body).kind, storeId: JSON.parse(request.body).storeId, storeName: 'Quận 7', customerId: 1}
                    : { userId: 1, role: options.role || 'USER', storeId: options.storeId || 5, storeName: 'Quận 7', stores: [],
                        conversations: options.conversations || [], messages: [], hasMore: false }) };
        }
    };
    if(options.workbench) node('flowerChat').classList.add('chat-workbench');
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/chat-widget.js'), 'utf8'), context);
    return { node, sockets, timers, requests, documentEvents, windowEvents, deny() { denied = true; }, reads: () => reads };
}
const flush = () => new Promise(resolve => setImmediate(resolve));

test('connection timeout releases socket and allows reconnect', async () => {
    const app = widget(); await flush();
    assert.deepEqual(Array.from(app.sockets[0].protocols), ['v12.stomp']);
    const timeout = [...app.timers.values()].find(timer => timer.delay === 10000);
    assert.ok(timeout); timeout.callback();
    assert.equal(app.sockets[0].readyState, 3);
    assert.match(app.node('chatStatus').textContent, /10 giây/);
    assert.equal(app.node('chatSend').disabled, true);
    await app.node('chatReconnect').events.click();
    assert.equal(app.sockets.length, 2);
    app.sockets[0].message('CONNECTED\nversion:1.2\n\n\0');
    assert.match(app.node('chatStatus').textContent, /Đang kết nối/);
});

test('STOMP error closes connection, keeps draft and stops automatic retries', async () => {
    const app = widget(); await flush();
    app.node('chatText').value = 'Tin chưa gửi';
    const socket = app.sockets[0]; socket.open(); socket.message('CONNECTED\nversion:1.2\n\n\0'); await flush();
    socket.message('ERROR\nmessage:Denied\n\nAccess denied\0');
    assert.equal(socket.readyState, 3);
    assert.match(app.node('chatStatus').textContent, /Server từ chối/);
    assert.equal(app.node('chatText').value, 'Tin chưa gửi');
    assert.equal(app.node('chatSend').disabled, true);
    assert.equal(app.timers.size, 0);
});

test('security JSON message is displayed and locked session is not reconnected', async () => {
    const app = widget(); await flush(); app.deny();
    await app.node('chatLauncher').events.click();
    assert.match(app.node('chatStatus').textContent, /Tài khoản đã bị khóa/);
    assert.equal(app.sockets[0].readyState, 3);
    const count = app.sockets.length;
    await app.node('chatReconnect').events.click();
    assert.equal(app.sockets.length, count);
    assert.equal(app.timers.size, 0);
});

test('manager uses bound branch socket and has no store picker or personal support', async () => {
    const app = widget({role: 'MANAGER', storeId: 5, conversations: [
        {id: 7, kind: 'STORE', storeId: 5, storeName: 'Quận 7', customerId: 2},
        {id: 8, kind: 'STORE', storeId: 6, storeName: 'Quận 1', customerId: 3},
        {id: 9, kind: 'ADMIN', customerId: 1},
    ]}); await flush();
    assert.equal(app.node('chatSupportButton').hidden, true);
    assert.match(app.node('chatHeading').textContent, /Quận 7/);
    const socket = app.sockets[0]; socket.open();
    assert.match(socket.lastFrame, /chat-store-id:5/);
    socket.message('CONNECTED\nversion:1.2\n\n\0'); await flush();
    assert.ok(socket.frames.some(frame => frame.includes('destination:/user/queue/chat.store.5\n')));
    assert.ok(!socket.frames.some(frame => frame.includes('destination:/user/queue/chat\n')));
    assert.ok(app.requests.some(r => r.url.includes('/conversations?') && new URLSearchParams(r.url.split('?')[1]).get('storeId') === '5'));
    assert.equal(app.node('chatConversations').children.length, 1);
    socket.message('MESSAGE\nsubscription:chat-messages\n\n' + JSON.stringify({type: 'MESSAGE',
        conversation: {id: 10, kind: 'STORE', storeId: 6, customerId: 3}, message: {senderId: 3}}) + '\0');
    assert.equal(app.node('chatConversations').children.length, 1);
    assert.equal(app.node('chatBadge').hidden, true);
});

test('product contact opens seller automatically and suggests context without sending', async () => {
    const app = widget(); await flush();
    const button = app.node('contactProduct'); button.disabled = false; button.dataset = {chatProductId: '26', chatStoreId: '5'};
    await app.documentEvents.click({target: {closest: () => button}, preventDefault() {}});
    const request = app.requests.find(r => r.url.endsWith('/conversations/contact'));
    assert.deepEqual(JSON.parse(request.request.body), {productId: 26, orderId: null, storeId: 5});
    assert.match(app.node('chatCurrentTitle').textContent, /Quận 7/);
    assert.match(app.node('chatText').value, /mẫu hoa #26/);
    assert.equal(button.disabled, false);
    assert.ok(!(app.sockets[0].frames || []).some(frame => frame.startsWith('SEND')));
});

test('order contact supplies only order id and leaves branch resolution to backend', async () => {
    const app = widget(); await flush();
    const button = app.node('contactOrder'); button.disabled = false; button.dataset = {chatOrderId: '23'};
    await app.documentEvents.click({target: {closest: () => button}, preventDefault() {}});
    assert.deepEqual(JSON.parse(app.requests.find(r => r.url.endsWith('/contact')).request.body), {productId: null, orderId: 23, storeId: null});
});

test('manager opens separate internal support for its bound branch', async () => {
    const app = widget({role: 'MANAGER', storeId: 5}); await flush();
    const socket = app.sockets[0]; socket.open();socket.message('CONNECTED\nversion:1.2\n\n\0');await flush();
    assert.equal(app.node('chatBranchSupportButton').hidden, false);
    assert.ok(socket.frames.some(frame => frame.includes('destination:/user/queue/chat.support.5\n')));
    await app.node('chatBranchSupportButton').events.click({currentTarget: app.node('chatBranchSupportButton')});
    const request = app.requests.find(r => r.request.method === 'POST');
    assert.deepEqual(JSON.parse(request.request.body), {kind: 'BRANCH_SUPPORT', storeId: 5});
    assert.match(app.node('chatCurrentTitle').textContent, /Hỗ trợ nội bộ.*Quận 7/);
    assert.equal(app.node('chatSendForm').hidden, false);
    assert.equal(app.node('chatSupportButton').hidden, true);
});

test('admin monitor hides and disables sending including programmatic submit', async () => {
    const app = widget({role: 'ADMIN', conversations: [{id: 7, kind: 'STORE', storeId: 5, storeName: 'Quận 7', customerId: 2}]});await flush();
    const socket=app.sockets[0];socket.open();socket.message('CONNECTED\nversion:1.2\n\n\0');await flush();
    await app.node('channelSTORE').events.click();
    await app.node('chatConversations').children[0].events.click();
    assert.equal(app.node('chatSendForm').hidden,true);
    assert.equal(app.node('chatMonitorNotice').hidden,false);
    assert.equal(app.node('chatText').disabled,true);assert.equal(app.node('chatImage').disabled,true);
    app.node('chatText').value='Giả danh cửa hàng';
    await app.node('chatSendForm').events.submit({preventDefault(){}});
    assert.ok(!socket.frames.some(frame=>frame.startsWith('SEND')));
    await app.node('channelADMIN').events.click();
    assert.equal(app.node('chatSendForm').hidden,false);
    assert.equal(app.node('chatMonitorNotice').hidden,true);
});

test('late inbox from previous admin channel cannot clear or mix current monitor list', async () => {
    let finish;
    const oldInbox=new Promise(resolve=>{finish=resolve;});
    const app=widget({role:'ADMIN',respond:async url=>{
        if(url.includes('/conversations?') && new URLSearchParams(url.split('?')[1]).get('kind')==='ADMIN') return oldInbox;
        if(url.includes('/conversations?')) return {conversations:[{id:7,kind:'STORE',storeId:5,storeName:'Quận 7',customerId:2}],hasMore:false};
        return null;
    }});await flush();const socket=app.sockets[0];socket.open();socket.message('CONNECTED\nversion:1.2\n\n\0');await flush();
    await app.node('channelSTORE').events.click();
    assert.equal(app.node('chatConversations').children.length,1);
    finish({conversations:[{id:8,kind:'ADMIN',customerId:3}],hasMore:false});await flush();
    assert.equal(app.node('chatConversations').children.length,1);
});

test('live admin support events cannot bypass active branch filters', async () => {
    const app=widget({role:'ADMIN',conversations:[{id:7,kind:'BRANCH_SUPPORT',storeId:5,storeName:'Quận 7',customerId:2}]});
    await flush();const socket=app.sockets[0];socket.open();socket.message('CONNECTED\nversion:1.2\n\n\0');await flush();
    app.node('chatFilterStore').value='5';await app.node('channelBRANCH_SUPPORT').events.click();
    const before=app.requests.length;
    socket.message('MESSAGE\nsubscription:chat-messages\n\n'+JSON.stringify({type:'MESSAGE',
        conversation:{id:8,kind:'BRANCH_SUPPORT',storeId:6,storeName:'Quận 1',customerId:3},message:{senderId:3}})+'\0');
    await flush();
    assert.equal(app.node('chatConversations').children.length,1);
    assert.ok(app.requests.slice(before).some(r=>r.url.includes('/conversations?') && new URLSearchParams(r.url.split('?')[1]).get('storeId')==='5'));
});

test('account system chat opens admin directly without automatically sending', async () => {
    const app=widget();await flush();
    const button=app.node('systemMenu');button.disabled=false;button.dataset={chatSystem:''};
    await app.documentEvents.click({target:{closest:()=>button},preventDefault(){}});
    assert.deepEqual(JSON.parse(app.requests.find(r=>r.request.method==='POST').request.body),{kind:'ADMIN'});
    assert.match(app.node('chatCurrentTitle').textContent,/Trò chuyện với hệ thống/);
    assert.equal(app.node('chatQuestionProduct').hidden,true);
    assert.ok(!(app.sockets[0].frames||[]).some(frame=>frame.startsWith('SEND')));
});

test('homepage launcher lists both stores and system without opening or sending a conversation', async () => {
    const app=widget({conversations:[{id:7,kind:'STORE',storeId:5,storeName:'Quận 7',customerId:1},
        {id:8,kind:'ADMIN',customerId:1}]});await flush();
    await app.node('chatLauncher').events.click();
    assert.equal(app.node('chatConversations').children.length,2);
    assert.match(app.node('chatCurrentTitle').textContent,/Chọn hội thoại/);
    assert.ok(!app.requests.some(r=>r.request.method==='POST'));
    assert.ok(app.requests.filter(r=>r.url.includes('/conversations?')).every(r=>!new URLSearchParams(r.url.split('?')[1]).has('kind')));
});

test('product card is displayed before sending and product id is attached to question', async () => {
    const app=widget();await flush();const socket=app.sockets[0];socket.open();socket.message('CONNECTED\nversion:1.2\n\n\0');await flush();
    const button=app.node('productButton');button.disabled=false;button.dataset={chatProductId:'26',chatStoreId:'5'};
    await app.documentEvents.click({target:{closest:()=>button},preventDefault(){}});
    assert.equal(app.node('chatQuestionProduct').hidden,false);
    const card=app.node('chatQuestionProductCard').children[0];
    assert.equal(card.href,'/products/26');assert.equal(card.children[0].src,'https://example.test/orchid.jpg');
    assert.equal(card.children[1].children[0].textContent,'Lan hồ điệp');
    assert.ok(!socket.frames.some(frame=>frame.startsWith('SEND')));
    app.node('chatText').value='Mẫu này còn không?';
    await app.node('chatSendForm').events.submit({preventDefault(){}});
    const frame=socket.frames.find(frame=>frame.startsWith('SEND'));
    const body=JSON.parse(frame.split('\n\n')[1].slice(0,-1));
    assert.equal(body.productId,26);assert.equal(body.conversationId,7);
    socket.message('MESSAGE\nsubscription:chat-acks\n\n'+JSON.stringify({id:1,conversationId:7,senderId:1,senderName:'Khách',
        content:body.content,clientMessageId:body.clientMessageId,createdAt:'2026-10-13T17:00:00',
        product:{id:26,name:'Lan hồ điệp',price:2300000}})+'\0');
    assert.equal(app.node('chatQuestionProduct').hidden,true);
    assert.equal(app.node('chatMessages').children[0].children[1].href,'/products/26');
});

test('changing to system conversation clears product attachment', async () => {
    const app=widget();await flush();
    const product=app.node('productButton');product.disabled=false;product.dataset={chatProductId:'26'};
    await app.documentEvents.click({target:{closest:()=>product},preventDefault(){}});
    const system=app.node('systemButton');system.disabled=false;system.dataset={chatSystem:''};
    await app.documentEvents.click({target:{closest:()=>system},preventDefault(){}});
    assert.equal(app.node('chatQuestionProduct').hidden,true);
    assert.match(app.node('chatCurrentTitle').textContent,/Trò chuyện với hệ thống/);
});

test('floating message button on product detail opens the selling branch and product context', async () => {
    const app=widget({detailProduct:true});await flush();
    const button=app.node('detailContact');button.disabled=false;button.dataset={chatProductId:'26',chatStoreId:'5'};
    let done;button.click=()=>{done=app.documentEvents.click({target:{closest:()=>button},preventDefault(){}});};
    await app.node('chatLauncher').events.click();await done;
    assert.deepEqual(JSON.parse(app.requests.find(r=>r.url.endsWith('/contact')).request.body),{productId:26,storeId:5,orderId:null});
    assert.equal(app.node('chatQuestionProduct').hidden,false);
    assert.ok(!(app.sockets[0].frames||[]).some(frame=>frame.startsWith('SEND')));
});

test('admin monitoring places customer messages right and branch messages left', async () => {
    const app=widget({role:'ADMIN',conversations:[{id:7,kind:'STORE',storeId:5,storeName:'Quận 7',customerId:2}],
        respond:async url=>url.includes('/messages') ? {messages:[
            {id:101,senderId:'2',senderName:'Khách',content:'Tôi cần tư vấn',createdAt:'2026-10-13T17:00:00'},
            {id:102,senderId:3,senderName:'Quản lý',content:'Cửa hàng xin chào',createdAt:'2026-10-13T17:01:00'},
            {id:103,senderId:4,senderName:'Quản lý khác cùng chi nhánh',content:'Chúng tôi hỗ trợ bạn',createdAt:'2026-10-13T17:02:00'}
        ],hasMore:false} : null});
    await flush();await app.node('channelSTORE').events.click();
    await app.node('chatConversations').children[0].events.click();
    const [customer,branch,otherManager]=app.node('chatMessages').children;
    assert.equal(customer.classList.contains('mine'),true);
    assert.equal(customer.classList.contains('customer-side'),true);
    for(const message of [branch,otherManager]) {
        assert.equal(message.classList.contains('mine'),false);
        assert.equal(message.classList.contains('branch-side'),true);
    }
    assert.equal(app.node('chatSendForm').hidden,true);
    assert.equal(app.node('chatText').disabled,true);
    assert.equal(app.node('chatImage').disabled,true);
});

test('admin customer support retains own-message alignment outside monitoring', async () => {
    const app=widget({role:'ADMIN',conversations:[{id:7,kind:'ADMIN',customerId:2}],
        respond:async url=>url.includes('/messages') ? {messages:[
            {id:101,senderId:2,senderName:'Khách',content:'Cần hỗ trợ',createdAt:'2026-10-13T17:00:00'},
            {id:102,senderId:1,senderName:'Admin',content:'Xin chào',createdAt:'2026-10-13T17:01:00'}
        ],hasMore:false} : null});
    await flush();await app.node('chatLauncher').events.click();
    await app.node('chatConversations').children[0].events.click();
    const [customer,admin]=app.node('chatMessages').children;
    assert.equal(customer.classList.contains('mine'),false);
    assert.equal(admin.classList.contains('mine'),true);
    assert.equal(customer.classList.contains('branch-side'),false);
    assert.equal(admin.classList.contains('customer-side'),false);
    assert.equal(app.node('chatSendForm').hidden,false);
});

test('admin filters start collapsed and branch field is excluded for customer support', async () => {
    const app=widget({role:'ADMIN'});await flush();
    assert.equal(app.node('chatFilters').hidden,true);
    assert.equal(app.node('chatFilterToggle').hidden,false);
    assert.equal(app.node('chatFilterStoreField').hidden,true);
    await app.node('chatFilterToggle').events.click();
    assert.equal(app.node('chatFilters').hidden,false);
    assert.equal(app.node('chatFilterToggle').attributes['aria-expanded'],'true');
    app.node('chatFilterStore').value='5';
    await app.node('chatFilters').events.submit({preventDefault(){}});
    assert.ok(!new URLSearchParams(app.requests.at(-1).url.split('?')[1]).has('storeId'));
    await app.node('channelBRANCH_SUPPORT').events.click();
    assert.equal(app.node('chatFilterStoreField').hidden,false);
    assert.equal(new URLSearchParams(app.requests.at(-1).url.split('?')[1]).get('storeId'),'5');
    await app.node('chatFilterToggle').events.click();
    assert.equal(app.node('chatFilters').hidden,true);
});

test('hiding filters keeps applied values and clear filters removes them', async () => {
    const app=widget({role:'ADMIN'});await flush();
    await app.node('chatFilterToggle').events.click();
    app.node('chatFilterKeyword').value='Ly Gia Han';
    await app.node('chatFilters').events.submit({preventDefault(){}});
    assert.equal(app.node('chatFilterBadge').hidden,false);
    await app.node('chatFilterToggle').events.click();
    await app.node('chatRefresh').events.click();
    assert.equal(new URLSearchParams(app.requests.at(-1).url.split('?')[1]).get('keyword'),'Ly Gia Han');
    await app.node('chatClearFilters').events.click();
    assert.equal(app.node('chatFilterBadge').hidden,true);
    assert.ok(!new URLSearchParams(app.requests.at(-1).url.split('?')[1]).has('keyword'));
});

test('admin inbox puts customer names first rather than repeating support labels', async () => {
    const app=widget({role:'ADMIN',conversations:[
        {id:7,kind:'ADMIN',customerId:2,customerName:'Ly Gia Han'},
        {id:8,kind:'STORE',storeId:5,storeName:'Quận 7',customerId:3,customerName:'Khách chi nhánh'}]});
    await flush();await app.node('chatLauncher').events.click();
    assert.equal(app.node('chatConversations').children[0].children[0].textContent,'Ly Gia Han');
    assert.equal(app.node('chatScope').hidden,true);
    await app.node('channelSTORE').events.click();
    const item=app.node('chatConversations').children[0];
    assert.equal(item.children[0].textContent,'Khách chi nhánh');
    assert.equal(item.children[1].textContent,'Quận 7');
});

test('composer grows for multiline input but has a height cap and responds to resizing', async () => {
    const app=widget();await flush();const text=app.node('chatText');
    assert.equal(text.style.height,'42px');
    text.scrollHeight=88;text.events.input();assert.equal(text.style.height,'88px');
    text.scrollHeight=240;text.events.input();assert.equal(text.style.height,'112px');assert.equal(text.style.overflowY,'auto');
    text.scrollHeight=22;text.events.input();assert.equal(text.style.height,'42px');assert.equal(text.style.overflowY,'hidden');
    text.scrollHeight=70;app.windowEvents.resize();assert.equal(text.style.height,'70px');
});

test('workbench escape closes mobile navigation without hiding the chat', async () => {
    const app=widget({role:'ADMIN',workbench:true});await flush();await flush();
    assert.equal(app.node('chatPanel').hidden,false);
    app.documentEvents.keydown({key:'Escape'});assert.equal(app.node('chatPanel').hidden,false);
    await app.node('chatNavigationToggle').events.click();
    assert.equal(app.node('chatAdminNavigation').classList.contains('is-open'),true);
    app.documentEvents.keydown({key:'Escape'});
    assert.equal(app.node('chatAdminNavigation').classList.contains('is-open'),false);
    assert.equal(app.node('chatPanel').hidden,false);
    await app.node('chatNavigationToggle').events.click();await app.node('chatNavigationClose').events.click();
    assert.equal(app.node('chatNavigationToggle').attributes['aria-expanded'],'false');
});

test('attachment filename row appears only after selecting an image', async () => {
    const app=widget();await flush();const image=app.node('chatImage');
    image.files=[{name:'hoa.png',type:'image/png',size:100}];image.events.change();
    assert.equal(app.node('chatAttachmentBar').hidden,false);
    assert.equal(app.node('chatFileName').textContent,'hoa.png');
    image.files=[];image.events.change();assert.equal(app.node('chatAttachmentBar').hidden,true);
});

test('workbench layout consumes available height with independently scrollable panes', () => {
    const css=fs.readFileSync(path.join(__dirname,'../../main/resources/static/css/chat-widget.css'),'utf8');
    assert.match(css,/body\.admin-body\.admin-chat-page\s*\{[^}]*height:\s*100dvh\s*!important;[^}]*overflow:\s*hidden\s*!important/);
    assert.match(css,/#flowerChat\.chat-workbench \.chat-panel\s*\{[^}]*height:\s*100%;[^}]*min-height:\s*0/);
    assert.match(css,/#flowerChat \.chat-messages\s*\{[^}]*overflow-y:\s*auto/);
    assert.match(css,/#flowerChat \.chat-sidebar \.chat-inbox\s*\{[^}]*max-height:\s*none/);
    assert.ok(!css.includes('calc(100dvh - 190px)'));
});


test('staff receives only its branch customer queue and has no internal support controls', async () => {
    const app = widget({role: 'STAFF', storeId: 5}); await flush();
    const socket = app.sockets[0]; socket.open();
    assert.ok(socket.frames.some(frame => frame.includes('chat-store-id:5')));
    socket.message('CONNECTED\nversion:1.2\n\n\0'); await flush();
    assert.ok(socket.frames.some(frame => frame.includes('destination:/user/queue/chat.store.5\n')));
    assert.ok(!socket.frames.some(frame => frame.includes('destination:/user/queue/chat.support.')));
    assert.equal(app.node('chatBranchSupportButton').hidden, true);
    assert.equal(app.node('chatChannelTabs').hidden, true);
    assert.match(app.node('chatHeading').textContent, /Hộp thư khách hàng/);
});


test('staff workbench scopes requests and ignores conversations from another branch', async () => {
    const app = widget({role: 'STAFF', storeId: 5, workbench: true, conversations: [
        {id: 7, kind: 'STORE', storeId: 5, storeName: 'Quận 7', customerId: 2},
        {id: 8, kind: 'STORE', storeId: 9, storeName: 'Chi nhánh khác', customerId: 3}
    ]}); await flush(); await flush();
    assert.equal(app.node('chatPanel').hidden, false);
    assert.equal(app.node('chatConversations').children.length, 1);
    assert.ok(app.requests.some(request => request.url.includes('/conversations?') && request.url.includes('storeId=5')));
    await app.node('chatConversations').children[0].events.click();
    assert.ok(app.requests.some(request => request.url.includes('/conversations/7/messages?storeId=5')));
});
