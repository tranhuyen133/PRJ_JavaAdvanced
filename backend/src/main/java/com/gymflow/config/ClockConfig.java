package com.gymflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Mọi phép tính "hôm nay" dùng chung Clock này để test có thể cố định thời gian. */
@Configuration
public class ClockConfig {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}
