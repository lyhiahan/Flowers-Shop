import os
import matplotlib.pyplot as plt
import matplotlib.patches as patches

os.makedirs('docs/diagrams', exist_ok=True)
plt.rcParams['font.sans-serif'] = 'DejaVu Sans'
plt.rcParams['axes.edgecolor'] = '#CCCCCC'

# 1. APPLICATION ARCHITECTURE DIAGRAM
fig, ax = plt.subplots(figsize=(12, 8), dpi=200)
ax.set_xlim(0, 12)
ax.set_ylim(0, 8.5)
ax.axis('off')

# Title
ax.text(6, 8.1, "KIEN TRUC TONG THE UNG DUNG BLOOM FLOWER CHAIN", 
        fontsize=14, fontweight='bold', ha='center', color='#1B365D')
ax.text(6, 7.8, "Spring Boot + Thymeleaf + Bootstrap + JPA + SQL Server + WebSocket + JWT + Cloudinary", 
        fontsize=9, fontstyle='italic', ha='center', color='#555555')

def draw_layer(ax, x, y, w, h, title, bg_color, border_color):
    rect = patches.FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.1", 
                                  facecolor=bg_color, edgecolor=border_color, linewidth=1.5)
    ax.add_patch(rect)
    ax.text(x + 0.2, y + h - 0.35, title, fontsize=9.5, fontweight='bold', color=border_color)

# Layer 1: Client Presentation
draw_layer(ax, 0.5, 6.4, 11, 1.1, "1. CLIENT PRESENTATION LAYER (Giao dien nguoi dung)", "#E8F4FD", "#0B5394")
clients = [
    ("Khach hang (Web/Mobile)", "Thymeleaf + Bootstrap 5 + SockJS", 0.8, 6.55, 3.2, 0.6),
    ("Quan ly chi nhanh (Manager)", "Dashboard KPI, Kho hoa, Xu ly don", 4.3, 6.55, 3.4, 0.6),
    ("Quan tri vien (Admin)", "Tong quan chuoi, Duyet shop, Thong ke", 8.0, 6.55, 3.2, 0.6)
]
for title, sub, cx, cy, cw, ch in clients:
    ax.add_patch(patches.FancyBboxPatch((cx, cy), cw, ch, boxstyle="round,pad=0.05", facecolor="#FFFFFF", edgecolor="#B0C4DE"))
    ax.text(cx + cw/2, cy + ch*0.6, title, fontsize=8, fontweight='bold', ha='center', color='#1B365D')
    ax.text(cx + cw/2, cy + ch*0.2, sub, fontsize=6.5, ha='center', color='#666666')

# Layer 2: Security & Gateway
draw_layer(ax, 0.5, 5.0, 11, 1.1, "2. SECURITY & AUTHENTICATION GATEWAY (Bao mat phan quyen kep)", "#FFF2CC", "#B45F06")
secs = [
    ("Web MVC Security Chain", "Session Cookie + CSRF Protection\nPhan quyen URL: ROLE_USER, ROLE_VENDOR, ROLE_ADMIN", 0.8, 5.15, 5.1, 0.65),
    ("REST API JWT Security Chain", "Stateless + Bearer Token + Token Rotation\nEndpoints: /api/auth/**, /api/**", 6.1, 5.15, 5.1, 0.65)
]
for title, sub, cx, cy, cw, ch in secs:
    ax.add_patch(patches.FancyBboxPatch((cx, cy), cw, ch, boxstyle="round,pad=0.05", facecolor="#FFFFFF", edgecolor="#E69138"))
    ax.text(cx + cw/2, cy + ch*0.65, title, fontsize=8, fontweight='bold', ha='center', color='#783F04')
    ax.text(cx + cw/2, cy + ch*0.25, sub, fontsize=6.5, ha='center', color='#555555')

# Layer 3: Controllers & Endpoints
draw_layer(ax, 0.5, 3.6, 11, 1.1, "3. CONTROLLER & DISPATCHER LAYER (Dieu huong & Nhan request)", "#E2F0D9", "#38761D")
ctrls = [
    ("Customer Controllers", "Home, Product, Cart,\nCheckout, Order", 0.8, 3.75, 2.4, 0.65),
    ("Manager Controllers", "Dashboard, Order, Product,\nStore, Staff, Wallet", 3.4, 3.75, 2.7, 0.65),
    ("Admin Controllers", "Dashboard, Stores, Users,\nCategories, Styles, Fees", 6.3, 3.75, 2.7, 0.65),
    ("WebSocket & API", "STOMP ChatController,\nJwtAuthController", 9.2, 3.75, 2.0, 0.65)
]
for title, sub, cx, cy, cw, ch in ctrls:
    ax.add_patch(patches.FancyBboxPatch((cx, cy), cw, ch, boxstyle="round,pad=0.05", facecolor="#FFFFFF", edgecolor="#93C47D"))
    ax.text(cx + cw/2, cy + ch*0.65, title, fontsize=7.5, fontweight='bold', ha='center', color='#274E13')
    ax.text(cx + cw/2, cy + ch*0.25, sub, fontsize=6.2, ha='center', color='#555555')

# Layer 4: Business Services & Logic
draw_layer(ax, 0.5, 2.2, 11, 1.1, "4. BUSINESS LOGIC & SERVICE LAYER (Xu ly nghiep vu chuoi)", "#FCE5CD", "#C65911")
svcs = [
    ("Order & Stock Service", "Tru kho tuc thoi, Hoan kho,\nKhoa bi quan (Pessimistic)", 0.8, 2.35, 2.5, 0.65),
    ("Store & Product Service", "Mo hinh chuoi, Mau chung/rieng,\nThuoc tinh hoa da tieu chi", 3.5, 2.35, 2.6, 0.65),
    ("Realtime & Media Service", "STOMP WebSocket chat,\nCloudinary image upload", 6.3, 2.35, 2.5, 0.65),
    ("Security & Support Service", "JWT Token rotation,\nMail OTP khong lo credential", 9.0, 2.35, 2.2, 0.65)
]
for title, sub, cx, cy, cw, ch in svcs:
    ax.add_patch(patches.FancyBboxPatch((cx, cy), cw, ch, boxstyle="round,pad=0.05", facecolor="#FFFFFF", edgecolor="#F6B26B"))
    ax.text(cx + cw/2, cy + ch*0.65, title, fontsize=7.5, fontweight='bold', ha='center', color='#7E22CE' if 'Security' in title else '#8A3B00')
    ax.text(cx + cw/2, cy + ch*0.25, sub, fontsize=6.2, ha='center', color='#555555')

# Layer 5: Data & Infrastructure
draw_layer(ax, 0.5, 0.4, 11, 1.5, "5. DATA PERSISTENCE & EXTERNAL INFRASTRUCTURE (Luu tru & He thong ngoai)", "#EDEDED", "#434343")
dbs = [
    ("Spring Data JPA & Hibernate", "20+ Entity quan he, Repositories,\nPessimistic Lock, Lazy Loading", 0.8, 0.55, 3.3, 0.95),
    ("Microsoft SQL Server (RentalShop)", "Flyway Migrations V1-V8, Schema dbo\nBang: stores, products, store_inventories,\norders, order_items, users, wallets", 4.3, 0.55, 4.0, 0.95),
    ("External Cloud Services", "Cloudinary (Media Storage)\nSMTP Mail (OTP Verification)\nMoMo / Bank Sandbox", 8.5, 0.55, 2.7, 0.95)
]
for title, sub, cx, cy, cw, ch in dbs:
    ax.add_patch(patches.FancyBboxPatch((cx, cy), cw, ch, boxstyle="round,pad=0.05", facecolor="#FFFFFF", edgecolor="#999999"))
    ax.text(cx + cw/2, cy + ch*0.7, title, fontsize=8, fontweight='bold', ha='center', color='#222222')
    ax.text(cx + cw/2, cy + ch*0.3, sub, fontsize=6.5, ha='center', color='#555555')

# Connective arrows
for y_conn in [6.4, 5.0, 3.6, 2.2]:
    ax.annotate("", xy=(6, y_conn), xytext=(6, y_conn + 0.15),
                arrowprops=dict(arrowstyle="->", color="#555555", lw=1.5))

plt.tight_layout()
plt.savefig('docs/diagrams/diagram_architecture.png', bbox_inches='tight')
plt.close()

# 2. ORDER & STOCK RESERVATION FLOWCHART
fig, ax = plt.subplots(figsize=(10, 8), dpi=200)
ax.set_xlim(0, 10)
ax.set_ylim(0, 10)
ax.axis('off')

ax.text(5, 9.6, "SO DO HOAT DONG: DAT HANG & TRU TON KHO TUC THOI (COD/ONLINE)", 
        fontsize=13, fontweight='bold', ha='center', color='#1B365D')

def draw_box(ax, x, y, w, h, text, bg, border, shape="rect"):
    if shape == "round":
        patch = patches.FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.15", facecolor=bg, edgecolor=border, lw=1.5)
    elif shape == "diamond":
        patch = patches.Polygon([[x+w/2, y], [x+w, y+h/2], [x+w/2, y+h], [x, y+h/2]], facecolor=bg, edgecolor=border, lw=1.5)
    else:
        patch = patches.FancyBboxPatch((x, y), w, h, boxstyle="square,pad=0.05", facecolor=bg, edgecolor=border, lw=1.5)
    ax.add_patch(patch)
    ax.text(x + w/2, y + h/2, text, fontsize=7.5, fontweight='bold', ha='center', va='center', color='#111111')

draw_box(ax, 3.5, 8.7, 3.0, 0.6, "Khach hang chon hoa & vao Checkout", "#E8F4FD", "#0B5394", "round")
draw_box(ax, 3.0, 7.6, 4.0, 0.7, "Chon hinh thuc nhan:\nGiao tan noi hoac Den lay tai quay", "#FFFFFF", "#0B5394")
draw_box(ax, 2.8, 6.4, 4.4, 0.8, "He thong xac dinh chi nhanh phuc vu\n(Uu tien chi nhanh gan nhat co du hang)", "#FFF2CC", "#B45F06")
draw_box(ax, 3.0, 5.0, 4.0, 0.9, "Pessimistic Write Lock:\nKiem tra so luong ton kho\nStoreInventory.quantity >= so luong mua?", "#E2F0D9", "#38761D", "diamond")

# Decision branches
draw_box(ax, 7.7, 5.15, 2.0, 0.6, "Thong bao: Het hang!\nKhoa dat hoa", "#FCE5CD", "#C65911")
draw_box(ax, 2.8, 3.7, 4.4, 0.8, "TRU TON KHO TUC THOI tai chi nhanh\n(Cap nhat store_inventories.quantity ngay,\nke ca don COD)", "#D9E1F2", "#2F5597")
draw_box(ax, 2.8, 2.5, 4.4, 0.7, "Luu Don Hang (Order) & Chi tiet (OrderItem)\nTrang thai: PENDING (Cho xac nhan)", "#E2F0D9", "#38761D")
draw_box(ax, 3.0, 1.4, 4.0, 0.7, "Gui thong bao WebSocket toi Quan ly chi nhanh\nva hien thi xac nhan cho Khach hang", "#E8F4FD", "#0B5394")
draw_box(ax, 3.5, 0.4, 3.0, 0.6, "HOAN TAT DAT HOA", "#D9EAD3", "#274E13", "round")

# Arrows
ax.annotate("", xy=(5, 8.3), xytext=(5, 8.7), arrowprops=dict(arrowstyle="->", lw=1.2))
ax.annotate("", xy=(5, 7.2), xytext=(5, 7.6), arrowprops=dict(arrowstyle="->", lw=1.2))
ax.annotate("", xy=(5, 5.9), xytext=(5, 6.4), arrowprops=dict(arrowstyle="->", lw=1.2))
ax.annotate("", xy=(7.7, 5.45), xytext=(7.0, 5.45), arrowprops=dict(arrowstyle="->", lw=1.2))
ax.text(7.2, 5.6, "KHONG", fontsize=7, fontweight='bold', color='red')
ax.annotate("", xy=(5, 4.5), xytext=(5, 5.0), arrowprops=dict(arrowstyle="->", lw=1.2))
ax.text(5.1, 4.7, "CO DU HANG", fontsize=7, fontweight='bold', color='green')
ax.annotate("", xy=(5, 3.2), xytext=(5, 3.7), arrowprops=dict(arrowstyle="->", lw=1.2))
ax.annotate("", xy=(5, 2.1), xytext=(5, 2.5), arrowprops=dict(arrowstyle="->", lw=1.2))
ax.annotate("", xy=(5, 1.0), xytext=(5, 1.4), arrowprops=dict(arrowstyle="->", lw=1.2))

plt.tight_layout()
plt.savefig('docs/diagrams/diagram_order_flow.png', bbox_inches='tight')
plt.close()

# 3. ORDER LIFECYCLE DIAGRAM
fig, ax = plt.subplots(figsize=(11, 6), dpi=200)
ax.set_xlim(0, 11)
ax.set_ylim(0, 6)
ax.axis('off')

ax.text(5.5, 5.6, "QUY TRINH VONG DOI DON HANG HOA TAI CHI NHANH (ORDER LIFECYCLE)", 
        fontsize=12, fontweight='bold', ha='center', color='#1B365D')

states = [
    ("PENDING\n(Cho xac nhan)", 0.6, 3.6, "#E8F4FD", "#0B5394"),
    ("CONFIRMED\n(Da xac nhan)", 2.8, 3.6, "#FFF2CC", "#B45F06"),
    ("PREPARING\n(Dang cam hoa)", 5.0, 3.6, "#FCE5CD", "#C65911"),
    ("SHIPPING\n(Dang giao hang)", 7.2, 3.6, "#D9E1F2", "#2F5597"),
    ("COMPLETED\n(Hoan tat don)", 9.3, 3.6, "#D9EAD3", "#274E13")
]
for text, sx, sy, bg, border in states:
    ax.add_patch(patches.FancyBboxPatch((sx, sy), 1.6, 1.0, boxstyle="round,pad=0.1", facecolor=bg, edgecolor=border, lw=1.5))
    ax.text(sx + 0.8, sy + 0.5, text, fontsize=7.5, fontweight='bold', ha='center', va='center', color=border)

# Regular progression arrows
for i in range(len(states)-1):
    x_from = states[i][1] + 1.6
    x_to = states[i+1][1]
    ax.annotate("", xy=(x_to, 4.1), xytext=(x_from, 4.1), arrowprops=dict(arrowstyle="->", lw=1.5, color="#333333"))

# Rule note: cannot cancel after PREPARING
ax.add_patch(patches.FancyBboxPatch((4.8, 4.8), 5.8, 0.6, boxstyle="round,pad=0.08", facecolor="#F8CECC", edgecolor="#B85450"))
ax.text(7.7, 5.1, "QUY TAC KHOA: Tu trang thai PREPARING (Cam hoa),\nKhach hang KHONG the tu y huy don!", 
        fontsize=7.5, fontweight='bold', ha='center', va='center', color='#900000')

# Cancel state
ax.add_patch(patches.FancyBboxPatch((3.5, 0.8), 4.0, 1.1, boxstyle="round,pad=0.1", facecolor="#F8CECC", edgecolor="#B85450", lw=1.5))
ax.text(5.5, 1.5, "CANCELLED (Don hang bi huy)", fontsize=8.5, fontweight='bold', ha='center', color='#900000')
ax.text(5.5, 1.1, "1. Hoan ton kho ve chi nhanh tren don dung 1 lan\n2. Quan ly huy ngoai le bat buoc nhap ly do (<=300 ky tu)", 
        fontsize=6.8, ha='center', color='#444444')

# Cancellation arrows
ax.annotate("Khach huy (Truoc cam hoa)", xy=(4.5, 1.9), xytext=(1.4, 3.6), 
            arrowprops=dict(arrowstyle="->", lw=1.2, color="#B85450", connectionstyle="arc3,rad=-0.2"), 
            fontsize=6.5, color="#B85450")
ax.annotate("Quan ly huy ngoai le (Ghi ly do)", xy=(6.5, 1.9), xytext=(5.8, 3.6), 
            arrowprops=dict(arrowstyle="->", lw=1.2, color="#B85450", connectionstyle="arc3,rad=0.2"), 
            fontsize=6.5, color="#B85450")

plt.tight_layout()
plt.savefig('docs/diagrams/diagram_order_lifecycle.png', bbox_inches='tight')
plt.close()

# 4. DUAL SECURITY ARCHITECTURE DIAGRAM
fig, ax = plt.subplots(figsize=(10, 6), dpi=200)
ax.set_xlim(0, 10)
ax.set_ylim(0, 6)
ax.axis('off')

ax.text(5, 5.6, "KIEN TRUC BAO MAT PHAN QUYEN KEP (DUAL SECURITY SYSTEM)", 
        fontsize=12, fontweight='bold', ha='center', color='#1B365D')

ax.add_patch(patches.FancyBboxPatch((3.5, 4.6), 3.0, 0.6, boxstyle="round,pad=0.1", facecolor="#E8F4FD", edgecolor="#0B5394", lw=1.5))
ax.text(5.0, 4.9, "HTTP Request Den Ung Dung", fontsize=8.5, fontweight='bold', ha='center', va='center', color='#0B5394')

# Left side: Web MVC Session
ax.add_patch(patches.FancyBboxPatch((0.5, 0.8), 4.2, 3.2, boxstyle="round,pad=0.1", facecolor="#FFF2CC", edgecolor="#B45F06", lw=1.5))
ax.text(2.6, 3.7, "PHAN HE WEB (Thymeleaf UI)", fontsize=9, fontweight='bold', ha='center', color='#783F04')
web_steps = [
    ("1. Request Web: /, /manager/**, /admin/**", 0.7, 3.0),
    ("2. Session Cookie Authentication (JSESSIONID)", 0.7, 2.5),
    ("3. CSRF Filter kiem tra token hop le", 0.7, 2.0),
    ("4. Phân quyen URL: ADMIN, VENDOR, USER", 0.7, 1.5),
    ("5. Render giao dien Thymeleaf HTML + Form", 0.7, 1.0)
]
for text, tx, ty in web_steps:
    ax.text(tx, ty, text, fontsize=7, color='#333333')

# Right side: REST API JWT
ax.add_patch(patches.FancyBboxPatch((5.3, 0.8), 4.2, 3.2, boxstyle="round,pad=0.1", facecolor="#E2F0D9", edgecolor="#38761D", lw=1.5))
ax.text(7.4, 3.7, "PHAN HE REST API (Stateless JWT)", fontsize=9, fontweight='bold', ha='center', color='#274E13')
api_steps = [
    ("1. Request API: /api/**", 5.5, 3.0),
    ("2. Header: Authorization: Bearer <token>", 5.5, 2.5),
    ("3. JwtTokenService xac thuc HS256 Secret", 5.5, 2.0),
    ("4. Kiem tra danh sach den & Refresh Rotation", 5.5, 1.5),
    ("5. Tra du lieu JSON ve ung dung di dong/client", 5.5, 1.0)
]
for text, tx, ty in api_steps:
    ax.text(tx, ty, text, fontsize=7, color='#333333')

# Connective arrows
ax.annotate("", xy=(2.6, 4.0), xytext=(4.3, 4.6), arrowprops=dict(arrowstyle="->", lw=1.5, color="#B45F06"))
ax.annotate("", xy=(7.4, 4.0), xytext=(5.7, 4.6), arrowprops=dict(arrowstyle="->", lw=1.5, color="#38761D"))

plt.tight_layout()
plt.savefig('docs/diagrams/diagram_dual_security.png', bbox_inches='tight')
plt.close()

# 5. WEBSOCKET REALTIME CHAT DIAGRAM
fig, ax = plt.subplots(figsize=(10, 5), dpi=200)
ax.set_xlim(0, 10)
ax.set_ylim(0, 5)
ax.axis('off')

ax.text(5, 4.6, "LUONG GIAO TIEP REALTIME CHAT QUA WEBSOCKET & STOMP", 
        fontsize=12, fontweight='bold', ha='center', color='#1B365D')

ax.add_patch(patches.FancyBboxPatch((0.5, 2.0), 2.2, 1.4, boxstyle="round,pad=0.1", facecolor="#E8F4FD", edgecolor="#0B5394", lw=1.5))
ax.text(1.6, 2.8, "KHACH HANG\n(Web Browser)", fontsize=8, fontweight='bold', ha='center', color='#0B5394')
ax.text(1.6, 2.3, "SockJS Client\nSTOMP Protocol", fontsize=6.8, ha='center', color='#555555')

ax.add_patch(patches.FancyBboxPatch((3.8, 1.5), 2.4, 2.4, boxstyle="round,pad=0.1", facecolor="#E2F0D9", edgecolor="#38761D", lw=1.5))
ax.text(5.0, 3.5, "SPRING BOOT\nMESSAGE BROKER", fontsize=8, fontweight='bold', ha='center', color='#274E13')
ax.text(5.0, 2.8, "Endpoint: /ws-chat\nPrefix: /app, /topic", fontsize=6.8, ha='center', color='#333333')
ax.text(5.0, 2.1, "ChatController luu\nvao SQL Server", fontsize=6.8, ha='center', color='#666666')

ax.add_patch(patches.FancyBboxPatch((7.3, 2.0), 2.2, 1.4, boxstyle="round,pad=0.1", facecolor="#FFF2CC", edgecolor="#B45F06", lw=1.5))
ax.text(8.4, 2.8, "QUAN LY CHI NHANH\n(Manager Dashboard)", fontsize=8, fontweight='bold', ha='center', color='#B45F06')
ax.text(8.4, 2.3, "Kenh rieng chi nhanh\nThong bao tuc thi", fontsize=6.8, ha='center', color='#555555')

# Arrows
ax.annotate("1. Gui tin: /app/chat.send", xy=(3.8, 3.1), xytext=(2.7, 3.1), arrowprops=dict(arrowstyle="->", lw=1.2, color="#0B5394"), fontsize=6.5)
ax.annotate("2. Phat song: /topic/messages.{id}", xy=(2.7, 2.2), xytext=(3.8, 2.2), arrowprops=dict(arrowstyle="->", lw=1.2, color="#38761D"), fontsize=6.5)
ax.annotate("3. Thong bao: /topic/store.{id}", xy=(7.3, 2.7), xytext=(6.2, 2.7), arrowprops=dict(arrowstyle="->", lw=1.2, color="#B45F06"), fontsize=6.5)

plt.tight_layout()
plt.savefig('docs/diagrams/diagram_websocket_chat.png', bbox_inches='tight')
plt.close()

print("All 5 diagrams generated successfully in docs/diagrams/")
