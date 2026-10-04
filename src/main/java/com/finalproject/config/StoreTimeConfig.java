package com.finalproject.config;

import org.springframework.context.annotation.*;
import java.time.*;
@Configuration
public class StoreTimeConfig {
    @Bean public Clock applicationClock() { return Clock.system(ZoneId.of("Asia/Bangkok")); }
}
