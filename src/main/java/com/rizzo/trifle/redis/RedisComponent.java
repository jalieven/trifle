package com.rizzo.trifle.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rizzo.trifle.aop.PerformanceLogAspect;
import com.rizzo.trifle.domain.CrawlProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.repository.redis.RedisMetricRepository;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

@Component
@EnableCaching
public class RedisComponent {

    @Value("${spring.redis.cache.default-expiration-seconds}")
    private Long redisCacheDefaultExpiration;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    public RedisMetricRepository redisMetricRepository() {
        return new RedisMetricRepository(this.redisConnectionFactory);
    }

    @Bean
    public PerformanceLogAspect performanceLogAspect() {
        return new PerformanceLogAspect();
    }

    @Bean
    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping();
        return objectMapper;
    }

    @Bean
    public RedisCacheManager redisCacheManager() {
        final RedisCacheManager redisCacheManager = new RedisCacheManager(crawlProcessRedisTemplate());
        redisCacheManager.setDefaultExpiration(redisCacheDefaultExpiration);
        return redisCacheManager;
    }

    @Bean
    public RedisTemplate crawlProcessRedisTemplate() {
        final RedisTemplate redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        final StringRedisSerializer keySerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(keySerializer);
        final Jackson2JsonRedisSerializer<CrawlProcess> valueSerializer =
                new Jackson2JsonRedisSerializer<>(CrawlProcess.class);
        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
