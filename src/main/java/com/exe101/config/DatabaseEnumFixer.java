package com.exe101.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseEnumFixer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixEnum() {
        try {
            jdbcTemplate.execute("ALTER TYPE prod.credential_provider ADD VALUE IF NOT EXISTS 'FACEBOOK'");
            System.out.println("✅ Đã tự động thêm FACEBOOK vào enum credential_provider thành công!");
        } catch (Exception e) {
            System.out.println("⚠️ Không thể tự động thêm FACEBOOK vào enum: " + e.getMessage());
        }
    }
}
