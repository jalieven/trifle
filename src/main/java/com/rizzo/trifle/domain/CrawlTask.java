package com.rizzo.trifle.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Maps;
import com.rizzo.trifle.filter.CrawlFilterAlgorithm;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Pattern;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
public class CrawlTask implements Serializable {

    private String crawlProcessId;

    private Integer crawlStep;

    private Integer politeness;

    private String url;

    private Integer priority;

    private Boolean download;

    private String resultQuery;

    private Map<String, String> resultAttrQueries;

    private String[] resultAttrConditionals;

    private Pattern crawlFilter;

    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
    private CrawlFilterAlgorithm crawlFilterAlgorithm;

    private Map<Integer, Pattern> crawlStepPatterns;

    public String getCrawlProcessId() {
        return crawlProcessId;
    }

    public CrawlTask setCrawlProcessId(String crawlProcessId) {
        this.crawlProcessId = crawlProcessId;
        return this;
    }

    public Integer getCrawlStep() {
        return crawlStep;
    }

    public CrawlTask setCrawlStep(Integer crawlStep) {
        this.crawlStep = crawlStep;
        return this;
    }

    public Integer getPoliteness() {
        return politeness;
    }

    public CrawlTask setPoliteness(Integer politeness) {
        this.politeness = politeness;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public CrawlTask setUrl(String url) {
        this.url = url;
        return this;
    }

    public Integer getPriority() {
        return priority;
    }

    public CrawlTask setPriority(Integer priority) {
        this.priority = priority;
        return this;
    }

    public Boolean getDownload() {
        return download;
    }

    public CrawlTask setDownload(Boolean download) {
        this.download = download;
        return this;
    }

    public String getResultQuery() {
        return resultQuery;
    }

    public CrawlTask setResultQuery(String resultQuery) {
        this.resultQuery = resultQuery;
        return this;
    }

    public Map<String, String> getResultAttrQueries() {
        return resultAttrQueries;
    }

    public CrawlTask setResultAttrQueries(Map<String, String> resultAttrQueries) {
        this.resultAttrQueries = resultAttrQueries;
        return this;
    }

    public String[] getResultAttrConditionals() {
        return resultAttrConditionals;
    }

    public CrawlTask setResultAttrConditionals(String[] resultAttrConditionals) {
        this.resultAttrConditionals = resultAttrConditionals;
        return this;
    }

    public Pattern getCrawlFilter() {
        return crawlFilter;
    }

    public CrawlTask setCrawlFilter(Pattern crawlFilter) {
        this.crawlFilter = crawlFilter;
        return this;
    }

    public String getCrawlFilterPattern() {
        return this.crawlFilter.pattern();
    }

    public CrawlTask setCrawlFilterPattern(String crawlPattern) {
        this.crawlFilter = Pattern.compile(crawlPattern, Pattern.DOTALL);
        return this;
    }

    @JsonIgnore
    public boolean shouldVisit(String link) {
        return (this.crawlFilter != null) && this.crawlFilter.matcher(link).matches();
    }

    public CrawlFilterAlgorithm getCrawlFilterAlgorithm() {
        return crawlFilterAlgorithm;
    }

    public CrawlTask setCrawlFilterAlgorithm(CrawlFilterAlgorithm crawlFilterAlgorithm) {
        this.crawlFilterAlgorithm = crawlFilterAlgorithm;
        return this;
    }

    @JsonIgnore
    public CrawlTask addCrawlStepPattern(Integer step, String crawlStepPattern) {
        getCrawlStepPatterns().put(step, Pattern.compile(crawlStepPattern));
        return this;
    }

    public Map<Integer, Pattern> getCrawlStepPatterns() {
        if(this.crawlStepPatterns == null) {
            this.crawlStepPatterns = Maps.newTreeMap();
        }
        return crawlStepPatterns;
    }

    public CrawlTask setCrawlStepPatterns(Map<Integer, Pattern> crawlStepPatterns) {
        this.crawlStepPatterns = Maps.newTreeMap();
        this.crawlStepPatterns.putAll(crawlStepPatterns);
        return this;
    }

    @JsonIgnore
    public CrawlTask mergeConfig(CrawlTask crawlTask) {
        if(this.politeness == null) {
            this.politeness = crawlTask.getPoliteness();
        }
        if(this.crawlFilter == null){
            this.crawlFilter = crawlTask.getCrawlFilter();
        }
        if(StringUtils.isBlank(this.resultQuery)) {
            this.resultQuery = crawlTask.getResultQuery();
        }
        if(this.resultAttrQueries == null) {
            this.resultAttrQueries = crawlTask.getResultAttrQueries();
        }
        if(this.crawlFilterAlgorithm == null) {
            this.crawlFilterAlgorithm = crawlTask.getCrawlFilterAlgorithm();
        }
        if(this.crawlStepPatterns == null) {
            this.crawlStepPatterns = crawlTask.getCrawlStepPatterns();
        }
        if(this.download == null) {
            this.download = crawlTask.getDownload();
        }
        return this;
    }

    @JsonIgnore
    public Integer nextStep() {
        for (Map.Entry<Integer, Pattern> keyPatternEntry : getCrawlStepPatterns().entrySet()) {
            if(keyPatternEntry.getValue().matcher(url).matches()) {
                return keyPatternEntry.getKey();
            }
        }
        return (this.crawlStep + 1);
    }
}