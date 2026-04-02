package com.platformdemo.identity.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class TimeConfig {
    @Bean
    fun identityClock(): Clock = Clock.systemUTC()
}
