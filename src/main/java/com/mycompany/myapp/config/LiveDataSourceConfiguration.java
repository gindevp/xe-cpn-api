package com.mycompany.myapp.config;

/**
 * Không thay pool của Spring. API luôn dùng database trong biến môi trường Railway.
 * Đổi pool sang database khác chỉ làm khi cổng database mới đã mở và có bước chuyển riêng.
 */
public final class LiveDataSourceConfiguration {

    private LiveDataSourceConfiguration() {}
}
