package com.finalproject.db.migration;

import com.finalproject.config.*;
import org.flywaydb.core.api.migration.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V4__chat_payment_and_review_integrity extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        var jdbc = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        new PaymentSchemaMigration(jdbc).run(null);
        new ChatSchemaMigration(jdbc).run(null);
        new ReviewIntegrityMigration(jdbc).run(null);
    }
}
