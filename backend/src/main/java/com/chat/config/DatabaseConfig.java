package com.chat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.File;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${app.db.name:chat_server.db}")
    private String dbName;

    @Bean
    public DataSource dataSource() {
        // Xác định đường dẫn file SQLite luôn ở thư mục gốc project (d:\UngDungChat\chat_server.db)
        File dbFile;
        try {
            File canonicalDir = new File(".").getCanonicalFile();
            if (canonicalDir.getName().equalsIgnoreCase("backend")) {
                dbFile = new File(canonicalDir.getParentFile(), dbName);
            } else {
                dbFile = new File(canonicalDir, dbName);
            }
        } catch (Exception e) {
            dbFile = new File(dbName);
        }

        String dbPath = dbFile.getAbsolutePath();
        logger.info("Connecting to SQLite database at: {}", dbPath);

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dbPath);

        // Khởi tạo bảng ngay khi kết nối
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute("PRAGMA busy_timeout = 5000;");
            jdbcTemplate.execute("PRAGMA journal_mode = WAL;");

            // Giữ nguyên 100% schema cũ của Server.py
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY, " +
                    "username TEXT UNIQUE, " +
                    "password_hash TEXT, " +
                    "created_at TEXT)");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS public_messages (" +
                    "id INTEGER PRIMARY KEY, " +
                    "username TEXT, " +
                    "message TEXT, " +
                    "timestamp TEXT)");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS private_messages (" +
                    "id INTEGER PRIMARY KEY, " +
                    "sender TEXT, " +
                    "receiver TEXT, " +
                    "message TEXT, " +
                    "timestamp TEXT)");

            // Bổ sung các cột phục vụ quản trị an toàn (không phá vỡ schema cũ của Server.py)
            try { jdbcTemplate.execute("ALTER TABLE users ADD COLUMN role TEXT DEFAULT 'user'"); } catch (Exception ignored) {}
            try { jdbcTemplate.execute("ALTER TABLE users ADD COLUMN status TEXT DEFAULT 'active'"); } catch (Exception ignored) {}
            try { jdbcTemplate.execute("ALTER TABLE users ADD COLUMN locked_at TEXT"); } catch (Exception ignored) {}
            try { jdbcTemplate.execute("ALTER TABLE users ADD COLUMN deleted_at TEXT"); } catch (Exception ignored) {}

            // Bảng lưu vết thao tác quản trị (Audit Log)
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "timestamp TEXT, " +
                    "admin_username TEXT, " +
                    "action TEXT, " +
                    "target TEXT, " +
                    "result TEXT, " +
                    "details TEXT)");

            Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            logger.info("SQLite Database initialized successfully! Total users: {}", userCount);
        } catch (Exception e) {
            logger.error("Error initializing SQLite database schema: {}", e.getMessage(), e);
        }

        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
