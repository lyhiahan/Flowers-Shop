package com.finalproject;

import jakarta.persistence.Entity;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class PortableDatabaseMappingTests {
    @Test void longUnicodeColumnsRemainCompatibleWithBothDatabases() throws Exception {
        var columns = Map.of("categories", "description", "products", "description", "notifications", "content",
                "stores", "featured_images");
        for (String dialect : new String[]{"SQLServer", "PostgreSQL"}) {
            var registry = new StandardServiceRegistryBuilder().applySettings(Map.of(
                    "hibernate.dialect", "org.hibernate.dialect." + dialect + "Dialect",
                    "hibernate.boot.allow_jdbc_metadata_access", false,
                    "hibernate.physical_naming_strategy", new PhysicalNamingStrategySnakeCaseImpl())).build();
            try {
                var scanner = new ClassPathScanningCandidateComponentProvider(false);
                scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
                var sources = new MetadataSources(registry);
                for (var bean : scanner.findCandidateComponents("com.finalproject.entity"))
                    sources.addAnnotatedClass(Class.forName(bean.getBeanClassName()));
                var metadata = sources.buildMetadata();
                for (var entry : columns.entrySet()) {
                    var table = metadata.collectTableMappings().stream().filter(t -> t.getName().equals(entry.getKey())).findFirst().orElseThrow();
                    var column = table.getColumns().stream().filter(c -> c.getName().equals(entry.getValue())).findFirst().orElseThrow();
                    assertThat(column.getSqlType(metadata)).as("%s %s.%s", dialect, entry.getKey(), entry.getValue())
                            .isEqualTo(dialect.equals("SQLServer") ? "nvarchar(max)" : "text");
                }
            } finally { StandardServiceRegistryBuilder.destroy(registry); }
        }
    }
}
