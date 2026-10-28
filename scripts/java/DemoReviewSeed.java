import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Explicit, repeatable sample data. No customer orders, sales, inventory or money are created. */
public class DemoReviewSeed {
    static final String PREFIX = "[Đánh giá mẫu] ";
    static final String USER_PREFIX = "demo_review_";
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    record Flower(long id, String name, int sold, Long store, String image, int existing) {}

    static int target(int sold) {
        if (sold <= 0) return 0;
        if (sold == 1) return 1;
        return sold < 20 ? 2 : sold < 50 ? 3 : sold < 80 ? 4 : sold < 120 ? 5 : 6;
    }
    static int stars(long product, int index) { return new int[]{5,4,5,4,5,3}[(int)((product+index)%6)]; }
    static String content(Flower f, int index) {
        String[] comments = {
            "Tông màu hài hòa, phần hoa chính nổi bật và tổng thể dễ phối với nhiều không gian.",
            "Bố cục gọn gàng, giấy gói và nơ có thể phối cùng tông để làm quà tặng sinh nhật.",
            "Các lớp hoa tạo chiều sâu, phần lá phụ giúp mẫu trông tự nhiên và mềm mại hơn.",
            "Màu sắc nhẹ nhàng, phù hợp gửi lời chúc hoặc làm điểm nhấn cho bàn tiếp khách.",
            "Kiểu dáng trang nhã; có thể điều chỉnh kích thước và lời nhắn theo dịp tặng hoa.",
            "Mẫu có bố cục rõ ràng; khi đặt thật nên trao đổi trước về sắc độ hoa theo mùa."
        };
        String name=f.name.length()>200 ? f.name.substring(0,200) : f.name;
        return PREFIX+name+": "+comments[index%comments.length]+" Ảnh minh họa từ ảnh sản phẩm; đây là dữ liệu demo.";
    }
    static String image(Flower f) {
        if (f.image!=null && !f.image.isBlank() && f.image.length()<=500
                && (f.image.startsWith("https://") || f.image.startsWith("/images/"))) return f.image;
        String name=f.name.toLowerCase(Locale.ROOT);
        if(name.contains("kệ")) return "/images/ke_hoa_stand.jpg";
        if(name.contains("giỏ")) return "/images/gio_hoa_basket.jpg";
        if(name.contains("bình")) return "/images/binh_hoa_vase.jpg";
        return "/images/demo-rose-bouquet.jpg";
    }
    static Connection connect() throws Exception {
        Properties config=new Properties();
        try(var reader=Files.newBufferedReader(Path.of(".env.aiven"))){config.load(reader);}
        Properties auth=new Properties();
        for(String[] pair:new String[][]{{"user","AIVEN_USER"},{"password","AIVEN_PASSWORD"},{"sslrootcert","AIVEN_CA_CERT"}})
            auth.setProperty(pair[0],Objects.requireNonNull(config.getProperty(pair[1]),"Missing local setting "+pair[1]));
        auth.setProperty("sslmode","verify-full");auth.setProperty("connectTimeout","15");auth.setProperty("socketTimeout","60");
        auth.setProperty("ApplicationName","flower-demo-review-seed");
        String url="jdbc:postgresql://"+config.getProperty("AIVEN_HOST")+":"+config.getProperty("AIVEN_PORT")+"/"+config.getProperty("AIVEN_DATABASE");
        try { return DriverManager.getConnection(url,auth); }
        finally { auth.clear();config.clear(); }
    }
    static List<Flower> flowers(Connection c) throws SQLException {
        var list=new ArrayList<Flower>();
        try(var s=c.createStatement();var r=s.executeQuery("""
            SELECT p.id,p.name,coalesce(p.sold,0),p.store_id,p.image_url,
                   (SELECT count(*) FROM reviews r WHERE r.product_id=p.id)
            FROM products p LEFT JOIN stores s ON s.id=p.store_id
            WHERE coalesce(p.is_deleted,false)=false AND p.is_active=true AND coalesce(p.sold,0)>0
              AND (p.store_id IS NULL OR (s.status=true AND s.is_open=true)) ORDER BY p.id
            """)) {
            while(r.next()) list.add(new Flower(r.getLong(1),r.getString(2),r.getInt(3),r.getObject(4,Long.class),r.getString(5),r.getInt(6)));
        }
        return list;
    }
    static int printPlan(List<Flower> flowers) {
        int rows=0,products=0;
        for(var f:flowers) {
            int add=Math.max(0,target(f.sold)-f.existing);
            if(add>0) products++;
            rows+=add;
            System.out.printf(Locale.ROOT,"#%d | %s | sold=%d | existing=%d | target=%d | add=%d%n",f.id,f.name,f.sold,f.existing,target(f.sold),add);
        }
        System.out.println("PLAN products_to_update="+products+" reviews_to_add="+rows);
        return rows;
    }
    static long ensureUser(Connection c,int index,Properties snapshot) throws SQLException {
        String username=USER_PREFIX+String.format(Locale.ROOT,"%02d",index+1);
        try(var s=c.prepareStatement("SELECT id,is_active,full_name,account_disabled_by_admin FROM users WHERE username=?")) {
            s.setString(1,username);
            try(var r=s.executeQuery()) {
                if(r.next()) {
                    if(r.getBoolean(2) || !r.getString(3).contains("Demo") || !r.getBoolean(4))
                        throw new SQLException("Sample username collision; existing account left unchanged");
                    return r.getLong(1);
                }
            }
        }
        try(var s=c.prepareStatement("""
            INSERT INTO users (username,email,full_name,password,role,is_active,is_email_active,account_disabled_by_admin,
                retired_delivery_account,point,e_wallet,created_date,created_at,creation_time_known,avatar_url)
            VALUES (?,?,?,?,'USER',false,false,true,false,0,0,?,?,true,'/images/demo-review-avatar.svg') RETURNING id
            """)) {
            s.setString(1,username);s.setString(2,username+"@example.invalid");
            s.setString(3,String.format(Locale.ROOT,"Khách mẫu %02d (Demo)",index+1));
            s.setString(4,"!disabled-demo:"+UUID.randomUUID());
            s.setObject(5,LocalDate.now(ZONE));s.setObject(6,LocalDateTime.now(ZONE));
            long id;
            try(var r=s.executeQuery()){r.next();id=r.getLong(1);}
            try(var role=c.prepareStatement("INSERT INTO user_roles (user_id,role) VALUES (?,'USER')")){role.setLong(1,id);role.executeUpdate();}
            snapshot.setProperty("user."+id,username);return id;
        }
    }
    static void snapshotRating(Connection c,String table,long id,Properties snapshot) throws SQLException {
        try(var s=c.prepareStatement("SELECT rating FROM "+table+" WHERE id=? FOR UPDATE")) {
            s.setLong(1,id);
            try(var r=s.executeQuery()) { if(!r.next()) throw new SQLException("Missing rating row");String value=r.getString(1);snapshot.setProperty(table+"."+id,value==null?"NULL":value); }
        }
    }
    static int seed(Connection c,List<Flower> list,Properties snapshot) throws SQLException {
        var stores=new TreeSet<Long>();
        for(var f:list)if(target(f.sold)>f.existing&&f.store!=null)stores.add(f.store);
        for(long id:stores)snapshotRating(c,"stores",id,snapshot);
        for(var f:list)if(target(f.sold)>f.existing)snapshotRating(c,"products",f.id,snapshot);
        int added=0;
        long[] sampleUsers=new long[6];
        for(var f:list) {
            int remaining=Math.max(0,target(f.sold)-f.existing);
            if(remaining==0)continue;
            for(int i=0;i<6&&remaining>0;i++) {
                if(sampleUsers[i]==0)sampleUsers[i]=ensureUser(c,i,snapshot);
                long user=sampleUsers[i];
                try(var exists=c.prepareStatement("SELECT 1 FROM reviews WHERE user_id=? AND product_id=?")) {
                    exists.setLong(1,user);exists.setLong(2,f.id);
                    try(var r=exists.executeQuery()){if(r.next())continue;}
                }
                try(var s=c.prepareStatement("""
                    INSERT INTO reviews (user_id,product_id,store_id,order_id,rating,content,image_url,created_at,updated_at)
                    VALUES (?,?,?,NULL,?,?,?,?,?) RETURNING id
                    """)) {
                    s.setLong(1,user);s.setLong(2,f.id);
                    if(f.store==null)s.setNull(3,Types.BIGINT);else s.setLong(3,f.store);
                    s.setInt(4,stars(f.id,i));s.setString(5,content(f,i));s.setString(6,image(f));
                    s.setObject(7,LocalDateTime.now(ZONE));s.setObject(8,LocalDateTime.now(ZONE));
                    try(var r=s.executeQuery()){r.next();snapshot.setProperty("review."+r.getLong(1),Long.toString(f.id));}
                }
                remaining--;added++;
            }
            try(var s=c.prepareStatement("UPDATE products SET rating=(SELECT round(avg(rating)::numeric,1) FROM reviews WHERE product_id=?) WHERE id=?")) {
                s.setLong(1,f.id);s.setLong(2,f.id);s.executeUpdate();
            }
        }
        for(long id:stores)try(var s=c.prepareStatement("UPDATE stores SET rating=(SELECT round(avg(rating)::numeric,1) FROM reviews WHERE store_id=?) WHERE id=?")) {
            s.setLong(1,id);s.setLong(2,id);s.executeUpdate();
        }
        return added;
    }
    static String businessState(Connection c) throws SQLException {
        var state=new ArrayList<String>();
        for(String query:List.of(
                "SELECT count(*),coalesce(sum(total_amount),0),coalesce(string_agg(id||':'||status||':'||payment_status,',' ORDER BY id),'') FROM orders",
                "SELECT count(*),coalesce(sum(amount),0) FROM payments",
                "SELECT count(*),coalesce(sum(amount),0) FROM transactions",
                "SELECT count(*),coalesce(sum(quantity),0) FROM store_inventories",
                "SELECT count(*),coalesce(sum(sold),0) FROM products",
                "SELECT count(*),coalesce(sum(balance),0) FROM customer_wallets",
                "SELECT count(*),coalesce(sum(amount),0) FROM customer_wallet_entries",
                "SELECT coalesce(sum(e_wallet),0),coalesce(sum(pending_balance),0) FROM stores",
                "SELECT coalesce(sum(e_wallet),0) FROM users")) {
            try(var statement=c.createStatement();var rs=statement.executeQuery(query)) {
                rs.next();for(int i=1;i<=rs.getMetaData().getColumnCount();i++)state.add(rs.getString(i));
            }
        }
        return String.join("|",state);
    }
    static void verify(Connection c,Properties snapshot) throws SQLException {
        for(String key:snapshot.stringPropertyNames())if(key.startsWith("review.")) {
            try(var s=c.prepareStatement("SELECT r.content,r.rating,r.image_url,r.order_id,u.is_active,u.full_name FROM reviews r JOIN users u ON u.id=r.user_id WHERE r.id=?")) {
                s.setLong(1,Long.parseLong(key.substring(7)));
                try(var r=s.executeQuery()) {
                    if(!r.next()||!r.getString(1).startsWith(PREFIX)||r.getString(1).length()<50||r.getInt(2)<1||r.getInt(2)>5
                            ||r.getString(3)==null||r.getString(3).isBlank()||r.getObject(4)!=null||r.getBoolean(5)||!r.getString(6).contains("Demo"))
                        throw new SQLException("Sample review verification failed");
                }
            }
        }
    }
    static void undo(Connection c,Path backup) throws Exception {
        Properties snapshot=new Properties();
        try(var reader=Files.newBufferedReader(backup)){snapshot.load(reader);}
        String before=businessState(c);int removed=0;
        for(String key:snapshot.stringPropertyNames())if(key.startsWith("review.")) {
            try(var s=c.prepareStatement("""
                DELETE FROM reviews WHERE id=? AND product_id=? AND content LIKE ?
                  AND user_id IN (SELECT id FROM users WHERE username LIKE 'demo_review_%'
                    AND is_active=false AND account_disabled_by_admin=true AND full_name LIKE '%(Demo)')
                """)) {
                s.setLong(1,Long.parseLong(key.substring(7)));s.setLong(2,Long.parseLong(snapshot.getProperty(key)));
                s.setString(3,PREFIX+"%");removed+=s.executeUpdate();
            }
        }
        for(String table:List.of("stores","products"))for(String key:snapshot.stringPropertyNames())if(key.startsWith(table+".")) {
            long id=Long.parseLong(key.substring(table.length()+1));String column=table.equals("stores")?"store_id":"product_id";
            try(var s=c.prepareStatement("UPDATE "+table+" SET rating=coalesce((SELECT round(avg(rating)::numeric,1) FROM reviews WHERE "+column+"=?),?) WHERE id=?")) {
                s.setLong(1,id);String old=snapshot.getProperty(key);
                if(old.equals("NULL"))s.setNull(2,Types.DOUBLE);else s.setDouble(2,Double.parseDouble(old));
                s.setLong(3,id);s.executeUpdate();
            }
        }
        for(String key:snapshot.stringPropertyNames())if(key.startsWith("user.")) {
            long id=Long.parseLong(key.substring(5));
            try(var s=c.prepareStatement("SELECT 1 FROM reviews WHERE user_id=?")) {
                s.setLong(1,id);try(var r=s.executeQuery()){if(r.next())continue;}
            }
            try(var s=c.prepareStatement("DELETE FROM user_roles WHERE user_id=?")){s.setLong(1,id);s.executeUpdate();}
            try(var s=c.prepareStatement("DELETE FROM users WHERE id=? AND username=? AND is_active=false AND account_disabled_by_admin=true")) {
                s.setLong(1,id);s.setString(2,snapshot.getProperty(key));s.executeUpdate();
            }
        }
        if(!before.equals(businessState(c)))throw new SQLException("Business state changed during rollback");
        c.commit();System.out.println("ROLLED BACK: "+removed+" marked sample reviews; original reviews retained.");
    }
    static void selfTest() {
        int[] input={-1,0,1,2,19,20,49,50,79,80,119,120,999};
        int[] expected={0,0,1,2,2,3,3,4,4,5,5,6,6};
        for(int i=0;i<input.length;i++)if(target(input[i])!=expected[i])throw new AssertionError("Target count");
        Flower f=new Flower(1,"Bó Hoa Hồng",120,null,"https://example.invalid/hoa.jpg",0);
        for(int i=0;i<6;i++)if(content(f,i).length()<50||content(f,i).length()>1000||!content(f,i).startsWith(PREFIX)||stars(f.id,i)<1||stars(f.id,i)>5)throw new AssertionError("Content/rating");
        if(!image(f).equals(f.image)||!image(new Flower(2,"Giỏ hoa",10,null,null,0)).equals("/images/gio_hoa_basket.jpg"))throw new AssertionError("Image");
        System.out.println("PASS: sales boundaries, six marked sample texts, ratings and image fallback.");
    }
    public static void main(String[] args) throws Exception {
        String mode=args.length==0?"--plan":args[0];
        if(mode.equals("--self-test")){selfTest();return;}
        if(!List.of("--plan","--rehearse","--apply","--rollback").contains(mode))throw new IllegalArgumentException("Use --plan, --rehearse, --apply, --rollback snapshot or --self-test");
        try(var c=connect()) {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            c.setAutoCommit(false);c.setReadOnly(mode.equals("--plan"));
            try {
                if(!mode.equals("--plan"))try(var s=c.createStatement()){s.execute("SELECT pg_advisory_xact_lock(hashtext('flower-demo-reviews-v1'))");s.execute("SET LOCAL lock_timeout='15s'");}
                if(mode.equals("--rollback")) {
                    if(args.length!=2)throw new IllegalArgumentException("Supply snapshot path");
                    undo(c,Path.of(args[1]));return;
                }
                var list=flowers(c);int expected=printPlan(list);
                if(mode.equals("--plan")) {
                    Files.createDirectories(Path.of("target"));
                    Files.write(Path.of("target/demo-review-images.tsv"),list.stream().map(f -> f.id+"\t"+image(f)).toList());
                }
                if(mode.equals("--plan")||expected==0){c.rollback();return;}
                Properties snapshot=new Properties();snapshot.setProperty("created_at",LocalDateTime.now(ZONE).toString());
                String before=businessState(c);
                int added=seed(c,list,snapshot);
                verify(c,snapshot);
                if(!before.equals(businessState(c)))throw new SQLException("Orders, sales, inventory or money changed; rolling back");
                if(added!=expected)throw new SQLException("Unexpected inserted count; transaction rolled back");
                if(mode.equals("--rehearse")){c.rollback();System.out.println("REHEARSAL PASS: "+added+" sample reviews inserted, checked, then rolled back.");return;}
                Path dir=Path.of(".tools/demo-review-backups");Files.createDirectories(dir);
                Path backup=dir.resolve("reviews-"+System.currentTimeMillis()+".properties");
                try(var writer=Files.newBufferedWriter(backup)){snapshot.store(writer,"Sample review IDs and previous ratings; no credentials.");}
                c.commit();System.out.println("APPLIED: "+added+" marked sample reviews. Snapshot: "+backup);
            }catch(Exception e){c.rollback();throw e;}
        }
    }
}
