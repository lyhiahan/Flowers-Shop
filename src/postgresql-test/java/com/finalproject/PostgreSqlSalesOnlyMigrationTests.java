package com.finalproject;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class PostgreSqlSalesOnlyMigrationTests {
    static EmbeddedPostgres postgres;
    String url;
    @BeforeAll static void start() throws Exception { postgres=EmbeddedPostgres.builder().setPort(0).start(); }
    @AfterAll static void stop() throws Exception { if(postgres!=null)postgres.close(); }
    @BeforeEach void database() throws Exception {
        String name="sales_"+UUID.randomUUID().toString().replace("-", "");
        try(var c=postgres.getPostgresDatabase().getConnection();var s=c.createStatement()){s.execute("CREATE DATABASE "+name);}
        url=postgres.getJdbcUrl("postgres",name);
        migrate("1").migrate();
    }
    Flyway migrate(String target) {
        var config=Flyway.configure().dataSource(url,"postgres","postgres").locations("classpath:db/migration/postgresql").cleanDisabled(true);
        if(target!=null)config.target(target);
        return config.load();
    }
    String value(String sql) throws Exception {
        try(var c=DriverManager.getConnection(url,"postgres","postgres");var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getString(1);}
    }
    @Test void archivesRemovedValuesAndPreservesFlowerPrice() throws Exception {
        try(var c=DriverManager.getConnection(url,"postgres","postgres");var s=c.createStatement()) {
            s.execute("ALTER TABLE orders ADD CONSTRAINT test_payment_status CHECK(payment_status IN ('UNPAID','PAID','REFUNDED'))");
            s.execute("INSERT INTO categories(id,name) VALUES(1,'Hoa')");
            s.execute("INSERT INTO products(id,name,category_id,import_price,price_buy,price_rent_per_day,deposit_fee,is_active,is_selling,sold) VALUES(1,'Hoa',1,100000,350000,7000,50000,true,true,8)");
        }
        assertThat(migrate(null).migrate().migrationsExecuted).isEqualTo(1);
        assertThat(new java.math.BigDecimal(value("SELECT price_buy FROM products WHERE id=1"))).isEqualByComparingTo("350000");
        assertThat(value("SELECT sold FROM products WHERE id=1")).isEqualTo("8");
        assertThat(value("SELECT count(*) FROM pg_constraint WHERE conname='test_payment_status'")).isEqualTo("1");
        assertThat(new java.math.BigDecimal(value("SELECT payload->>'price_rent_per_day' FROM schema_change_archive WHERE table_name='products' AND row_id=1"))).isEqualByComparingTo("7000");
        assertThat(value("SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND column_name IN ('price_rent_per_day','deposit_fee','rent_start_date','rent_end_date','actual_return_date','return_store_id','total_deposit','fine_amount')")).isEqualTo("0");
        assertThat(migrate(null).migrate().migrationsExecuted).isZero();
    }
    @Test void refusesUnreconciledTransactionsWithoutDroppingHistory() throws Exception {
        try(var c=DriverManager.getConnection(url,"postgres","postgres");var s=c.createStatement()) {
            s.execute("INSERT INTO promotions(name,discount_percentage,apply_type) VALUES('Historical',5,'RENT')");
        }
        assertThatThrownBy(()->migrate(null).migrate()).hasMessageContaining("Unsupported historical transactions");
        assertThat(value("SELECT apply_type FROM promotions")).isEqualTo("RENT");
        assertThat(value("SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='products' AND column_name='deposit_fee'")).isEqualTo("1");
    }
    @Test void finalSchemaAcceptsOnlyFlowerPurchases() throws Exception {
        migrate(null).migrate();
        try(var c=DriverManager.getConnection(url,"postgres","postgres");var s=c.createStatement()) {
            assertThatThrownBy(()->s.execute("INSERT INTO promotions(name,discount_percentage,apply_type) VALUES('Invalid',5,'RENT')")).isInstanceOf(SQLException.class);
            s.execute("INSERT INTO promotions(name,discount_percentage,apply_type) VALUES('Sale',5,'BUY')");
        }
        assertThat(value("SELECT count(*) FROM promotions")).isEqualTo("1");
    }
}
