package com.finalproject.tools;

import tools.jackson.databind.json.JsonMapper;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Explicit offline transfer tool. No Spring bean, endpoint, source writes or destructive SQL. */
public final class DatabaseTransfer {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final byte[] MAGIC = "RENTAL-SNAPSHOT-1\n".getBytes(StandardCharsets.US_ASCII);
    private static final Set<String> AUTH_TABLES = Set.of("jwt_sessions", "jwt_refresh_tokens", "refresh_tokens", "otp_verifications");
    private static final String ARCHIVE_SCHEMA = "rental_sqlserver_archive";

    public record Column(String name, int jdbcType, String typeName, int size, int scale, boolean nullable, boolean identity) {}
    public record Table(String name, List<Column> columns, List<List<String>> rows) {}
    public record Snapshot(int format, String id, String createdAt, String sourceDatabase, String sourceVersion, List<Table> tables) {}
    public record TablePlan(String table, int sourceRows, long targetRows, List<String> archivedColumns) {}
    public record Plan(String snapshotId, List<TablePlan> tables, List<String> archiveOnlyTables, List<String> skippedAuthenticationTables) {}
    public record Check(String table, long rows, String sha256) {}
    public record Result(String snapshotId, List<Check> verifiedTables, Map<String, String> financialTotals) {}
    private record Target(Table table, List<String> primaryKey, Set<String> deferred, Set<String> dependencies) {}

    public static void main(String[] args) {
        try {
            if (args.length != 2 || !Set.of("export", "plan", "import").contains(args[0]))
                throw new IllegalArgumentException("Usage: DatabaseTransfer export|plan|import encrypted-snapshot-file");
            Path file = Path.of(args[1]);
            byte[] key = Base64.getDecoder().decode(required("MIGRATION_BACKUP_KEY"));
            try {
                if (args[0].equals("export")) {
                    Properties local = new Properties();
                    try (var input = Files.newBufferedReader(Path.of(".env"), StandardCharsets.UTF_8)) { local.load(input); }
                    Properties credentials = new Properties();
                    credentials.setProperty("user", local.getProperty("SPRING_DATASOURCE_USERNAME", ""));
                    credentials.setProperty("password", local.getProperty("SPRING_DATASOURCE_PASSWORD", ""));
                    if (!"tv_thanhtai".equals(credentials.getProperty("user")))
                        throw new IllegalStateException("Expected shared SQL login tv_thanhtai in local .env.");
                    try (var source = DriverManager.getConnection(
                            "jdbc:sqlserver://100.82.62.3:53735;databaseName=RentalShop;encrypt=true;trustServerCertificate=true;loginTimeout=15;", credentials)) {
                        try (var st = source.createStatement()) { st.execute("SET LOCK_TIMEOUT 15000"); }
                        Snapshot snapshot = capture(source, "dbo", "RentalShop");
                        writeEncrypted(snapshot, file, key);
                        System.out.println(JSON.writeValueAsString(Map.of("snapshotId", snapshot.id(), "createdAt", snapshot.createdAt(),
                                "tables", snapshot.tables().stream().map(t -> Map.of("table", t.name(), "rows", t.rows().size())).toList())));
                    } finally { credentials.clear(); local.clear(); }
                } else {
                    Snapshot snapshot = readEncrypted(file, key);
                    try (var destination = targetConnection()) {
                        if (args[0].equals("plan")) {
                            destination.setReadOnly(true);
                            System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(plan(snapshot, destination)));
                        } else {
                            Result result = apply(snapshot, destination);
                            Path report = file.resolveSibling(file.getFileName() + ".verified.json");
                            System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result));
                            try { Files.writeString(report, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result), StandardOpenOption.CREATE_NEW); }
                            catch (IOException reportFailure) { System.err.println("Import COMMITTED, but local report could not be written. Do not re-import; check the archive marker."); }
                        }
                    }
                }
            } finally { Arrays.fill(key, (byte) 0); }
        } catch (Exception failure) {
            // JDBC errors can include customer data or password hashes in failing row details.
            if (failure instanceof SQLException sql) {
                System.err.println("Database operation failed; SQLSTATE=" + sql.getSQLState() + ", code=" + sql.getErrorCode()
                        + ". Source was not modified. An uncommitted target import is rolled back.");
            } else if (failure instanceof IllegalArgumentException || failure instanceof IllegalStateException) {
                System.err.println(failure.getMessage());
            } else {
                System.err.println("Transfer failed (" + failure.getClass().getSimpleName()
                        + "). No source writes. Check the encrypted backup and verification report before retrying.");
            }
            System.exit(1);
        }
    }

    private static Connection targetConnection() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", required("AIVEN_USER"));
        properties.setProperty("password", required("AIVEN_PASSWORD"));
        properties.setProperty("sslmode", "verify-full");
        properties.setProperty("sslrootcert", required("AIVEN_CA_CERT"));
        properties.setProperty("connectTimeout", "15");
        properties.setProperty("socketTimeout", "120");
        properties.setProperty("ApplicationName", "rental-verified-transfer");
        try {
            return DriverManager.getConnection("jdbc:postgresql://" + required("AIVEN_HOST") + ":" + required("AIVEN_PORT")
                    + "/" + required("AIVEN_DATABASE"), properties);
        } finally { properties.clear(); }
    }

    public static Snapshot capture(Connection source, String schema, String database) throws SQLException {
        if (!source.getAutoCommit()) throw new IllegalStateException("Capture requires a dedicated connection.");
        source.setReadOnly(true);
        source.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        source.setAutoCommit(false);
        try {
            String version;
            try (var st = source.prepareStatement("SELECT version FROM " + q(schema) + ".flyway_schema_history WHERE success=? ORDER BY installed_rank DESC")) {
                st.setBoolean(1, true);
                try (var rs = st.executeQuery()) {
                    if (!rs.next()) throw new IllegalStateException("Missing successful source Flyway version.");
                    version = rs.getString(1);
                }
            }
            if (!"17".equals(version)) throw new IllegalStateException("Expected source V17; re-audit schema before exporting another version.");
            List<Table> result = new ArrayList<>();
            for (String name : tableNames(source, schema)) {
                List<Column> columns = columns(source, schema, name);
                result.add(new Table(name, columns, readRows(source, schema, name, columns)));
            }
            source.commit();
            return new Snapshot(1, UUID.randomUUID().toString(), Instant.now().toString(), database, version, result);
        } catch (SQLException | RuntimeException failure) {
            source.rollback(); throw failure;
        } finally { source.setAutoCommit(true); }
    }

    public static void writeEncrypted(Snapshot snapshot, Path path, byte[] key) throws Exception {
        byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);
        Cipher cipher = cipher(Cipher.ENCRYPT_MODE, key, iv);
        Files.createDirectories(path.toAbsolutePath().getParent());
        try (var out = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
            out.write(MAGIC); out.write(iv);
            try (var encrypted = new CipherOutputStream(out, cipher)) { encrypted.write(JSON.writeValueAsBytes(snapshot)); }
        }
    }

    public static Snapshot readEncrypted(Path path, byte[] key) throws Exception {
        try (var input = Files.newInputStream(path)) {
            if (!Arrays.equals(input.readNBytes(MAGIC.length), MAGIC)) throw new IllegalArgumentException("Unknown snapshot format.");
            byte[] iv = input.readNBytes(12);
            if (iv.length != 12) throw new IllegalArgumentException("Incomplete encrypted snapshot.");
            try (var decrypted = new CipherInputStream(input, cipher(Cipher.DECRYPT_MODE, key, iv))) {
                // Fully authenticate the GCM stream before parsing or touching the destination.
                return JSON.readValue(decrypted.readAllBytes(), Snapshot.class);
            }
        }
    }

    private static Cipher cipher(int mode, byte[] key, byte[] iv) throws GeneralSecurityException {
        if (key.length != 32) throw new IllegalArgumentException("A 256-bit backup key is required.");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        cipher.updateAAD(MAGIC);
        return cipher;
    }

    public static Plan plan(Snapshot snapshot, Connection destination) throws SQLException {
        validateSnapshot(snapshot);
        validateDestination(destination);
        Map<String, Table> source = byName(snapshot);
        List<TablePlan> plans = new ArrayList<>();
        Set<String> targetNames = new TreeSet<>(tableNames(destination, "public"));
        for (String name : targetNames) {
            if (name.equals("flyway_schema_history")) continue;
            long count = count(destination, "public", name);
            if (count != 0) throw new IllegalStateException("Destination is not empty: " + name + ". No overwrite or deletion is permitted.");
            if (AUTH_TABLES.contains(name)) continue;
            Table from = source.get(name);
            if (from == null) throw new IllegalStateException("Source is missing application table " + name);
            List<Column> target = columns(destination, "public", name);
            Map<String, Column> sourceColumns = new HashMap<>();
            from.columns().forEach(c -> sourceColumns.put(c.name(), c));
            for (Column column : target) {
                if (!sourceColumns.containsKey(column.name()))
                    throw new IllegalStateException("Source is missing destination column " + name + "." + column.name());
            }
            Set<String> targetColumns = new HashSet<>(); target.forEach(c -> targetColumns.add(c.name()));
            plans.add(new TablePlan(name, from.rows().size(), count,
                    from.columns().stream().map(Column::name).filter(c -> !targetColumns.contains(c)).toList()));
        }
        return new Plan(snapshot.id(), plans,
                source.keySet().stream().filter(t -> !targetNames.contains(t) && !AUTH_TABLES.contains(t)).sorted().toList(),
                snapshot.tables().stream().map(Table::name).filter(AUTH_TABLES::contains).sorted().toList());
    }

    public static Result apply(Snapshot snapshot, Connection destination) throws SQLException {
        if (!destination.getAutoCommit()) throw new IllegalStateException("Import requires a dedicated connection.");
        destination.setReadOnly(false);
        destination.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        destination.setAutoCommit(false);
        try {
            try (var st = destination.createStatement()) {
                st.execute("SET LOCAL lock_timeout='15s'");
                st.execute("SET LOCAL statement_timeout='120s'");
                try (var lock = st.executeQuery("SELECT pg_try_advisory_xact_lock(1845025339)")) {
                    lock.next(); if (!lock.getBoolean(1)) throw new IllegalStateException("Another transfer is running.");
                }
                List<String> tables = tableNames(destination, "public").stream().filter(t -> !t.equals("flyway_schema_history")).toList();
                st.execute("LOCK TABLE " + String.join(",", tables.stream().map(t -> "public." + q(t)).toList()) + " IN ACCESS EXCLUSIVE MODE NOWAIT");
            }
            Plan plan = plan(snapshot, destination);
            Map<String, Table> source = byName(snapshot);
            List<Target> targets = new ArrayList<>();
            for (var item : plan.tables()) targets.add(target(destination, item.table()));
            List<Target> ordered = order(targets);
            for (Target target : ordered) insert(destination, source.get(target.table().name()), target);
            for (Target target : ordered) restoreOptionalReferences(destination, source.get(target.table().name()), target);
            List<Check> checks = new ArrayList<>();
            for (Target target : ordered) {
                Table table = target.table();
                List<List<String>> expected = project(source.get(table.name()), table.columns());
                List<List<String>> actual = readRows(destination, "public", table.name(), table.columns());
                String digest = digest(table.columns(), expected);
                if (actual.size() != expected.size() || !digest.equals(digest(table.columns(), actual)))
                    throw new IllegalStateException("Reconciliation failed for " + table.name() + "; entire import rolled back.");
                checks.add(new Check(table.name(), actual.size(), digest));
                restartIdentity(destination, target);
            }
            archive(destination, snapshot);
            Map<String, String> totals = financialTotals(destination);
            destination.commit();
            return new Result(snapshot.id(), checks, totals);
        } catch (SQLException | RuntimeException failure) {
            destination.rollback(); throw failure;
        } finally { destination.setAutoCommit(true); }
    }

    private static void validateSnapshot(Snapshot snapshot) {
        if (snapshot.format() != 1 || !"RentalShop".equals(snapshot.sourceDatabase()) || !"17".equals(snapshot.sourceVersion()))
            throw new IllegalArgumentException("Unsupported snapshot/database/version.");
        UUID.fromString(snapshot.id()); Instant.parse(snapshot.createdAt());
        Set<String> names = new HashSet<>();
        for (Table table : snapshot.tables()) {
            q(table.name()); if (!names.add(table.name())) throw new IllegalArgumentException("Duplicate snapshot table.");
            Set<String> cols = new HashSet<>();
            for (Column c : table.columns()) { q(c.name()); if (!cols.add(c.name())) throw new IllegalArgumentException("Duplicate column."); }
            for (var row : table.rows()) if (row.size() != table.columns().size()) throw new IllegalArgumentException("Snapshot row width mismatch.");
        }
    }

    private static void validateDestination(Connection db) throws SQLException {
        if (!db.getMetaData().getDatabaseProductName().equals("PostgreSQL")) throw new IllegalStateException("Destination must be PostgreSQL.");
        try (var st = db.createStatement(); var rs = st.executeQuery("SELECT version, success FROM public.flyway_schema_history ORDER BY installed_rank")) {
            int rows = 0;
            while (rs.next()) {
                rows++;
                if (!"1".equals(rs.getString(1)) || !rs.getBoolean(2)) throw new IllegalStateException("Expected fresh PostgreSQL Flyway V1.");
            }
            if (rows != 1) throw new IllegalStateException("Unexpected target Flyway history.");
        }
        try (var schemas = db.getMetaData().getSchemas(null, ARCHIVE_SCHEMA)) {
            if (schemas.next()) throw new IllegalStateException("Migration archive already exists. Inspect the prior import; do not overwrite it.");
        }
    }

    private static Target target(Connection db, String name) throws SQLException {
        List<Column> cols = columns(db, "public", name);
        Map<String, Column> index = new HashMap<>(); cols.forEach(c -> index.put(c.name(), c));
        TreeMap<Short,String> key = new TreeMap<>();
        try (var rs = db.getMetaData().getPrimaryKeys(null, "public", name)) { while (rs.next()) key.put(rs.getShort("KEY_SEQ"), rs.getString("COLUMN_NAME")); }
        if (key.isEmpty()) throw new IllegalStateException("Missing target primary key for " + name);
        Map<String, List<String>> foreignColumns = new LinkedHashMap<>();
        Map<String, String> referenced = new HashMap<>();
        try (var rs = db.getMetaData().getImportedKeys(null, "public", name)) {
            while (rs.next()) {
                String fk = rs.getString("FK_NAME");
                if (!"public".equals(rs.getString("PKTABLE_SCHEM"))) throw new IllegalStateException("Unexpected cross-schema FK.");
                foreignColumns.computeIfAbsent(fk, ignored -> new ArrayList<>()).add(rs.getString("FKCOLUMN_NAME"));
                referenced.put(fk, rs.getString("PKTABLE_NAME"));
            }
        }
        Set<String> deferred = new TreeSet<>(), dependencies = new TreeSet<>();
        for (var fk : foreignColumns.entrySet()) {
            List<String> nullable = fk.getValue().stream().filter(c -> index.get(c).nullable()).toList();
            if (nullable.isEmpty()) dependencies.add(referenced.get(fk.getKey())); else deferred.addAll(nullable);
        }
        return new Target(new Table(name, cols, List.of()), List.copyOf(key.values()), deferred, dependencies);
    }

    private static List<Target> order(List<Target> targets) {
        List<Target> pending = new ArrayList<>(targets), result = new ArrayList<>(); Set<String> done = new HashSet<>();
        while (!pending.isEmpty()) {
            boolean progress = false;
            for (var iterator = pending.iterator(); iterator.hasNext();) {
                Target next = iterator.next();
                if (done.containsAll(next.dependencies())) { result.add(next); done.add(next.table().name()); iterator.remove(); progress = true; }
            }
            if (!progress) throw new IllegalStateException("Required FK cycle or excluded dependency; manual schema review required.");
        }
        return result;
    }

    private static void insert(Connection db, Table source, Target target) throws SQLException {
        List<Column> cols = target.table().columns();
        String sql = "INSERT INTO public." + q(source.name()) + " (" + String.join(",", cols.stream().map(c -> q(c.name())).toList())
                + ") VALUES (" + String.join(",", Collections.nCopies(cols.size(), "?")) + ")";
        try (var st = db.prepareStatement(sql)) {
            for (var row : project(source, cols)) {
                for (int i = 0; i < cols.size(); i++) bind(st, i + 1, cols.get(i), target.deferred().contains(cols.get(i).name()) ? null : row.get(i));
                st.addBatch();
            }
            st.executeBatch();
        }
    }

    private static void restoreOptionalReferences(Connection db, Table source, Target target) throws SQLException {
        if (target.deferred().isEmpty()) return;
        List<Column> cols = target.table().columns();
        Map<String,Integer> positions = new HashMap<>(); for (int i = 0; i < cols.size(); i++) positions.put(cols.get(i).name(), i);
        List<String> nullable = new ArrayList<>(target.deferred());
        String sql = "UPDATE public." + q(source.name()) + " SET " + String.join(",", nullable.stream().map(c -> q(c) + "=?").toList())
                + " WHERE " + String.join(" AND ", target.primaryKey().stream().map(c -> q(c) + "=?").toList());
        try (var st = db.prepareStatement(sql)) {
            for (var row : project(source, cols)) {
                if (nullable.stream().allMatch(c -> row.get(positions.get(c)) == null)) continue;
                int parameter = 1;
                for (String name : nullable) bind(st, parameter++, cols.get(positions.get(name)), row.get(positions.get(name)));
                for (String name : target.primaryKey()) bind(st, parameter++, cols.get(positions.get(name)), row.get(positions.get(name)));
                if (st.executeUpdate() != 1) throw new IllegalStateException("Could not restore optional relation in " + source.name());
            }
        }
    }

    private static List<List<String>> project(Table source, List<Column> target) {
        Map<String,Integer> positions = new HashMap<>(); for (int i = 0; i < source.columns().size(); i++) positions.put(source.columns().get(i).name(), i);
        List<List<String>> result = new ArrayList<>();
        for (var row : source.rows()) {
            List<String> copy = new ArrayList<>();
            for (Column column : target) copy.add(normalize(column, row.get(positions.get(column.name()))));
            result.add(copy);
        }
        return result;
    }

    private static void restartIdentity(Connection db, Target target) throws SQLException {
        for (Column column : target.table().columns()) {
            if (!column.identity()) continue;
            long next = 1;
            try (var st = db.createStatement(); var rs = st.executeQuery("SELECT max(" + q(column.name()) + ") FROM public." + q(target.table().name()))) {
                rs.next(); long max = rs.getLong(1); if (!rs.wasNull()) next = Math.max(1, Math.addExact(max, 1));
            }
            // ALTER RESTART is transactional; setval() is not and would survive a failed import.
            try (var st = db.createStatement()) { st.execute("ALTER TABLE public." + q(target.table().name()) + " ALTER COLUMN " + q(column.name()) + " RESTART WITH " + next); }
        }
    }

    private static void archive(Connection db, Snapshot snapshot) throws SQLException {
        Snapshot redacted = new Snapshot(snapshot.format(), snapshot.id(), snapshot.createdAt(), snapshot.sourceDatabase(), snapshot.sourceVersion(),
                snapshot.tables().stream().filter(t -> !AUTH_TABLES.contains(t.name())).toList());
        try (var st = db.createStatement()) {
            st.execute("CREATE SCHEMA " + ARCHIVE_SCHEMA);
            st.execute("REVOKE ALL ON SCHEMA " + ARCHIVE_SCHEMA + " FROM PUBLIC");
            st.execute("CREATE TABLE " + ARCHIVE_SCHEMA + ".source_snapshot (snapshot_id varchar(36) PRIMARY KEY, exported_at timestamptz NOT NULL, contents jsonb NOT NULL)");
            st.execute("REVOKE ALL ON ALL TABLES IN SCHEMA " + ARCHIVE_SCHEMA + " FROM PUBLIC");
        }
        try (var st = db.prepareStatement("INSERT INTO " + ARCHIVE_SCHEMA + ".source_snapshot VALUES (?, ?, CAST(? AS jsonb))")) {
            st.setString(1, snapshot.id()); st.setObject(2, OffsetDateTime.parse(snapshot.createdAt())); st.setString(3, JSON.writeValueAsString(redacted)); st.executeUpdate();
        }
    }

    private static Map<String,String> financialTotals(Connection db) throws SQLException {
        Map<String,String> result = new LinkedHashMap<>();
        for (String[] pair : new String[][]{{"customer_wallets","balance"},{"stores","e_wallet"},{"stores","pending_balance"},{"customer_wallet_entries","amount"}}) {
            try (var st = db.createStatement(); var rs = st.executeQuery("SELECT coalesce(sum(" + q(pair[1]) + "),0) FROM public." + q(pair[0]))) {
                rs.next(); result.put(pair[0] + "." + pair[1], rs.getBigDecimal(1).toPlainString());
            }
        }
        return result;
    }

    private static String digest(List<Column> columns, List<List<String>> rows) {
        try {
            List<String> normalized = new ArrayList<>();
            for (var row : rows) {
                List<String> cells = new ArrayList<>(); for (int i = 0; i < columns.size(); i++) cells.add(normalize(columns.get(i), row.get(i)));
                normalized.add(JSON.writeValueAsString(cells));
            }
            Collections.sort(normalized);
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            for (String row : normalized) { hash.update(row.getBytes(StandardCharsets.UTF_8)); hash.update((byte) '\n'); }
            return HexFormat.of().formatHex(hash.digest());
        } catch (GeneralSecurityException failure) { throw new IllegalStateException("SHA-256 unavailable"); }
    }

    private static List<String> tableNames(Connection db, String schema) throws SQLException {
        List<String> result = new ArrayList<>();
        try (var rs = db.getMetaData().getTables(null, schema, "%", new String[]{"TABLE"})) { while (rs.next()) result.add(rs.getString("TABLE_NAME")); }
        Collections.sort(result); return result;
    }

    private static List<Column> columns(Connection db, String schema, String table) throws SQLException {
        List<Column> result = new ArrayList<>();
        try (var rs = db.getMetaData().getColumns(null, schema, table, "%")) {
            while (rs.next()) result.add(new Column(rs.getString("COLUMN_NAME"), rs.getInt("DATA_TYPE"), rs.getString("TYPE_NAME"),
                    rs.getInt("COLUMN_SIZE"), rs.getInt("DECIMAL_DIGITS"), rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls, "YES".equals(rs.getString("IS_AUTOINCREMENT"))));
        }
        return result;
    }

    private static List<List<String>> readRows(Connection db, String schema, String table, List<Column> columns) throws SQLException {
        List<List<String>> result = new ArrayList<>();
        String sql = "SELECT " + String.join(",", columns.stream().map(c -> q(c.name())).toList()) + " FROM " + q(schema) + "." + q(table);
        try (var st = db.createStatement()) {
            st.setQueryTimeout(60);
            try (var rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    List<String> row = new ArrayList<>();
                    for (int i = 0; i < columns.size(); i++) row.add(readCell(rs, i + 1, columns.get(i)));
                    result.add(row);
                }
            }
        }
        return result;
    }

    private static String readCell(ResultSet rs, int index, Column column) throws SQLException {
        if (rs.getObject(index) == null) return null;
        return switch (column.jdbcType()) {
            case Types.BIT, Types.BOOLEAN -> Boolean.toString(rs.getBoolean(index));
            case Types.NUMERIC, Types.DECIMAL -> rs.getBigDecimal(index).toPlainString();
            case Types.DATE -> rs.getObject(index, LocalDate.class).toString();
            case Types.TIME -> rs.getObject(index, LocalTime.class).toString();
            case Types.TIMESTAMP -> rs.getObject(index, LocalDateTime.class).toString();
            case Types.TIMESTAMP_WITH_TIMEZONE, -155 -> rs.getObject(index, OffsetDateTime.class).toString();
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> Base64.getEncoder().encodeToString(rs.getBytes(index));
            default -> rs.getString(index);
        };
    }

    private static String normalize(Column column, String value) {
        if (value == null) return null;
        return switch (column.jdbcType()) {
            case Types.NUMERIC, Types.DECIMAL -> new BigDecimal(value).stripTrailingZeros().toPlainString();
            case Types.TIMESTAMP -> LocalDateTime.parse(value).truncatedTo(ChronoUnit.MICROS).toString();
            case Types.TIMESTAMP_WITH_TIMEZONE, -155 -> OffsetDateTime.parse(value).toInstant().truncatedTo(ChronoUnit.MICROS).toString();
            case Types.TIME -> LocalTime.parse(value).truncatedTo(ChronoUnit.MICROS).toString();
            case Types.FLOAT, Types.DOUBLE, Types.REAL -> {
                double number = Double.parseDouble(value);
                if (!Double.isFinite(number)) throw new IllegalArgumentException("Non-finite number in " + column.name());
                yield Double.toString(number);
            }
            default -> value;
        };
    }

    private static void bind(PreparedStatement st, int index, Column c, String value) throws SQLException {
        if (value == null) { st.setNull(index, c.jdbcType()); return; }
        switch (c.jdbcType()) {
            case Types.BOOLEAN, Types.BIT -> st.setBoolean(index, Boolean.parseBoolean(value));
            case Types.BIGINT -> st.setLong(index, Long.parseLong(value));
            case Types.INTEGER, Types.SMALLINT, Types.TINYINT -> st.setInt(index, Integer.parseInt(value));
            case Types.NUMERIC, Types.DECIMAL -> st.setBigDecimal(index, new BigDecimal(value));
            case Types.FLOAT, Types.DOUBLE, Types.REAL -> st.setDouble(index, Double.parseDouble(value));
            case Types.DATE -> st.setObject(index, LocalDate.parse(value));
            case Types.TIME -> st.setObject(index, LocalTime.parse(value).truncatedTo(ChronoUnit.MICROS));
            case Types.TIMESTAMP -> st.setObject(index, LocalDateTime.parse(value).truncatedTo(ChronoUnit.MICROS));
            case Types.TIMESTAMP_WITH_TIMEZONE -> st.setObject(index, OffsetDateTime.parse(value));
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> st.setBytes(index, Base64.getDecoder().decode(value));
            default -> st.setString(index, value);
        }
    }

    private static Map<String,Table> byName(Snapshot snapshot) {
        Map<String,Table> result = new LinkedHashMap<>(); snapshot.tables().forEach(t -> result.put(t.name(), t)); return result;
    }

    private static long count(Connection db, String schema, String table) throws SQLException {
        try (var st = db.createStatement(); var rs = st.executeQuery("SELECT count(*) FROM " + q(schema) + "." + q(table))) { rs.next(); return rs.getLong(1); }
    }

    private static String q(String identifier) {
        if (identifier == null || !identifier.matches("[a-zA-Z_][a-zA-Z0-9_]*")) throw new IllegalArgumentException("Unsafe SQL identifier.");
        return "\"" + identifier + "\"";
    }

    private static String required(String name) {
        String value = System.getenv(name); if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + name); return value;
    }
}
