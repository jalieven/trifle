package com.rizzo.trifle.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Document(indexName = "trifle", type = "download", shards = 1, replicas = 0, refreshInterval = "-1")
public class DownloadResult {

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Id
    private String id;

    @JsonProperty("file-location")
    private String fileLocation;

    @JsonProperty("status-code")
    private Integer statusCode;

    private String timestamp;

    @JsonProperty("base-url")
    private String baseUrl;

    private boolean redirected;

    @JsonProperty("content-type")
    private String contentType;

    @JsonProperty("content-length")
    private Long contentLength;

    @Field(type = FieldType.Nested)
    private Map<String, String> attributes;

    @Field(type = FieldType.Nested)
    private Map<String, Long> metrics;

    @Field(type = FieldType.Nested)
    private Map<String, List<String>> headers;

    public String getId() {
        return id;
    }

    public DownloadResult setId(String id) {
        this.id = id;
        return this;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public DownloadResult setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
        return this;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public DownloadResult setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public DownloadResult setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public DownloadResult stamp() {
        this.timestamp = TIMESTAMP_FORMAT.format(new Date());
        return this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public DownloadResult setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public boolean isRedirected() {
        return redirected;
    }

    public DownloadResult setRedirected(boolean redirected) {
        this.redirected = redirected;
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public DownloadResult setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public Long getContentLength() {
        return contentLength;
    }

    public DownloadResult setContentLength(Long contentLength) {
        this.contentLength = contentLength;
        return this;
    }

    public Map<String, String> getAttributes() {
        if(this.attributes == null) {
            this.attributes = Maps.newHashMap();
        }
        return attributes;
    }

    public DownloadResult setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
        return this;
    }

    public Map<String, Long> getMetrics() {
        if(this.metrics == null) {
            this.metrics = Maps.newHashMap();
        }
        return metrics;
    }

    public DownloadResult setMetrics(Map<String, Long> metrics) {
        this.metrics = metrics;
        return this;
    }

    public Map<String, List<String>> getHeaders() {
        if(this.headers == null) {
            this.headers = Maps.newHashMap();
        }
        return headers;
    }

    public DownloadResult setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("fileLocation", fileLocation)
                .append("statusCode", statusCode)
                .append("timestamp", timestamp)
                .append("baseUrl", baseUrl)
                .append("redirected", redirected)
                .append("contentType", contentType)
                .append("contentLength", contentLength)
                .append("attributes", attributes)
                .append("metrics", metrics)
                .append("headers", headers)
                .toString();
    }
}
