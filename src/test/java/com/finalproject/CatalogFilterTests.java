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

    @BeforeEach
    void setupTestData() {
        Category cat = categoryRepository.save(Category.builder()
                .name("Hoa sinh nhật")
                .description("Các mẫu hoa sinh nhật đẹp")
                .build());

        // Tạo 15 sản phẩm để kiểm tra phân trang (size = 12 -> 2 trang)
        for (int i = 1; i <= 15; i++) {
            productRepository.save(Product.builder()
                    .name("Bó Hoa Hồng Đỏ Tươi " + i)
                    .description("Mẫu hoa tươi sinh nhật tặng bạn bè " + i)
                    .importPrice(new BigDecimal("200000"))
                    .priceBuy(BigDecimal.valueOf(300000 + i * 20000))
                    .priceRentPerDay(new BigDecimal("50000"))
                    .depositFee(new BigDecimal("100000"))
                    .category(cat)
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
    void testAllProducts_DisplaysFilterBarAndProductGrid() throws Exception {
        MvcResult result = mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        // Kiểm tra lưới sản phẩm
        assertThat(html).contains("productListGrid");
        assertThat(html).contains("flower-card");
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

        // Kiểm tra tiêu đề và nhãn lọc không bị đứt gãy
        assertThat(html).contains("Bó hoa");
        assertThat(html).contains("active-filter-badge");
        assertThat(html).contains("data-group=\"flowerStyle\"");
        assertThat(html).contains("data-value=\"Bó hoa\"");
        // Đảm bảo không bị lồng trang báo lỗi mid-stream
        assertThat(html).doesNotContain("Thông Báo Hệ Thống");
        assertThat(html).doesNotContain("Đã có vấn đề phát sinh khi xử lý yêu cầu");
        // Kiểm tra danh sách sản phẩm được render đầy đủ
        assertThat(html).contains("productListGrid");
        assertThat(html).contains("Bó Hoa Hồng Đỏ Tươi 1");
        // Kiểm tra phân trang được hiển thị (vì có 15 sản phẩm, size 8)
        assertThat(html).contains("custom-pagination");
    }

    @Test
    void testSearchByKeyword() throws Exception {
        MvcResult result = mockMvc.perform(get("/products")
                .param("keyword", "Bó Hoa Hồng Đỏ Tươi 1"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        assertThat(html).contains("Bó Hoa Hồng Đỏ Tươi 1");
        assertThat(html).contains("productListGrid");
        assertThat(html).doesNotContain("Thông Báo Hệ Thống");
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

        // Kiểm tra đầy đủ cả 4 nhãn lọc đang hoạt động
        assertThat(html).contains("Kiểu dáng:");
        assertThat(html).contains("Bó hoa");
        assertThat(html).contains("Loại hoa:");
        assertThat(html).contains("Hoa hồng");
        assertThat(html).contains("Màu sắc:");
        assertThat(html).contains("Đỏ");
        assertThat(html).contains("Dịp tặng:");
        assertThat(html).contains("Sinh nhật");
        // Kiểm tra danh sách sản phẩm không bị hỏng layout
        assertThat(html).contains("productListGrid");
        assertThat(html).doesNotContain("Thông Báo Hệ Thống");
    }

    @Test
    void testPaginationKeepsFilters() throws Exception {
        MvcResult result = mockMvc.perform(get("/products")
                .param("flowerStyle", "Bó hoa")
                .param("page", "2"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        // Trang 2/2
        assertThat(html).contains("<span>2/2</span>");
        assertThat(html).contains("productListGrid");
        // Link phân trang vẫn giữ flowerStyle
        assertThat(html).contains("flowerStyle=");
    }

    @Test
    void testKeywordHoaHongAndFlowerStyleBoHoa() throws Exception {
        MvcResult result = mockMvc.perform(get("/products")
                .param("keyword", "hoa hồng")
                .param("flowerStyle", "Bó hoa"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        // Kiểm tra không bị ngắt quãng giữa chừng
        assertThat(html).doesNotContain("Thông Báo Hệ Thống");
        assertThat(html).contains("Từ khóa:");
        assertThat(html).contains("Kiểu dáng:");
        assertThat(html).contains("Bó hoa");
        assertThat(html).contains("productListGrid");
        assertThat(html).contains("</html>");
    }

    @Test
    void testFlowerStyleBoHoaAndFlowerTypeHoaHong() throws Exception {
        MvcResult result = mockMvc.perform(get("/products")
                .param("flowerStyle", "Bó hoa")
                .param("flowerType", "Hoa hồng"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        // Cả hai nhãn Kiểu dáng và Loại hoa đều hiển thị đầy đủ, không lỗi
        assertThat(html).doesNotContain("Thông Báo Hệ Thống");
        assertThat(html).contains("Kiểu dáng:");
        assertThat(html).contains("Bó hoa");
        assertThat(html).contains("Loại hoa:");
        assertThat(html).contains("Hoa hồng");
        assertThat(html).contains("data-group=\"flowerStyle\"");
        assertThat(html).contains("data-group=\"flowerType\"");
        assertThat(html).contains("productListGrid");
        assertThat(html).contains("</html>");
    }

    @Test
    void testMultiValueSameGroup_UsesOrCondition() throws Exception {
        MvcResult result = mockMvc.perform(get("/products")
                .param("flowerStyle", "Bó hoa, Giỏ hoa"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        assertThat(html).doesNotContain("Thông Báo Hệ Thống");
        assertThat(html).contains("Kiểu dáng:");
        assertThat(html).contains("Bó hoa");
        assertThat(html).contains("Giỏ hoa");
        assertThat(html).contains("productListGrid");
        assertThat(html).contains("</html>");
    }
}
