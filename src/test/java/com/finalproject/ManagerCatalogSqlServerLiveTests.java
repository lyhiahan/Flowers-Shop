package com.finalproject;

import com.finalproject.entity.Product;
import com.finalproject.service.StoreProductService;
import jakarta.persistence.Entity;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Read-only catalog queries against an existing disposable SQL Server copy; no application startup. */
@EnabledIfSystemProperty(named="catalog.sqlserver.live", matches="true")
class ManagerCatalogSqlServerLiveTests {
    @Test void everyBranchCatalogTabCanBeQueriedOnSqlServer() throws Exception {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        if (url == null || !url.matches("(?is).*;databaseName=CodexCatalogCopy_[0-9_]+;.*"))
            throw new IllegalArgumentException("Only a disposable catalog copy may be queried.");
        Map<String,Object> settings = new HashMap<>();
        settings.put("hibernate.connection.url", url);
        settings.put("hibernate.connection.username", System.getenv("SPRING_DATASOURCE_USERNAME"));
        settings.put("hibernate.connection.password", System.getenv("SPRING_DATASOURCE_PASSWORD"));
        settings.put("hibernate.connection.driver_class", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        settings.put("hibernate.default_schema", "dbo");
        settings.put("hibernate.physical_naming_strategy", new PhysicalNamingStrategySnakeCaseImpl());
        settings.put("hibernate.hbm2ddl.auto", "validate");
        var registry = new StandardServiceRegistryBuilder().applySettings(settings).build();
        try {
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            var metadata = new MetadataSources(registry);
            for (var bean : scanner.findCandidateComponents("com.finalproject.entity"))
                metadata.addAnnotatedClass(Class.forName(bean.getBeanClassName()));
            try (var factory = metadata.buildMetadata().buildSessionFactory(); var session = factory.openSession()) {
                session.setDefaultReadOnly(true);
                var storeIds = session.createQuery("select s.id from Store s", Long.class).getResultList();
                assertThat(storeIds).isNotEmpty();
                var cb = session.getCriteriaBuilder();
                for (Long storeId : storeIds) for (String tab : List.of("selling", "stored", "all", "deleted")) {
                    var query = cb.createQuery(Product.class);
                    var root = query.from(Product.class);
                    query.select(root).where(StoreProductService.catalog(storeId, tab).toPredicate(root, query, cb))
                            .orderBy(cb.desc(root.get("id")));
                    var results = session.createQuery(query).setMaxResults(8).getResultList();
                    assertThat(results).allSatisfy(p -> {
                        assertThat(p.getStore() == null || storeId.equals(p.getStore().getId())).isTrue();
                    });
                    var count = cb.createQuery(Long.class);
                    var countRoot = count.from(Product.class);
                    count.select(cb.count(countRoot)).where(StoreProductService.catalog(storeId, tab).toPredicate(countRoot, count, cb));
                    assertThat(session.createQuery(count).getSingleResult()).isGreaterThanOrEqualTo((long) results.size());
                }
            }
        } finally { StandardServiceRegistryBuilder.destroy(registry); }
    }
}
