package com.finalproject;

import com.finalproject.db.migration.V16__separate_store_staff_role;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.*;

class StaffRoleMigrationTests {
    @Test void migrationSeparatesStaffPreservesManagersAdminsLocksAndInactiveMembershipsAndCanRetry() {
        var jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:staff-separation;MODE=MSSQLServer;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, role VARCHAR(30), store_id BIGINT, is_active BOOLEAN)");
        jdbc.execute("CREATE TABLE stores (id BIGINT PRIMARY KEY, owner_id BIGINT)");
        jdbc.execute("CREATE TABLE user_roles (user_id BIGINT, role VARCHAR(30), PRIMARY KEY(user_id,role))");
        jdbc.execute("CREATE TABLE store_memberships (user_id BIGINT,store_id BIGINT,access_role VARCHAR(16),active BOOLEAN)");
        jdbc.update("INSERT INTO users VALUES (1,'MANAGER',10,true),(2,'MANAGER',10,true),(3,'MANAGER',10,false),(4,'MANAGER',NULL,true),(5,'ADMIN',NULL,true),(6,'MANAGER',10,true)");
        jdbc.update("INSERT INTO stores VALUES (10,1),(11,1)");
        jdbc.update("INSERT INTO user_roles VALUES (1,'MANAGER'),(2,'MANAGER'),(2,'USER'),(3,'MANAGER'),(5,'ADMIN'),(6,'ADMIN')");
        jdbc.update("INSERT INTO store_memberships VALUES (2,10,'PARTNER',true),(3,10,'PARTNER',false),(6,10,'PARTNER',true)");
        var migration=new V16__separate_store_staff_role();migration.separate(jdbc);migration.separate(jdbc);
        assertThat(jdbc.queryForList("SELECT id FROM users WHERE role='STAFF' ORDER BY id",Long.class)).containsExactly(2L,3L);
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id=1",String.class)).isEqualTo("MANAGER");
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id=4",String.class)).isEqualTo("MANAGER");
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id=5",String.class)).isEqualTo("ADMIN");
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id=6",String.class)).isEqualTo("MANAGER");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_roles WHERE user_id IN (2,3) AND role='MANAGER'",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_roles WHERE role='STAFF'",Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT is_active FROM users WHERE id=3",Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("SELECT active FROM store_memberships WHERE user_id=3",Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("SELECT access_role FROM store_memberships WHERE user_id=3",String.class)).isEqualTo("STAFF");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stores WHERE owner_id=1",Integer.class)).isEqualTo(2);
    }
}
