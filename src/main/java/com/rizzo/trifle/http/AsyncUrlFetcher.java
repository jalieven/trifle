package com.rizzo.trifle.http;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import com.rizzo.trifle.aop.LogPerformance;
import com.rizzo.trifle.domain.CrawlTask;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class AsyncUrlFetcher implements InitializingBean, UrlFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncUrlFetcher.class);

    @Value("${fetcher.maximum.request-timeout-milliseconds}")
    private int requestTimeoutInMs;

    @Value("${fetcher.maximum.connections-per-host}")
    private int maximumConnectionsPerHost;

    @Value("${fetcher.maximum.redirects}")
    private int maximumNumberOfRedirects;

    @Value("${fetcher.maximum.thread-pool-size}")
    private int threadPoolMaximumSize;

    @Value("${fetcher.maximum.thread-pool-keepalive-seconds}")
    private long threadPoolKeepAliveSeconds;

    @Value("${fetcher.follow-redirects}")
    private boolean followRedirects = true;

    @Value("${fetcher.user-agent}")
    private String userAgent;

    @Autowired
    private MonitoredThrottleRequestFilter monitoredThrottleRequestFilter;

    private AsyncHttpClient asyncHttpClient;

    @Override
    public void afterPropertiesSet() throws Exception {
        final AsyncHttpClientConfig.Builder clientConfigBuilder = new AsyncHttpClientConfig.Builder();
        AsyncHttpClientConfig.Builder httpClientConfigBuilder = clientConfigBuilder
                .setAllowPoolingConnection(true)
                .setRequestTimeoutInMs(requestTimeoutInMs)
                .setMaximumConnectionsPerHost(maximumConnectionsPerHost)
                .setExecutorService(new ThreadPoolExecutor(0, threadPoolMaximumSize,
                        threadPoolKeepAliveSeconds, TimeUnit.SECONDS,
                        new SynchronousQueue<Runnable>()))
                .setUserAgent(userAgent)
                .setFollowRedirects(followRedirects)
                .setMaximumNumberOfRedirects(maximumNumberOfRedirects)
                .addRequestFilter(monitoredThrottleRequestFilter);
        this.asyncHttpClient = new AsyncHttpClient(httpClientConfigBuilder.build());
    }

    @LogPerformance
    public void getAsString(final CrawlTask crawlTask, final CrawlHttpReponseHandler<String> crawlHttpReponseHandler) throws IOException {
        LOGGER.info("Fetching url: " + crawlTask.getUrl());
        try {
            this.asyncHttpClient.prepareGet(crawlTask.getUrl()).execute(
                    new AsyncCompletionHandler<String>() {

                        @Override
                        public String onCompleted(Response response) throws Exception {
                            return crawlHttpReponseHandler.onCompleted(crawlTask, response.getResponseBody(), getBaseUrl(crawlTask.getUrl()));
                        }

                        @Override
                        public void onThrowable(Throwable t) {
                            crawlHttpReponseHandler.onThrowable(t);
                        }

                    });
        } catch (IllegalArgumentException eae) {
            crawlHttpReponseHandler.onWeirdUrl(crawlTask.getUrl());
        } catch (Exception e) {
            LOGGER.error("Fetch failed!", e);
        }
    }

    @LogPerformance
    public void download(final String url, final Map<String, String> attributes, final DownloadHttpReponseHandler downloadHttpReponseHandler) throws IOException {
        LOGGER.info("Downloading url: " + url);
        try {
            this.asyncHttpClient.prepareGet(url).execute(
                    new AsyncCompletionHandler<Integer>() {

                        @Override
                        public Integer onCompleted(Response response) throws Exception {
                            return downloadHttpReponseHandler.onCompleted(url, response, attributes, getBaseUrl(url), getExtension(url));
                        }

                        @Override
                        public void onThrowable(Throwable t) {
                            downloadHttpReponseHandler.onThrowable(t);
                        }

                    });
        } catch (Exception e) {
            LOGGER.error("Download failed!", e);
        }
    }

    public String getBaseUrl(String link) throws MalformedURLException {
        URL url = new URL(link);
        return url.getProtocol() + "://" + url.getHost();
    }

    public String getExtension(String link) throws MalformedURLException {
        URL url = new URL(link);
        return FilenameUtils.getExtension(url.getPath());
    }

}
