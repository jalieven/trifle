package com.rizzo.trifle.util;

import com.google.common.collect.Sets;
import com.rizzo.trifle.elasticsearch.CrawlResultRepository;
import com.rizzo.trifle.elasticsearch.DownloadResultRepository;
import net.logstash.logback.encoder.org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CleanSweeper implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanSweeper.class);

    @Value("${clean.redis}")
    private boolean cleanRedis;

    @Value("${clean.elasticsearch}")
    private boolean cleanElasticsearch;

    @Value("${clean.downloads}")
    private boolean cleanDownloads;

    @Autowired
    private DownloadResultRepository downloadResultRepository;

    @Autowired
    private CrawlResultRepository crawlResultRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${downloads.folder-location}")
    private Resource downloads;

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            if(cleanRedis) {
                LOGGER.info("Clean sweeping Redis...");
                final Set<String> springKeys = this.stringRedisTemplate.keys("spring*");
                final Set<String> crawlerKeys = this.stringRedisTemplate.keys("trifle*");
                this.stringRedisTemplate.delete(Sets.union(springKeys, crawlerKeys));
                this.stringRedisTemplate.delete("keys.spring.metrics.");
            }
            if(cleanElasticsearch) {
                LOGGER.info("Clean sweeping ElasticSearch...");
                this.downloadResultRepository.deleteAll();
                this.crawlResultRepository.deleteAll();
            }
            if(cleanDownloads) {
                if (downloads.getFile().exists()) {
                    LOGGER.info("Clean sweeping Downloads folder...");
                    FileUtils.cleanDirectory(downloads.getFile());
                } else {
                    FileUtils.forceMkdir(downloads.getFile());
                    LOGGER.info("No need to clean sweeping Downloads folder (it does not exist) creating it now...");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception while clean sweeping...", e);
        }

    }
}
