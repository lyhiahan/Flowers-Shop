package com.finalproject;
import com.finalproject.config.RoleConsolidationMigration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.*;
class RoleConsolidationMigrationTests {
 @Test void migrationIsIdempotentAndKeepsAccountsLocksAndUniqueAuthorities() {
  var jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:role-migration;MODE=MSSQLServer;DB_CLOSE_DELAY=-1","sa",""));
  jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, role VARCHAR(30), is_active BOOLEAN)");
  jdbc.execute("CREATE TABLE user_roles (user_id BIGINT,role VARCHAR(30),PRIMARY KEY(user_id,role))");
  jdbc.execute("CREATE TABLE role_requests (id BIGINT PRIMARY KEY,target_role VARCHAR(30))");
  jdbc.execute("CREATE TABLE store_memberships (id BIGINT PRIMARY KEY,access_role VARCHAR(30),active BOOLEAN)");
  jdbc.update("INSERT INTO users VALUES (1,'CUSTOMER',true),(2,'VENDOR',true),(3,'STAFF',false),(4,'SHIPPER',false),(5,'ADMIN',true)");
  jdbc.update("INSERT INTO user_roles VALUES (1,'CUSTOMER'),(2,'VENDOR'),(2,'MANAGER'),(2,'STAFF'),(2,'CUSTOMER'),(3,'STAFF')");
  jdbc.update("INSERT INTO role_requests VALUES (1,'VENDOR'),(2,'STAFF')");
  jdbc.update("INSERT INTO store_memberships VALUES (1,'STAFF',false),(2,'PARTNER',true)");
  var migration=new RoleConsolidationMigration(jdbc);migration.run(null);migration.run(null);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users",Integer.class)).isEqualTo(5);
  assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id=3",String.class)).isEqualTo("MANAGER");
  assertThat(jdbc.queryForObject("SELECT is_active FROM users WHERE id=3",Boolean.class)).isFalse();
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_roles WHERE user_id=2 AND role='MANAGER'",Integer.class)).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_roles WHERE role IN ('CUSTOMER','VENDOR','STAFF')",Integer.class)).isZero();
  assertThat(jdbc.queryForObject("SELECT active FROM store_memberships WHERE id=1",Boolean.class)).isFalse();
  assertThat(jdbc.queryForObject("SELECT access_role FROM store_memberships WHERE id=1",String.class)).isEqualTo("PARTNER");
 }
}
