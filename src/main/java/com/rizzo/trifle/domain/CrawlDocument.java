package com.rizzo.trifle.domain;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Element;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CrawlDocument {

    public static final String LINK_ATTR_QUERY = "abs:href";

    private String domain;

    private List<Element> links;

    private List<Element> results;

    public List<Element> getLinks() {
        return links;
    }

    public CrawlDocument setLinks(List<Element> links) {
        this.links = links;
        return this;
    }

    public List<Element> getResults() {
        return results;
    }

    public CrawlDocument setResults(List<Element> results) {
        this.results = results;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public CrawlDocument setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public String[] getLinksAsStringArray() {
        List<String> mediaLinks = Lists.newArrayList();
        for (Element link : links) {
            final String absUrl = link.absUrl(LINK_ATTR_QUERY);
            if (StringUtils.isNotBlank(absUrl)) {
                mediaLinks.add(absUrl);
            }
            final String action = link.absUrl("action");
            if (StringUtils.isNotBlank(action)) {
                mediaLinks.add(action);
            }
        }
        return mediaLinks.toArray(new String[mediaLinks.size()]);
    }

    public List<DownloadTask> getDownloadResults(CrawlTask crawlTask) {
        List<DownloadTask> downloadTasks = Lists.newArrayList();
        if (results != null) {
            for (Element result : results) {
                Map<String, String> queryMap = Maps.newHashMap();
                if (crawlTask.getResultAttrQueries() != null) {
                    for (Map.Entry<String, String> keyValueEntry : crawlTask.getResultAttrQueries().entrySet()) {
                        final String attrValue = result.attr(keyValueEntry.getValue());
                        if (StringUtils.isNotBlank(attrValue)) {
                            queryMap.put(keyValueEntry.getKey(), attrValue);
                        }
                    }
                }
                final DownloadTask downloadTask = new DownloadTask()
                        .setUrl(result.absUrl("src"));

                downloadTask.getAttributes().putAll(queryMap);
                if (crawlTask.getResultAttrConditionals() != null) {
                    downloadTask.setAttributeConditionals(Sets.newHashSet(Arrays.asList(crawlTask.getResultAttrConditionals())));
                }
                if (downloadTask.shouldDownload()) {
                    downloadTasks.add(downloadTask);
                }
            }
        }
        return downloadTasks;
    }

    public List<CrawlResult> getCrawlResults(CrawlTask crawlTask) {
        List<CrawlResult> crawlResults = Lists.newArrayList();
        if (results != null) {
            for (Element result : results) {
                Map<String, String> queryMap = Maps.newHashMap();
                if (crawlTask.getResultAttrQueries() != null) {
                    for (Map.Entry<String, String> keyValueEntry : crawlTask.getResultAttrQueries().entrySet()) {
                        if("text()".equals(keyValueEntry.getValue())) {
                            final String text = result.text();
                            if (StringUtils.isNotBlank(text)) {
                                queryMap.put(keyValueEntry.getKey(), text);
                            }
                        } else {
                            final String attrValue = result.attr(keyValueEntry.getValue());
                            if (StringUtils.isNotBlank(attrValue)) {
                                queryMap.put(keyValueEntry.getKey(), attrValue);
                            }
                        }
                    }
                }
                final CrawlResult crawlResult = new CrawlResult();
                crawlResult.setProcessId(crawlTask.getCrawlProcessId())
                        .setUrl(crawlTask.getUrl()).stamp()
                        .getAttributes().putAll(queryMap);

                if (crawlResult.shouldSave(crawlTask.getResultAttrConditionals())) {
                    crawlResults.add(crawlResult);
                }
            }
        }
        return crawlResults;
    }
}
