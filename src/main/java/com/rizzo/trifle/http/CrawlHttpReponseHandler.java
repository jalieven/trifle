package com.rizzo.trifle.http;

import com.rizzo.trifle.domain.CrawlTask;

public interface CrawlHttpReponseHandler<T> {

    T onCompleted(CrawlTask crawlTask, T response, String baseUri);

    void onThrowable(Throwable t);

    void onWeirdUrl(String url);
}
