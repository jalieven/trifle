package com.rizzo.trifle.http;

import com.rizzo.trifle.domain.CrawlTask;

import java.io.IOException;
import java.util.Map;

public interface UrlFetcher {

    void getAsString(final CrawlTask crawlTask, final CrawlHttpReponseHandler<String> crawlHttpReponseHandler) throws IOException;

    void download(final String url, final Map<String, String> attributes, final DownloadHttpReponseHandler downloadHttpReponseHandler) throws IOException;

}
