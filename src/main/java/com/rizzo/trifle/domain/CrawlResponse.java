package com.rizzo.trifle.domain;

import java.io.Serializable;

public class CrawlResponse implements Serializable {

    private String response;

    private String processId;

    public String getResponse() {
        return response;
    }

    public CrawlResponse setResponse(String response) {
        this.response = response;
        return this;
    }

    public String getProcessId() {
        return processId;
    }

    public CrawlResponse setProcessId(String processId) {
        this.processId = processId;
        return this;
    }
}
