package com.rizzo.trifle.redis;

import com.google.common.collect.Maps;
import com.rizzo.trifle.aop.LogPerformance;
import com.rizzo.trifle.domain.CrawlProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RedisCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCache.class);

    public static final String SPRING_CACHE_CRAWL_PROCESS = "spring.cache.crawl.process";

    private Map<String, CrawlProcess> localCrawlProcessCache = Maps.newHashMap();

    @LogPerformance
    @CachePut(value = SPRING_CACHE_CRAWL_PROCESS, key="#p0.id")
    public CrawlProcess cachePut(CrawlProcess crawlProcess) {
        LOGGER.debug("Putting CrawlProcess into RedisCache '" + SPRING_CACHE_CRAWL_PROCESS + "'");
        localCrawlProcessCache.put(crawlProcess.getId(), crawlProcess);
        return crawlProcess;
    }

    @LogPerformance
    @Cacheable(value = "spring.cache.crawl.process")
    public CrawlProcess cacheGet(String crawlProcessId) {
        LOGGER.debug("Getting CrawlProcess from RedisCache '" + SPRING_CACHE_CRAWL_PROCESS + "'");
        return this.localCrawlProcessCache.get(crawlProcessId);
    }

}
