package com.rizzo.trifle.domain;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.mvel2.MVEL;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class DownloadTask implements Serializable {

    private String url;

    private Map<String, String> attributes;

    private String urlConditional;

    private Set<String> attributeConditionals;

    public String getUrl() {
        return url;
    }

    public DownloadTask setUrl(String url) {
        this.url = url;
        return this;
    }

    public Map<String, String> getAttributes() {
        if(this.attributes == null) {
            this.attributes = Maps.newHashMap();
        }
        return attributes;
    }

    public DownloadTask setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
        return this;
    }

    public String getUrlConditional() {
        return urlConditional;
    }

    public DownloadTask setUrlConditional(String urlConditional) {
        this.urlConditional = urlConditional;
        return this;
    }

    public Set<String> getAttributeConditionals() {
        if(attributeConditionals == null) {
            this.attributeConditionals = Sets.newHashSet();
        }
        return attributeConditionals;
    }

    public DownloadTask setAttributeConditionals(Set<String> attributeConditionals) {
        this.attributeConditionals = attributeConditionals;
        return this;
    }

    public boolean shouldDownload() {
        Boolean shouldDownload = true;
        if(StringUtils.isNotBlank(urlConditional)) {
            shouldDownload = (Boolean) MVEL.eval("'" + url + "' " + urlConditional);
        }
        if(attributes != null) {
            if (attributeConditionals != null) {
                for (String attributeConditional : attributeConditionals) {
                    if(!(boolean) MVEL.eval(attributeConditional, attributes)) {
                        shouldDownload = false;
                    }
                }
            }
        }
        return shouldDownload;
    }
}
