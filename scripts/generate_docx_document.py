import os
import docx
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

doc = Document()

# Set standard margins (1 inch = 72 pt)
for section in doc.sections:
    section.top_margin = Inches(0.8)
    section.bottom_margin = Inches(0.8)
    section.left_margin = Inches(0.9)
    section.right_margin = Inches(0.9)

# Color Palette Constants
COLOR_PRIMARY = RGBColor(27, 54, 93)     # Navy #1B365D
COLOR_SECONDARY = RGBColor(180, 95, 6)   # Amber #B45F06
COLOR_SUCCESS = RGBColor(46, 125, 50)    # Green #2E7D32
COLOR_DANGER = RGBColor(168, 40, 40)     # Red #A82828
COLOR_DARK = RGBColor(40, 40, 40)        # Dark Charcoal #282828
COLOR_MUTED = RGBColor(100, 100, 100)    # Muted Gray #646464

def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
    tcPr.append(tcMar)

def add_header_title(text, level=1):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(14)
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.keep_with_next = True
    run = p.add_run(text)
    run.font.name = 'Calibri'
    run.bold = True
    if level == 1:
        run.font.size = Pt(16)
        run.font.color.rgb = COLOR_PRIMARY
        # Add bottom border/accent effect via paragraph format
    elif level == 2:
        run.font.size = Pt(13)
        run.font.color.rgb = COLOR_SECONDARY
    else:
        run.font.size = Pt(11)
        run.font.color.rgb = COLOR_DARK
    return p

def add_callout(text, title="LUU Y QUAN TRONG / GHI CHU", bg_hex="F2F4F8", border_hex="1B365D"):
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl.autofit = False
    tbl.columns[0].width = Inches(6.5)
    cell = tbl.cell(0, 0)
    set_cell_background(cell, bg_hex)
    set_cell_margins(cell, top=120, bottom=120, left=180, right=180)
    
    # Left border
    tcPr = cell._tc.get_or_add_tcPr()
    borders = parse_xml(f'<w:tcBorders {nsdecls("w")}><w:left w:val="single" w:sz="24" w:space="0" w:color="{border_hex}"/><w:top w:val="none"/><w:right w:val="none"/><w:bottom w:val="none"/></w:tcBorders>')
    tcPr.append(borders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(2)
    r_title = p.add_run(f"📌 {title}\n")
    r_title.bold = True
    r_title.font.size = Pt(9.5)
    r_title.font.color.rgb = COLOR_PRIMARY
    
    r_text = p.add_run(text)
    r_text.font.size = Pt(9)
    r_text.font.color.rgb = COLOR_DARK
    doc.add_paragraph().paragraph_format.space_after = Pt(4)

def add_code_block(code_text, language="Java"):
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl.autofit = False
    tbl.columns[0].width = Inches(6.5)
    cell = tbl.cell(0, 0)
    set_cell_background(cell, "F8F9FA")
    set_cell_margins(cell, top=100, bottom=100, left=150, right=150)
    
    tcPr = cell._tc.get_or_add_tcPr()
    borders = parse_xml(f'<w:tcBorders {nsdecls("w")}><w:left w:val="single" w:sz="16" w:space="0" w:color="CCCCCC"/><w:top w:val="single" w:sz="4" w:space="0" w:color="E0E0E0"/><w:right w:val="single" w:sz="4" w:space="0" w:color="E0E0E0"/><w:bottom w:val="single" w:sz="4" w:space="0" w:color="E0E0E0"/></w:tcBorders>')
    tcPr.append(borders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(0)
    r_lang = p.add_run(f"// --- {language} Code Snippet ---\n")
    r_lang.font.name = 'Consolas'
    r_lang.font.size = Pt(8)
    r_lang.font.color.rgb = COLOR_MUTED
    
    r_code = p.add_run(code_text)
    r_code.font.name = 'Consolas'
    r_code.font.size = Pt(8.5)
    r_code.font.color.rgb = COLOR_DARK
    doc.add_paragraph().paragraph_format.space_after = Pt(4)

# ==================== TRANG BIA ====================
p_pre = doc.add_paragraph()
p_pre.alignment = WD_ALIGN_PARAGRAPH.CENTER
p_pre.paragraph_format.space_before = Pt(36)
p_pre.paragraph_format.space_after = Pt(4)
r_pre = p_pre.add_run("TRUONG DAI HOC - KHOA CONG NGHE THONG TIN\nBO MON LAP TRINH WEB")
r_pre.bold = True
r_pre.font.size = Pt(11)
r_pre.font.color.rgb = COLOR_MUTED

p_title = doc.add_paragraph()
p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
p_title.paragraph_format.space_before = Pt(24)
p_title.paragraph_format.space_after = Pt(8)
r_title = p_title.add_run("BAO CAO KIEN TRUC VA HUONG DAN XAY DUNG\nUNG DUNG BAN HANG THEO MO HINH CHUOI CUA HANG\n(BLOOM FLOWER CHAIN)")
r_title.bold = True
r_title.font.size = Pt(18)
r_title.font.color.rgb = COLOR_PRIMARY

p_sub = doc.add_paragraph()
p_sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
p_sub.paragraph_format.space_after = Pt(24)
r_sub = p_sub.add_run("Cong nghe: Spring Boot + Thymeleaf + Bootstrap + JPA + SQL Server + WebSocket + JWT + Cloudinary\nHo tro nghiem thu va bao ve do an mon hoc")
r_sub.font.size = Pt(10.5)
r_sub.font.italic = True
r_sub.font.color.rgb = COLOR_SECONDARY

# Meta info box
tbl_meta = doc.add_table(rows=4, cols=2)
tbl_meta.alignment = WD_TABLE_ALIGNMENT.CENTER
tbl_meta.autofit = False
tbl_meta.columns[0].width = Inches(2.2)
tbl_meta.columns[1].width = Inches(4.0)

meta_data = [
    ("Sinh vien / Nhom thuc hien:", "Nhom phat trien ung dung Chuoi cua hang hoa tuoi"),
    ("Ten de tai:", "Xay dung website ban hang theo mo hinh chuoi cua hang (100% Ban hoa - Khong cho thue)"),
    ("Nen tang cong nghe:", "Java 21, Spring Boot 4.1.1, Thymeleaf, Bootstrap 5.3.3, SQL Server, Flyway V1-V8"),
    ("Phuong thuc phat trien:", "Co su ho tro cua Tri tue nhan tao (AI-Assisted Development)")
]
for row_idx, (k, v) in enumerate(meta_data):
    cell_k = tbl_meta.cell(row_idx, 0)
    cell_v = tbl_meta.cell(row_idx, 1)
    set_cell_background(cell_k, "F2F4F8")
    set_cell_background(cell_v, "FFFFFF")
    set_cell_margins(cell_k, 60, 60, 100, 100)
    set_cell_margins(cell_v, 60, 60, 100, 100)
    pk = cell_k.paragraphs[0]
    rk = pk.add_run(k)
    rk.bold = True
    rk.font.size = Pt(9.5)
    pv = cell_v.paragraphs[0]
    rv = pv.add_run(v)
    rv.font.size = Pt(9.5)

doc.add_page_break()

# ==================== PHAN 1: KIEN TRUC UNG DUNG ====================
add_header_title("PHAN 1: KIEN TRUC TONG THE UNG DUNG (APPLICATION ARCHITECTURE)", level=1)

p_intro = doc.add_paragraph()
p_intro.paragraph_format.space_after = Pt(6)
p_intro.add_run("He thong website ban hang theo mo hinh chuoi cua hang duoc thiet ke theo kien truc phan tang tieu chuan (N-Tier Enterprise Architecture) dua tren nen tang Spring Boot 4.x va Java 21 LTS. Kien truc dam bao tinh tach bach giua tang bieu dien (Presentation), tang dieu huong (Controller), tang xu ly nghiep vu (Business Service), tang truy cap du lieu (Data Persistence) va cac dich vu ha tang dam may ngoai vi (Cloud Services).")

# Insert Architecture Diagram
arch_diag_path = 'docs/diagrams/diagram_architecture.png'
if os.path.exists(arch_diag_path):
    doc.add_picture(arch_diag_path, width=Inches(6.5))
    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_after = Pt(10)
    r_cap = p_cap.add_run("Hinh 1.1: So do kien truc tong the 5 tang cua he thong Bloom Flower Chain")
    r_cap.font.size = Pt(8.5)
    r_cap.font.italic = True
    r_cap.font.color.rgb = COLOR_MUTED

add_header_title("1.1. Dien giai chi tiet cac tang trong kien truc", level=2)

layers_desc = [
    ("Tang 1: Client Presentation Layer", "Bao gom giao dien web phuc vu Khach hang, Quan ly chi nhanh (Manager) va Quan tri vien (Admin). Su dung Thymeleaf ket hop cung Bootstrap 5.3.3 de tao giao dien responsive linh hoat. Giao tiep qua HTTP Request/Response (form submit/AJAX) va SockJS/STOMP danh cho cac tinh nang thoi gian thuc nhu chat va thong bao."),
    ("Tang 2: Security & Authentication Gateway", "Ap dung co che bao mat phan quyen kep doc lap: (1) Web Security Chain su dung Session Cookie va ma CSRF bao ve form web; (2) REST API Security Chain su dung co che Stateless, xac thuc qua Bearer JWT Token voi khoa bi mat HS256 va co che xoay Refresh Token."),
    ("Tang 3: Controller & Dispatcher Layer", "Dieu huong request theo tung phan he nguoi dung: CustomerController (tim kiem, gio hang, checkout), ManagerController (KPIs, xu ly don, kho hang), AdminController (quan ly toan chuoi, duyet cua hang, cau hinh), WebSocket ChatController va JwtAuthController."),
    ("Tang 4: Business Logic & Service Layer", "Thuc thi toan bo logic nghiep vu: Tru ton kho ngay khi dat hang (ke ca COD) voi khoa bi quan; hoan kho dung 1 lan khi huy; xep lich cam hoa theo tien do; tinh toan cuoc phi giao hoa; tich hop upload anh Cloudinary va gui ma OTP qua Email."),
    ("Tang 5: Data Persistence & External Infrastructure", "Su dung Spring Data JPA va Hibernate quan ly hon 20 thuc the CSDL. He quan tri CSDL Microsoft SQL Server (database RentalShop) duoc kiem soat phien ban chat che bang Flyway Migration tu V1 den V8. Ket hop Cloudinary CDN luu tru hinh anh.")
]

for title, desc in layers_desc:
    p_l = doc.add_paragraph()
    p_l.paragraph_format.space_after = Pt(4)
    r_lt = p_l.add_run(f"• {title}: ")
    r_lt.bold = True
    r_lt.font.color.rgb = COLOR_PRIMARY
    p_l.add_run(desc)

# ==================== PHAN 2: SO DO HOAT DONG & DIEN GIAI SOURCE CODE ====================
doc.add_page_break()
add_header_title("PHAN 2: SO DO HOAT DONG VA DIEN GIAI SOURCE CODE CAC CHUC NANG CHINH", level=1)

# Chuc nang 1: Mo hinh chuoi & Ton kho
add_header_title("2.1. Chuc nang quan ly chuoi chi nhanh & Ton kho doc lap", level=2)
p_c1 = doc.add_paragraph()
p_c1.add_run("Moi chi nhanh (Store) hoat dong nhu mot diem ban doc lap co dia chi, hotline va kho hang rieng. San pham duoc chia lam 2 loai: Mau chung toan chuoi (store=null) va Mau rieng cua tung chi nhanh (store!=null). So luong ton kho duoc luu tru tai bang `store_inventories` voi moi quan he Store-Product.")

add_code_block("""@Entity
@Table(name = "store_inventories", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"store_id", "product_id"})
})
public class StoreInventory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity; // So luong ton kho thuc te tai chi nhanh

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // Trang thai kinh doanh tai chi nhanh
}""", language="Java - StoreInventory.java")

# Chuc nang 2: Dat hang & Tru ton kho tuc thoi
add_header_title("2.2. Chuc nang Dat hang & Tru ton kho tuc thoi (Stock Reservation)", level=2)
p_c2 = doc.add_paragraph()
p_c2.add_run("Quy tac nghiep vu: Don hang dat thanh cong se TRU TON KHO NGAY tai chi nhanh xu ly, ke ca thanh toan khi nhan hang (COD). De chong hien tuong ban vuot ton kho (race condition) khi nhieu khach hang cung mua 1 bong hoa cuoi cung, he thong ap dung Khoa bi quan (Pessimistic Write Lock).")

order_flow_path = 'docs/diagrams/diagram_order_flow.png'
if os.path.exists(order_flow_path):
    doc.add_picture(order_flow_path, width=Inches(6.0))
    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_after = Pt(8)
    r_cap = p_cap.add_run("Hinh 2.1: So do hoat dong luong dat hang va tru ton kho tuc thoi")
    r_cap.font.size = Pt(8.5)
    r_cap.font.italic = True
    r_cap.font.color.rgb = COLOR_MUTED

add_code_block("""@Transactional(isolation = Isolation.REPEATABLE_READ)
public Order placeOrder(CheckoutRequest request, User customer) {
    Store store = resolveFulfillmentStore(request);
    
    for (CartItem item : request.getItems()) {
        // Khoa ban ghi ton kho de tranh race condition
        StoreInventory inventory = inventoryRepository
            .findWithPessimisticLock(store.getId(), item.getProduct().getId())
            .orElseThrow(() -> new OutOfStockException("San pham khong co tai chi nhanh!"));

        if (inventory.getQuantity() < item.getQuantity()) {
            throw new InsufficientStockException("Ton kho khong du de phuc vu don hang!");
        }

        // Tru ton kho ngay lap tuc
        inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
        inventoryRepository.save(inventory);
    }
    // Khoi tao va luu Don hang o trang thai PENDING...
    return orderRepository.save(newOrder);
}""", language="Java - OrderServiceImpl.java (Stock Reservation)")

# Chuc nang 3: Vong doi don hang
doc.add_page_break()
add_header_title("2.3. Quy trinh xu ly vong doi don hang hoa tai chi nhanh", level=2)
p_c3 = doc.add_paragraph()
p_c3.add_run("Tien trinh xu ly don hoa trai qua 6 buoc tuan tu: Cho xac nhan (PENDING) -> Da xac nhan (CONFIRMED) -> Dang cam hoa (PREPARING) -> Dang giao (SHIPPING) -> Da giao (DELIVERED) -> Hoan tat (COMPLETED). Dac biet, he thong cai dat quy tac khoa: Tu thoi diem bat dau cam hoa, khach hang bi khoa nut huy don de tranh ton that chi phi hoa tuoi.")

order_cycle_path = 'docs/diagrams/diagram_order_lifecycle.png'
if os.path.exists(order_cycle_path):
    doc.add_picture(order_cycle_path, width=Inches(6.0))
    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_after = Pt(8)
    r_cap = p_cap.add_run("Hinh 2.2: So do vong doi don hang va quy tac khoa huy don")
    r_cap.font.size = Pt(8.5)
    r_cap.font.italic = True
    r_cap.font.color.rgb = COLOR_MUTED

add_code_block("""@PostMapping("/manager/orders/{id}/cancel-exception")
public String cancelOrderException(@PathVariable Long id, 
                                   @RequestParam String cancelReason,
                                   @AuthenticationPrincipal UserPrincipal user,
                                   RedirectAttributes ra) {
    if (cancelReason == null || cancelReason.trim().isEmpty() || cancelReason.length() > 300) {
        ra.addFlashAttribute("error", "Ly do huy bat buoc va toi da 300 ky tu!");
        return "redirect:/manager/orders/" + id;
    }
    orderService.cancelOrderWithReason(id, cancelReason, user.getUsername());
    ra.addFlashAttribute("success", "Da huy don ngoai le va hoan ton kho ve chi nhanh.");
    return "redirect:/manager/orders";
}""", language="Java - ManagerOrderController.java")

# Chuc nang 4: Bao mat phan quyen kep
add_header_title("2.4. Kien truc bao mat phan quyen kep (Dual Security System)", level=2)
p_c4 = doc.add_paragraph()
p_c4.add_run("Dap ung tieu chi vua phuc vu nguoi dung web (Thymeleaf UI) vua cung cap API cho ung dung ben ngoai (REST Client), he thong thiet lap 2 Filter Chain doc lap trong Spring Security.")

dual_sec_path = 'docs/diagrams/diagram_dual_security.png'
if os.path.exists(dual_sec_path):
    doc.add_picture(dual_sec_path, width=Inches(5.8))
    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_after = Pt(8)
    r_cap = p_cap.add_run("Hinh 2.3: So do phan luong bao mat Session Cookie va Stateless JWT")
    r_cap.font.size = Pt(8.5)
    r_cap.font.italic = True
    r_cap.font.color.rgb = COLOR_MUTED

add_code_block("""@Configuration
@EnableWebSecurity
public class DualSecurityConfig {
    // Chain 1: REST API Stateless JWT
    @Bean @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
            .build();
    }

    // Chain 2: Web MVC Session + CSRF
    @Bean @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/manager/**").hasAnyRole("VENDOR", "ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll())
            .formLogin(form -> form.loginPage("/auth/login").permitAll())
            .build();
    }
}""", language="Java - DualSecurityConfig.java")

# Chuc nang 5: WebSocket Chat
doc.add_page_break()
add_header_title("2.5. Giao tiep thoi gian thuc qua WebSocket & STOMP", level=2)
p_c5 = doc.add_paragraph()
p_c5.add_run("Tinh nang chat truc tiep giua Khach hang va Chi nhanh cua hang su dung giao thuc STOMP tren nen WebSocket, kem theo co che fallback SockJS khi trinh duyet khong ho tro.")

chat_path = 'docs/diagrams/diagram_websocket_chat.png'
if os.path.exists(chat_path):
    doc.add_picture(chat_path, width=Inches(5.8))
    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_after = Pt(8)
    r_cap = p_cap.add_run("Hinh 2.4: So do luong tin nhan chat thoi gian thuc giua Khach hang va Cua hang")
    r_cap.font.size = Pt(8.5)
    r_cap.font.italic = True
    r_cap.font.color.rgb = COLOR_MUTED

# Chuc nang 6: Bao toan thuoc tinh cu khi sua san pham
add_header_title("2.6. Chuc nang bao toan thuoc tinh san pham va Dropdown chon tag", level=2)
p_c6 = doc.add_paragraph()
p_c6.add_run("Giai quyet triet de loi mat thuoc tinh cu khi sua san pham: Chuan hoa alias giua 'Phong cach' va 'Phong cach thiet ke', bo sung ham getSelectedStyleValueIds() giup form render dung tag va khong ghi de rong.")

add_code_block("""// ProductAttributeService.java - Bao toan thuoc tinh cu khi khong gui tham so
public void assignAttributes(Product product, List<Long> styleValueIds, Map<String, String> textAttributes) {
    Set<StyleValue> selected = new HashSet<>();
    
    // Giu lai cac thuoc tinh cu thuoc nhom khong duoc gui trong request
    for (StyleValue existing : product.getStyleValues()) {
        String groupName = existing.getStyle().getName();
        if (!textAttributes.containsKey(groupName) && !isGroupSubmitted(groupName)) {
            selected.add(existing);
        }
    }
    // Ghep them cac thuoc tinh moi duoc chon tu form dropdown
    selected.addAll(resolveSelectedValues(styleValueIds, textAttributes));
    product.setStyleValues(selected);
}""", language="Java - ProductAttributeService.java (Attribute Safeguard)")

# ==================== PHAN 3: DIEN GIAI GIAO DIEN UNG DUNG ====================
doc.add_page_break()
add_header_title("PHAN 3: DIEN GIAI GIAO DIEN UNG DUNG (USER INTERFACE BREAKDOWN)", level=1)

p_ui = doc.add_paragraph()
p_ui.add_run("Giao dien website duoc xay dung dua tren Bootstrap 5.3.3 voi gam mau chu dao Kem Hong Am (#FFF0F3) ket hop Xanh Rung Tu nhien (#1B365D / #2E7D32), mang lai cam giac sang trong, tuoi moi phu hop voi chuoi hoa tuoi cao cap.")

# Screenshot 1: Product Edit Screen
ui_img1 = r'C:\Users\lenovo\.gemini\antigravity-ide\brain\9168fc96-be72-40c3-95d5-99bcb2c9688a\.user_uploaded\media_1792075075870.png'
if os.path.exists(ui_img1):
    add_header_title("3.1. Giao dien Chinh sua thong tin & Thuoc tinh mau hoa", level=2)
    doc.add_picture(ui_img1, width=Inches(6.0))
    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_after = Pt(6)
    r_cap = p_cap.add_run("Hinh 3.1: Man hinh Chinh sua san pham (#40) voi cac nhom thuoc tinh hoa truc quan")
    r_cap.font.size = Pt(8.5)
    r_cap.font.italic = True
    r_cap.font.color.rgb = COLOR_MUTED
    
    p_desc = doc.add_paragraph()
    p_desc.add_run("• Dien giai thanh phan: (1) O nhap Gia ban va Gia khuyen mai tinh theo dong nguyen tu 1d; (2) Khu vuc 'Thuoc tinh hoa (khong bat buoc)' chia lam 3 nhom: Loai hoa, Mau sac, Phong cach thiet ke; (3) Cac tag duoc bao toan tu CSDL (Hoa Hong, Cam Tu Cau, Hong); (4) O nhap anh Cloudinary ho tro ca tai file truc tiep va nhap duong dan URL.")

# Screenshot 2: Dropdown Tag Screen
ui_img2 = r'C:\Users\lenovo\.gemini\antigravity-ide\brain\9168fc96-be72-40c3-95d5-99bcb2c9688a\.user_uploaded\media_1792057867817.png'
if os.path.exists(ui_img2):
    add_header_title("3.2. Giao dien Nut bam so xuong (Dropdown) chon thuoc tinh hoa", level=2)
    doc.add_picture(ui_img2, width=Inches(5.8))
    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_after = Pt(6)
    r_cap = p_cap.add_run("Hinh 3.2: Dropdown menu chon nhanh danh muc phu va cac dac tinh hoa khong can go tay")
    r_cap.font.size = Pt(8.5)
    r_cap.font.italic = True
    r_cap.font.color.rgb = COLOR_MUTED

# Screenshot 3: Order Management Screen
ui_img3 = r'C:\Users\lenovo\.gemini\antigravity-ide\brain\9168fc96-be72-40c3-95d5-99bcb2c9688a\.user_uploaded\media_1792054981573.png'
if os.path.exists(ui_img3):
    add_header_title("3.3. Giao dien Quan ly don hang tai chi nhanh theo tien do", level=2)
    doc.add_picture(ui_img3, width=Inches(6.0))
    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_after = Pt(6)
    r_cap = p_cap.add_run("Hinh 3.3: Danh sach don hang phan theo tab: Cho xac nhan, Cam hoa, Dang giao, Hoan tat")
    r_cap.font.size = Pt(8.5)
    r_cap.font.italic = True
    r_cap.font.color.rgb = COLOR_MUTED

# Screenshot 4: Dashboard KPI
ui_img4 = r'C:\Users\lenovo\.gemini\antigravity-ide\brain\9168fc96-be72-40c3-95d5-99bcb2c9688a\.user_uploaded\media_1792050377060.png'
if os.path.exists(ui_img4):
    add_header_title("3.4. Giao dien Dashboard chi nhanh voi 4 the KPI tren 1 hang", level=2)
    doc.add_picture(ui_img4, width=Inches(6.0))
    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_after = Pt(6)
    r_cap = p_cap.add_run("Hinh 3.4: Dashboard quan ly voi 4 the chi so KPI dong bo can doi va bieu do doanh thu")
    r_cap.font.size = Pt(8.5)
    r_cap.font.italic = True
    r_cap.font.color.rgb = COLOR_MUTED

# ==================== PHAN 4: HUONG DAN SETUP UNG DUNG ====================
doc.add_page_break()
add_header_title("PHAN 4: HUONG DAN CAI DAT VA VAN HANH UNG DUNG (SETUP GUIDE)", level=1)

p_setup_intro = doc.add_paragraph()
p_setup_intro.add_run("De giup Giang vien va Hoi dong cham thi co the khoi chay he thong nhanh chong tren moi truong may tinh ca nhan (Windows), du an da duoc dong goi kem san bo cong cu script tu dong va Maven Wrapper.")

add_header_title("4.1. Yeu cau he thong (Prerequisites)", level=2)
prereqs = [
    ("He dieu hanh:", "Windows 10 hoac Windows 11 64-bit."),
    ("Java Development Kit:", "JDK 21 LTS (Da tich hop ban portable trong thu muc .tools, khong can cai them)."),
    ("Co so du lieu:", "Microsoft SQL Server 2019 tro len (Instance: SQLEXPRESS, database: RentalShop)."),
    ("Trinh duyet web:", "Google Chrome, Microsoft Edge hoac Mozilla Firefox.")
]
tbl_pre = doc.add_table(rows=len(prereqs), cols=2)
tbl_pre.alignment = WD_TABLE_ALIGNMENT.CENTER
for idx, (k, v) in enumerate(prereqs):
    c1, c2 = tbl_pre.cell(idx, 0), tbl_pre.cell(idx, 1)
    c1.width, c2.width = Inches(2.2), Inches(4.3)
    set_cell_background(c1, "F2F4F8")
    set_cell_margins(c1, 40, 40, 80, 80)
    set_cell_margins(c2, 40, 40, 80, 80)
    c1.paragraphs[0].add_run(k).bold = True
    c2.paragraphs[0].add_run(v)

add_header_title("4.2. Cau hinh bien moi truong (.env)", level=2)
p_env = doc.add_paragraph()
p_env.add_run("He thong tu dong doc cac bien Cloudinary va JWT tu file `.env` tai thu muc goc cua du an:")

add_code_block("""# File .env tai thu muc goc du an
CLOUDINARY_CLOUD_NAME=rbw3u9td
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# Khoa bi mat JWT (toi thieu 32 byte Base64)
JWT_SECRET=c2VjdXJlX2Jsb29tX2Zsb3dlcl9jaGFpbl9qd3Rfc2VjcmV0X2tleV8yMDI2
SPRING_PROFILES_ACTIVE=dev""", language="Properties - .env")

add_header_title("4.3. Lenh khoi chay ung dung", level=2)
p_run = doc.add_paragraph()
p_run.add_run("Mo terminal PowerShell tai thu muc goc cua du an va thuc thi 1 trong 2 lenh sau:")

add_code_block("""# Cach 1 (Khuyen nghi): Khoi chay qua script tich hop tu dong doc SQL Server
.\\scripts\\sqlserver.ps1

# Cach 2: Khoi chay truc tiep qua Maven Wrapper voi profile dev
.\\scripts\\maven.ps1 spring-boot:run""", language="PowerShell")

p_open = doc.add_paragraph()
p_open.add_run("Sau khi Spring Boot khoi dong thanh cong (Tomcat started on port 8080), mo trinh duyet truy cap:\n👉 ")
r_url = p_open.add_run("http://localhost:8080")
r_url.bold = True
r_url.font.color.rgb = COLOR_PRIMARY

add_header_title("4.4. Danh sach tai khoan thu nghiem theo cac vai tro", level=2)
accounts = [
    ("1. Khach vang lai (Guest)", "(Khong can dang nhap)", "-", "http://localhost:8080/"),
    ("2. Nguoi mua (Customer)", "khachhang", "123456", "http://localhost:8080/"),
    ("3. Quan ly chi nhanh (Manager)", "manager_q1", "123456", "http://localhost:8080/manager/dashboard"),
    ("4. Quan tri vien (Admin)", "admin", "123456", "http://localhost:8080/admin/dashboard")
]
tbl_acc = doc.add_table(rows=len(accounts)+1, cols=4)
tbl_acc.alignment = WD_TABLE_ALIGNMENT.CENTER
headers = ["Vai tro (Role)", "Ten dang nhap", "Mat khau", "Trang dich mac dinh"]
for i, h in enumerate(headers):
    cell = tbl_acc.cell(0, i)
    set_cell_background(cell, "1B365D")
    set_cell_margins(cell, 60, 60, 80, 80)
    p = cell.paragraphs[0]
    r = p.add_run(h)
    r.bold = True
    r.font.color.rgb = RGBColor(255, 255, 255)

for r_idx, acc in enumerate(accounts):
    for c_idx in range(4):
        cell = tbl_acc.cell(r_idx+1, c_idx)
        set_cell_background(cell, "FFFFFF" if r_idx % 2 == 0 else "F8F9FA")
        set_cell_margins(cell, 50, 50, 80, 80)
        p = cell.paragraphs[0]
        r = p.add_run(acc[c_idx])
        if c_idx == 0:
            r.bold = True

# ==================== PHAN 5: DANH SACH PROMPT HO TRO BOI AI ====================
doc.add_page_break()
add_header_title("PHAN 5: DANH SACH CAC PROMPT NHOM DA SU DUNG DE AI HO TRO (2.0 DIEM)", level=1)

p_prompt_intro = doc.add_paragraph()
p_prompt_intro.add_run("Duoi day la bang tong hop toan bo cac prompt chinh ma nhom da su dung trong qua trinh phat trien du an voi su ho tro cua Tro ly Tri tue nhan tao (AI - Antigravity / Gemini), phuc vu tieu chi cham diem danh gia ky nang Prompt Engineering cua Giang vien:")

prompts_data = [
    ("Giai doan 1: Khoi tao du an", 
     "Thiet ke kien truc Spring Boot 4.x, Java 21, Thymeleaf, Bootstrap 5, JPA, SQL Server, WebSocket, JWT, Cloudinary. Phan chia package chuan MVC.",
     "AI sinh file pom.xml, khoi tao cau truc thu muc 3 lop, tao cac entity quan he Store, Product, StoreInventory va migration Flyway V1-V8."),
    
    ("Giai doan 2: Bao mat phan quyen kep",
     "Tao Dual Security: Web dung Session + CSRF, con API dung Bearer JWT Stateless voi Token Rotation. Gui ma OTP qua email, khong in ra console.",
     "AI viet JwtSecurityConfig, JwtTokenService, JwtAuthController va OtpService gui mail an toan."),
    
    ("Giai doan 3: Dat hang & Tru ton kho",
     "Dat hang COD phai tru ton kho ngay lap tuc tai chi nhanh. Tranh race condition khi 2 nguoi mua dong thoi bong hoa cuoi cung.",
     "AI cai dat Pessimistic Write Lock tren StoreInventory trong transaction REPEATABLE_READ va co che reconcileInventoryOnCancel()."),
    
    ("Giai doan 4: Vong doi don hang hoa",
     "Khi cua hang chuyen sang Dang cam hoa (PREPARING), khoa nut huy cua khach. Quan ly huy bat buoc nhap ly do <= 300 ky tu.",
     "AI cap nhat OrderStatus, kiem tra dieu kien tai Backend va tao modal popup xac nhan kem ghi log ly do huy vao CSDL."),
    
    ("Giai doan 5: Realtime Chat & Cloudinary",
     "Cau hinh WebSocket STOMP /ws-chat de chat thoi gian thuc theo chi nhanh. Upload anh Cloudinary lay API key tu file .env, khong hardcode.",
     "AI tao WebSocketConfig, ChatController va CloudinaryService doc tu file bien moi truong an toan."),
    
    ("Giai doan 6: Tinh chinh giao dien KPI",
     "Cho cac KPIs nay nam cung 1 hang tren Dashboard quan ly.",
     "AI viet lai CSS Grid va Flexbox responsive tren manager/dashboard.html va admin/dashboard.html."),
    
    ("Giai doan 7: Dat lai ten san pham",
     "Dat lai ten toan bo san pham cua cua hang cho dung va de hieu, khong dat ten tru tuong nhu 'Bo Hoa Huong Duong Anh Duong Rang Ro'.",
     "AI viet script cap nhat CSDL doi toan bo ten sang ten thuc te: 'Bo hoa huong duong', 'Bo hoa hong do tuoi'..."),
    
    ("Giai doan 8: Dropdown chon thuoc tinh",
     "Loi khi them san pham, toi muon no co nut bam so xuong de co the lua chon thuoc tinh hoa.",
     "AI viet component flower-attributes.html va JS ho tro dropdown so xuong chon nhanh tag Loai hoa, Mau sac, Phong cach, Dip tang."),
    
    ("Giai doan 9: Sua loi upload anh 413",
     "Khi them san pham bi loi 413 FileCountLimitExceededException.",
     "AI toi uu hoa multipart resolver, loai bo cac input file trung lap, cho phep upload anh on dinh."),
    
    ("Giai doan 10: Bao toan thuoc tinh cu",
     "Ban giup toi, sau khi toi sua cac thuoc tinh thi cac thuoc tinh cu cua toi bi mat di.",
     "AI phan tich lech alias 'Phong cach' vs 'Phong cach thiet ke', sua ProductAttributeService giu nguyen gia tri cu va them getSelectedStyleValueIds()."),
    
    ("Giai doan 11: Don dep du an & Sinh hinh",
     "Xoa toan bo cac file khong can den trong du an va tu dong tao trong qua trinh bien dich. Sinh hinh so do kien truc cho bao cao.",
     "AI chay mvn clean xoa target/, xoa .tmp/, viet script Python sinh 5 so do kien truc chat luong cao va tao file bao cao .docx nay.")
]

tbl_pr = doc.add_table(rows=len(prompts_data)+1, cols=3)
tbl_pr.alignment = WD_TABLE_ALIGNMENT.CENTER
pr_headers = ["Giai doan / Muc tieu", "Noi dung Prompt yeu cau AI", "Ket qua AI ho tro sinh ma nguon / giai phap"]
widths = [Inches(1.5), Inches(2.5), Inches(2.5)]

for i, h in enumerate(pr_headers):
    cell = tbl_pr.cell(0, i)
    cell.width = widths[i]
    set_cell_background(cell, "1B365D")
    set_cell_margins(cell, 60, 60, 80, 80)
    p = cell.paragraphs[0]
    r = p.add_run(h)
    r.bold = True
    r.font.size = Pt(9)
    r.font.color.rgb = RGBColor(255, 255, 255)

for r_idx, (phase, prompt, result) in enumerate(prompts_data):
    row_cells = [tbl_pr.cell(r_idx+1, c) for c in range(3)]
    for c, cell in enumerate(row_cells):
        cell.width = widths[c]
        set_cell_background(cell, "FFFFFF" if r_idx % 2 == 0 else "F8F9FA")
        set_cell_margins(cell, 50, 50, 80, 80)
        p = cell.paragraphs[0]
        text_val = [phase, prompt, result][c]
        r = p.add_run(text_val)
        r.font.size = Pt(8.5)
        if c == 0:
            r.bold = True
            r.font.color.rgb = COLOR_PRIMARY

# Save the document
output_path = 'docs/HUONG_DAN_XAY_DUNG_VA_KIEN_TRUC_UNG_DUNG.docx'
doc.save(output_path)
print(f"Document successfully created at {output_path} (size: {os.path.getsize(output_path)} bytes)")
