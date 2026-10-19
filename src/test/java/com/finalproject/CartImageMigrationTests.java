package com.finalproject;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class CartImageMigrationTests {
    @Test void imageRepairTargetsOnlyKnownDemoBouquetsAndDoesNotRepeat() throws Exception {
        String oldImage = "https://images.unsplash.com/photo-1548094878-84ced0f68b08?w=800";
        String newImage = "/images/demo-rose-bouquet.jpg";
        String currentName = "Bó Hoa Hồng Nhập Khẩu 20 Bông 'Tỏ Tình Lãng Mạn'";
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:cart-image-" + UUID.randomUUID() + ";MODE=MSSQLServer", "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA dbo");
            statement.execute("CREATE TABLE dbo.products(id int PRIMARY KEY, name nvarchar(300), image_url varchar(1000), price int)");
            String[][] data = {{currentName, oldImage}, {"Bó Hoa Hồng Đỏ 20 Bông", oldImage},
                    {"Different product", oldImage}, {currentName, "/upload/customer-photo.jpg"}, {currentName, null}};
            try (var insert = connection.prepareStatement("INSERT INTO dbo.products VALUES (?,?,?,580000)")) {
                for (int i = 0; i < data.length; i++) {
                    insert.setInt(1, i + 1); insert.setNString(2, data[i][0]); insert.setString(3, data[i][1]);
                    insert.executeUpdate();
                }
            }
            String migration = Files.readString(Path.of("src/main/resources/db/migration/sqlserver/V13__replace_missing_demo_rose_image.sql"));
            for (int run = 0; run < 2; run++) {
                assertThat(statement.executeUpdate(migration)).isEqualTo(run == 0 ? 2 : 0);
                try (var rows = statement.executeQuery("SELECT id,name,image_url,price FROM dbo.products ORDER BY id")) {
                    int count = 0;
                    while (rows.next()) {
                        int index = rows.getInt(1) - 1;
                        assertThat(rows.getNString(2)).isEqualTo(data[index][0]);
                        assertThat(rows.getString(3)).isEqualTo(index < 2 ? newImage : data[index][1]);
                        assertThat(rows.getInt(4)).isEqualTo(580000);
                        count++;
                    }
                    assertThat(count).isEqualTo(data.length);
                }
            }
        }
    }
}
