package com.finalproject;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MigrationHistoryTests {
    @Test void restoredV11ValidatesAgainstThePreviouslyAppliedChecksum() throws Exception {
        String url="jdbc:h2:mem:migration-history-"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1";
        var flyway=Flyway.configure().dataSource(url,"sa","").baselineVersion("10")
                .locations("filesystem:src/main/resources/db/migration/sqlserver")
                .ignoreMigrationPatterns("*:pending").cleanDisabled(true).load();
        flyway.baseline();
        try(var db=DriverManager.getConnection(url,"sa","");var st=db.createStatement()) {
            st.executeUpdate("INSERT INTO \"flyway_schema_history\" (\"installed_rank\",\"version\",\"description\",\"type\",\"script\",\"checksum\",\"installed_by\",\"execution_time\",\"success\") VALUES (2,'11','review image public id','SQL','V11__review_image_public_id.sql',306107251,'test',1,true)");
        }
        assertThatCode(flyway::validate).doesNotThrowAnyException();
        assertThat(flyway.info().pending()).extracting(m -> m.getVersion().getVersion())
                .containsExactly("12","13","14","15","17","18");
    }
}
