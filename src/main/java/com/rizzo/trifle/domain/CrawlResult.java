package com.rizzo.trifle.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import org.apache.commons.lang.RandomStringUtils;
import org.mvel2.MVEL;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Document(indexName = "trifle", type = "crawl", shards = 1, replicas = 0, refreshInterval = "-1")
public class CrawlResult {

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Id
    private String id = RandomStringUtils.randomAlphanumeric(20);

    @JsonProperty("process-id")
    private String processId;

    private String timestamp;

    private String url;

    @Field(type = FieldType.Nested)
    private Map<String, String> attributes;

    public String getId() {
        return id;
    }

    public CrawlResult setId(String id) {
        this.id = id;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public CrawlResult setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getProcessId() {
        return processId;
    }

    public CrawlResult setProcessId(String processId) {
        this.processId = processId;
        return this;
    }

    public Map<String, String> getAttributes() {
        if(this.attributes == null) {
            this.attributes = Maps.newHashMap();
        }
        return attributes;
    }

    public CrawlResult setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
        return this;
    }

    public boolean shouldSave(String[] attributeConditionals) {
        Boolean shouldSave = true;
        if(attributes != null) {
            if (attributeConditionals != null) {
                for (String attributeConditional : attributeConditionals) {
                    if(!(boolean) MVEL.eval(attributeConditional, attributes)) {
                        shouldSave = false;
                    }
                }
            }
        }
        return shouldSave;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public CrawlResult setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public CrawlResult stamp() {
        this.timestamp = TIMESTAMP_FORMAT.format(new Date());
        return this;
    }
}
