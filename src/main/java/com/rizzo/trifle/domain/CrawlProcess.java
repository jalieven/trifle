package com.rizzo.trifle.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.RandomStringUtils;

import java.io.Serializable;
import java.util.List;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
public class CrawlProcess implements Serializable {

    private static final String CRAWL_PROCESS_ID_PREFIX = "trifle-process-";

    private String id;

    private List<CrawlTask> crawlSteps;

    public String getId() {
        return id;
    }

    public CrawlProcess setId(String id) {
        this.id = id;
        return this;
    }

    @JsonIgnore
    public Integer getStepCount() {
        return getCrawlSteps().size();
    }

    @JsonIgnore
    public CrawlTask getFirstCrawlTask() {
        return getCrawlSteps().get(0);
    }

    public List<CrawlTask> getCrawlSteps() {
        if (this.crawlSteps == null) {
            this.crawlSteps = Lists.newArrayList();
        }
        return crawlSteps;
    }

    public CrawlProcess setCrawlSteps(List<CrawlTask> crawlSteps) {
        if(crawlSteps != null) {
            CrawlTask firstCrawlTask = Iterables.getFirst(crawlSteps, null);
            if(firstCrawlTask != null) {
                if(StringUtils.isBlank(firstCrawlTask.getUrl())) {
                    throw new IllegalArgumentException("The first CrawlTask should contain a URL!");
                }
            } else {
                throw new IllegalArgumentException("Have you provided CrawlTasks for the CrawlProcess?");
            }
        }
        this.crawlSteps = crawlSteps;
        return this;
    }

    @JsonIgnore
    public CrawlProcess checkIds() {
        if (StringUtils.isBlank(this.id)) {
            this.id = CRAWL_PROCESS_ID_PREFIX + RandomStringUtils.randomAlphabetic(32);
        }
        for (CrawlTask crawlStep : crawlSteps) {
            crawlStep.setCrawlProcessId(this.id);
        }
        return this;
    }

    @JsonIgnore
    public CrawlTask getCrawlTask(Integer crawlStep) {
        if (crawlStep < getStepCount()) {
            return this.getCrawlSteps().get(crawlStep);
        } else {
            return null;
        }
    }
}
