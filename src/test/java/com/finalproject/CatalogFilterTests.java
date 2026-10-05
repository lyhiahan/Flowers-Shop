package com.finalproject;

import com.finalproject.entity.Category;
import com.finalproject.entity.Product;
import com.finalproject.repository.CategoryRepository;
import com.finalproject.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CatalogFilterTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired private com.finalproject.repository.StoreRepository storeRepository;
    @Autowired private com.finalproject.repository.StyleRepository styles;
    @Autowired private com.finalproject.repository.StyleValueRepository values;
    @BeforeEach
    void setupTestData() {
        Category cat = categoryRepository.save(Category.builder()
                .name("Hoa sinh nhật")
                .description("Các mẫu hoa sinh nhật đẹp")
                .build());

        var store = storeRepository.save(com.finalproject.entity.Store.builder().name("Catalog fixture").address("123 Test Street").status(true).isOpen(true).build());
        // Tạo 15 sản phẩm để kiểm tra phân trang (size = 12 -> 2 trang)
        for (int i = 1; i <= 15; i++) {
            productRepository.save(Product.builder()
                    .name("Bó Hoa Hồng Đỏ Tươi " + i)
                    .description("Mẫu hoa tươi sinh nhật tặng bạn bè " + i)
                    .importPrice(new BigDecimal("200000"))
                    .priceBuy(BigDecimal.valueOf(300000 + i * 20000))
                    .priceRentPerDay(new BigDecimal("50000"))
                    .depositFee(new BigDecimal("100000"))
                    .category(cat).store(store)
                    .isActive(true)
                    .isSelling(true)
                    .isDeleted(false)
                    .sold(10 + i)
                    .rating(4.8)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    @Test
    void categorySearchIncludesLegacyNullDeletionFlagButExcludesDeletedProducts() throws Exception {
        Product legacy = productRepository.findAll().stream()
                .filter(p -> "Bó Hoa Hồng Đỏ Tươi 1".equals(p.getName())).findFirst().orElseThrow();
        legacy.setIsDeleted(null);
        legacy.setName("Legacy catalog regression visible");
        productRepository.saveAndFlush(legacy);
        Product deleted = productRepository.findAll().stream()
                .filter(p -> "Bó Hoa Hồng Đỏ Tươi 2".equals(p.getName())).findFirst().orElseThrow();
        deleted.setIsDeleted(true);
        deleted.setName("Legacy catalog regression deleted");
        productRepository.saveAndFlush(deleted);

        MvcResult result = mockMvc.perform(get("/products")
                .param("categoryId", legacy.getCategory().getId().toString())
                .param("keyword", "Legacy catalog regression"))
                .andExpect(status().isOk()).andReturn();
        java.util.List<?> products = (java.util.List<?>) result.getModelAndView().getModel().get("products");
        assertThat(products.stream().map(p -> ((Product) p).getId()).toList())
                .contains(legacy.getId()).doesNotContain(deleted.getId());
        String html = result.getResponse().getContentAsString();
        // Main uses URL-driven filters rather than the legacy duplicated form inputs.
        assertThat(html.split("id=\"catalogSortSelect\"", -1)).hasSize(2);
        assertThat(html).contains("applyCatalogSort", "removeRatingFilter");
        assertThat(html).doesNotContain("btnApplyFilterSecondary");
    }

    @Test
    void testAllProducts_DisplaysFilterBarAndProductGrid() throws Exception {
        MvcResult result = mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        // Kiểm tra lưới sản phẩm
        assertThat(html).contains("id=\"productGrid\"");
        // Kiểm tra đầy đủ các nút thanh lọc
        assertThat(html).contains("Tất cả bộ lọc");
        assertThat(html).contains("Kiểu dáng");
        assertThat(html).contains("Loại hoa");
        assertThat(html).contains("Màu sắc");
        assertThat(html).contains("Dịp tặng");
        assertThat(html).contains("Giá");
        assertThat(html).contains("Sắp xếp");
    }

    @Test
    void testFilterBoHoa_RendersProductsAndPaginationWithoutCrash() throws Exception {
        MvcResult result = mockMvc.perform(get("/products")
                .param("flowerStyle", "Bó hoa"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        // Kiểm tra tiêu đề và bộ lọc
        assertThat(html).contains("Bó hoa");
        assertThat(html).contains("active-filter-badge");
        // Kiểm tra sản phẩm được render đầy đủ không bị đứt quãng
        assertThat(html).contains("id=\"productGrid\"");
        assertThat(html).contains("Bó Hoa Hồng Đỏ Tươi 1");
        // Kiểm tra phân trang được hiển thị (vì có 15 sản phẩm, size 12)
        assertThat(html).contains("custom-pagination");
        assertThat(html).contains("flowerStyle=B%C3%B3%20hoa");
    }

    @Test
    void testSearchByKeyword() throws Exception {
        MvcResult result = mockMvc.perform(get("/products")
                .param("keyword", "Bó Hoa Hồng Đỏ Tươi 1"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        assertThat(html).contains("Kết quả tìm kiếm: “Bó Hoa Hồng Đỏ Tươi 1”");
        assertThat(html).contains("id=\"productGrid\"");
        assertThat(html).contains("Bó Hoa Hồng Đỏ Tươi 1");
    }

    @Test
    void testCombinedMultiFilter_PreservesAllTagsAndRenders() throws Exception {
        MvcResult result = mockMvc.perform(get("/products")
                .param("flowerStyle", "Bó hoa")
                .param("flowerType", "Hoa hồng")
                .param("flowerColor", "Đỏ")
                .param("flowerOccasion", "Sinh nhật")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        assertThat(html).contains("Bó hoa");
        assertThat(html).contains("Hoa hồng");
        assertThat(html).contains("Đỏ");
        assertThat(html).contains("Sinh nhật");
        assertThat(html).contains("id=\"productGrid\"");
        assertThat(html).contains("custom-pagination");
    }

    @Test
    void testPaginationKeepsFilters() throws Exception {
        MvcResult result = mockMvc.perform(get("/products")
                .param("flowerStyle", "Bó hoa")
                .param("page", "2"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        // Trang 2
        assertThat(html).contains("Trang ");
        assertThat(html).contains("id=\"productGrid\"");
        // Link về trang 1 vẫn giữ flowerStyle
        assertThat(html).contains("flowerStyle=B%C3%B3%20hoa");
    }

    @Test
    void testNoResultsDisplay() throws Exception {
        MvcResult result = mockMvc.perform(get("/products")
                .param("keyword", "TừKhóaKhôngCóThực123456789"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        assertThat(html).contains("Không tìm thấy mẫu hoa phù hợp");
        assertThat(html).contains("Không có sản phẩm nào khớp với điều kiện lọc hiện tại.");
    }

    @Test void explicitAttributesOverrideMisleadingDescriptionAndCombineAcrossGroups() throws Exception {
        var p=productRepository.findAll().get(0);
        p.setName("Thuộc tính thực riêng"); p.setDescription("Hoa hồng đỏ sinh nhật");
        var color=styles.save(com.finalproject.entity.Style.builder().name("Màu sắc").build());
        var blue=values.save(com.finalproject.entity.StyleValue.builder().style(color).name("Xanh").build());
        p.setStyleValues(new java.util.HashSet<>(java.util.List.of(blue))); productRepository.saveAndFlush(p);
        var match=mockMvc.perform(get("/products").param("keyword", "Thuộc tính thực riêng")
                .param("flowerColor", "Xanh").param("flowerType", "Hoa hồng", "Tulip"))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        assertThat(match.get("totalItems")).isEqualTo(1L);
        var wrongColor=mockMvc.perform(get("/products").param("keyword", "Thuộc tính thực riêng").param("flowerColor", "Đỏ"))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        assertThat(wrongColor.get("totalItems")).isEqualTo(0L);
        var wrongType=mockMvc.perform(get("/products").param("keyword", "Thuộc tính thực riêng")
                .param("flowerColor", "Xanh").param("flowerType", "Tulip"))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        assertThat(wrongType.get("totalItems")).isEqualTo(0L);
    }

    @Test void priceFilterUsesPromotionAndPublicCatalogueIncludesGlobalProducts() throws Exception {
        var p=productRepository.findAll().get(0); p.setName("Global promotion fixture"); p.setStore(null);
        p.setPromotionalPrice(new BigDecimal("250000")); productRepository.saveAndFlush(p);
        var result=mockMvc.perform(get("/products").param("keyword","Global promotion fixture")
                .param("maxPrice","260000").param("size","1000"))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        assertThat(result.get("totalItems")).isEqualTo(1L);
        assertThat(result.get("size")).isEqualTo(8);
    }
}
