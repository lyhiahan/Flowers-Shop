package com.finalproject;

import com.finalproject.config.DataInitializer;
import com.finalproject.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-init-tests;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
        "app.demo-data.enabled=true"
})
@ActiveProfiles({"dev", "test"})
class DemoDataInitializationTests {
    @Autowired DataInitializer initializer;
    @Autowired StoreRepository stores;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;

    @Test void demoBuildsChainAndRepeatedStartupPreservesEditedFixtures() throws Exception {
        assertThat(stores.count()).isGreaterThanOrEqualTo(3);
        assertThat(products.count()).isGreaterThan(5);
        assertThat(users.findByUsername("nguyenthanhminh")).hasValueSatisfying(user ->
                assertThat(user.getRole()).isEqualTo(com.finalproject.enums.Role.ADMIN));
        var product = products.findAll().getFirst();
        String title = "Sản phẩm đã chỉnh sửa thủ công";
        product.setName(title);
        product.setIsSelling(false);
        products.saveAndFlush(product);
        long productCount = products.count();
        long categoryCount = categories.count();
        initializer.run();
        var unchanged = products.findById(product.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo(title);
        assertThat(unchanged.getIsSelling()).isFalse();
        assertThat(products.count()).isEqualTo(productCount);
        assertThat(categories.count()).isEqualTo(categoryCount);
    }
}
