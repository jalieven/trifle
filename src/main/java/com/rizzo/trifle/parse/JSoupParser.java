package com.rizzo.trifle.parse;

import com.rizzo.trifle.aop.LogPerformance;
import com.rizzo.trifle.domain.CrawlDocument;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

@Component
public class JSoupParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSoupParser.class);

    public static final String FORM_QUERY = "form";
    public static final String LINKS_QUERY = "a";

    @LogPerformance
    public CrawlDocument getCrawlDocument(String resultQuery, String html, String baseUri) throws MalformedURLException {
        Document doc = Jsoup.parse(html, baseUri);
        LOGGER.info("Parsed document: " + doc.title());
        URL url = new URL(baseUri);
        Elements select = doc.select(LINKS_QUERY);
        if(select.isEmpty()) {
            select = doc.select(FORM_QUERY);
        }
        final CrawlDocument crawlDocument = new CrawlDocument().setLinks(select);
        if(StringUtils.isNotBlank(resultQuery)) {
            crawlDocument.setResults(doc.select(resultQuery));
        }
        return crawlDocument.setDomain(url.getHost());
    }

}
