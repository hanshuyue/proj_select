package com.scaffold.admin.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DbInit {
    public static void main(String[] args) throws Exception {
        File cfg = new File("src/main/resources/application-dev.yml");
        if (!cfg.exists()) {
            System.err.println("application-dev.yml not found at src/main/resources");
            System.exit(2);
        }

        String url = null, user = null, pass = null;
        boolean inDatasource = false;
        try (BufferedReader r = new BufferedReader(new FileReader(cfg))) {
            String line;
            while ((line = r.readLine()) != null) {
                String raw = line;
                line = line.trim();
                if (line.startsWith("datasource:") || line.contains("datasource:")) {
                    inDatasource = true;
                    continue;
                }
                // leave datasource block when we hit another top-level child like 'data:'
                if (inDatasource && (line.startsWith("data:") || line.matches("^[^ ].*:" ))) {
                    inDatasource = false;
                }
                if (inDatasource) {
                    if (line.startsWith("url: ") || line.startsWith("url:")) {
                        url = line.substring(line.indexOf(':') + 1).trim();
                    } else if (line.startsWith("username: ") || line.startsWith("username:")) {
                        user = line.substring(line.indexOf(':') + 1).trim();
                    } else if (line.startsWith("password: ") || line.startsWith("password:")) {
                        pass = line.substring(line.indexOf(':') + 1).trim();
                    }
                }
            }
        }

        if (url == null || user == null) {
            System.err.println("无法从 application-dev.yml 解析到 url/username。");
            System.exit(3);
        }

        // extract host, port, db
        Pattern p = Pattern.compile("jdbc:mysql://([^:/?#]+)(?::(\\d+))?/(\\w+)(.*)");
        Matcher m = p.matcher(url);
        if (!m.find()) {
            System.err.println("无法解析 JDBC URL: " + url);
            System.exit(4);
        }
        String host = m.group(1);
        String port = m.group(2) == null ? "3306" : m.group(2);
        String db = m.group(3);
        String tail = m.group(4) == null ? "" : m.group(4);

        String serverUrl = String.format("jdbc:mysql://%s:%s/%s?%s", host, port, "", tail.startsWith("?") ? tail.substring(1) : tail);
        // ensure create database param not present here
        if (serverUrl.endsWith("?")) serverUrl = serverUrl.substring(0, serverUrl.length()-1);

        System.out.println("Parsed DB host=" + host + " port=" + port + " db=" + db);
        System.out.println("Connecting to server URL: " + serverUrl);

        // support a 'fix' mode to drop and recreate database with proper settings
        if (args != null && args.length > 0 && "fix".equals(args[0])) {
            System.out.println("Running fix: DROP and recreate database with utf8 charset");
            try (Connection c = DriverManager.getConnection(serverUrl, user, pass);
                 Statement s = c.createStatement()) {
                System.out.println("Setting MySQL InnoDB parameters for new tables...");
                try {
                    s.execute("SET GLOBAL innodb_large_prefix=ON");
                } catch (Exception e) {
                    System.out.println("  (innodb_large_prefix not available, likely MySQL 8.0+)");
                }
                try {
                    s.execute("SET GLOBAL innodb_default_row_format=DYNAMIC");
                } catch (Exception e) {
                    System.out.println("  warn: innodb_default_row_format=DYNAMIC failed: " + e.getMessage());
                }
                System.out.println("Dropping existing database if present...");
                s.execute(String.format("DROP DATABASE IF EXISTS %s", db));
                System.out.println("Creating fresh database with utf8 charset...");
                s.execute(String.format("CREATE DATABASE %s CHARACTER SET utf8 COLLATE utf8_general_ci", db));
                System.out.println("Fix mode complete. Continuing to import SQL files...");
            }
            // fall through to import SQL, skipping the  normal create
        } else {
            // connect to server (no database) - normal mode
            try (Connection c = DriverManager.getConnection(serverUrl, user, pass)) {
                try (Statement s = c.createStatement()) {
                    String create = String.format("CREATE DATABASE IF NOT EXISTS %s CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci", db);
                    System.out.println("Executing: " + create);
                    s.execute(create);
                }
            }
        }

        // import SQL files from ../sql
        Path sqlDir = Path.of("..", "sql");
        if (!Files.exists(sqlDir)) {
            System.out.println("sql 目录不存在: " + sqlDir.toAbsolutePath());
            return;
        }

        List<Path> files = Files.list(sqlDir).filter(pf -> pf.getFileName().toString().endsWith(".sql")).sorted(Comparator.comparing(Path::toString)).toList();
        if (files.isEmpty()) {
            System.out.println("没有找到初始化 SQL 文件");
            return;
        }

        // connect to the created database
        String dbUrl = String.format("jdbc:mysql://%s:%s/%s?%s", host, port, db, tail.startsWith("?") ? tail.substring(1) : tail);
        System.out.println("Connecting to database URL: " + dbUrl);

        try (Connection c = DriverManager.getConnection(dbUrl, user, pass)) {
            for (Path f : files) {
                System.out.println("Importing: " + f.getFileName());
                String content = Files.readString(f);
                // naive split by semicolon
                String[] parts = content.split(";\\s*\\n");
                try (Statement s = c.createStatement()) {
                    for (String part : parts) {
                        String sql = part.trim();
                        if (sql.isEmpty()) continue;
                        // skip comments
                        if (sql.startsWith("--") || sql.startsWith("/*")) continue;
                        // skip PREPARE/EXECUTE statements that don't work with naive split
                        if (sql.toUpperCase().contains("PREPARE ") || sql.toUpperCase().startsWith("EXECUTE")) {
                            System.out.println("Skipping PREPARE/EXECUTE statement: " + (sql.length() > 80 ? sql.substring(0, 80) + "..." : sql));
                            continue;
                        }
                        System.out.println("Executing statement (truncated): " + (sql.length() > 120 ? sql.substring(0, 120) + "..." : sql));
                        try {
                            s.execute(sql);
                        } catch (Exception e) {
                            System.out.println("  WARN: SQL execution failed: " + e.getMessage());
                            System.out.println("  Continuing with next statement...");
                        }
                    }
                }
            }
        }

        System.out.println("数据库初始化完成");
    }
}
