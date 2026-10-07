package com.finalproject;

import com.finalproject.config.ReviewIntegrityMigration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.*;

class ReviewIntegrityMigrationTests {
    JdbcTemplate database() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + java.util.UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE reviews(id BIGINT PRIMARY KEY,user_id BIGINT,product_id BIGINT,order_id BIGINT)");
        return jdbc;
    }
    @Test void preservesMultipleLegacyReviewsWithoutOrderAndRejectsDuplicatePurchasedReview() {
        var jdbc = database();
        jdbc.update("INSERT INTO reviews VALUES(1,1,1,NULL),(2,1,1,NULL),(3,1,1,10)");
        var migration = new ReviewIntegrityMigration(jdbc);
        migration.run(null); migration.run(null);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reviews", Integer.class)).isEqualTo(3);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO reviews VALUES(4,1,1,10)"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        jdbc.update("INSERT INTO reviews VALUES(5,1,1,11)");
    }
    @Test void duplicateHistoryStopsMigrationWithoutDeletingData() {
        var jdbc = database();
        jdbc.update("INSERT INTO reviews VALUES(1,1,1,10),(2,1,1,10)");
        assertThatThrownBy(() -> new ReviewIntegrityMigration(jdbc).run(null)).hasMessageContaining("không tự xóa lịch sử");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reviews", Integer.class)).isEqualTo(2);
    }
}
