package ru.blogic.muzedodevwebutils;


import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.blogic.muzedodevwebutils.info.GetServerInfoResponse;

import java.time.Duration;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig implements CachingConfigurer {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("getServerInfo", "getServerLog");
    }
}
