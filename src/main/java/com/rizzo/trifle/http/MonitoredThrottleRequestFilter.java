package com.rizzo.trifle.http;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.filter.FilterContext;
import com.ning.http.client.filter.FilterException;
import com.ning.http.client.filter.RequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class MonitoredThrottleRequestFilter implements RequestFilter, InitializingBean {

    private final static Logger LOGGER = LoggerFactory.getLogger(MonitoredThrottleRequestFilter.class);

    public static final String GAUGE_METRIC_SUFFIX = ".urlfetcher.throttle.available.permits";

    private Semaphore available;

    private int maxWait;

    @Value("${fetcher.throttle.connections-total}")
    private int maximumConnectionsTotal;

    @Value("${fetcher.throttle.connections-wait-milliseconds}")
    private int maximumWaitConnectionThrottling;

    @Value("${node.name}")
    private String nodeName;

    @Autowired
    private GaugeService gaugeService;

    @Override
    public void afterPropertiesSet() throws Exception {
        int maxConnections = maximumConnectionsTotal;
        this.maxWait = maximumWaitConnectionThrottling;
        available = new Semaphore(maxConnections, true);
    }

    /**
     * {@inheritDoc}
     */
    /* @Override */
    public FilterContext filter(FilterContext ctx) throws FilterException {
        try {
            this.gaugeService.submit(nodeName + GAUGE_METRIC_SUFFIX, available.availablePermits());
            if (!available.tryAcquire(maxWait, TimeUnit.MILLISECONDS)) {
                throw new FilterException(
                        String.format("No slot available for processing Request %s with AsyncHandler %s",
                                ctx.getRequest(), ctx.getAsyncHandler()));
            }
            ;
        } catch (InterruptedException e) {
            throw new FilterException(
                    String.format("Interrupted Request %s with AsyncHandler %s", ctx.getRequest(), ctx.getAsyncHandler()));
        }
        return new FilterContext.FilterContextBuilder(ctx).asyncHandler(new AsyncHandlerWrapper(ctx.getAsyncHandler())).build();
    }



    private class AsyncHandlerWrapper<T> implements AsyncHandler {

        private final AsyncHandler<T> asyncHandler;

        public AsyncHandlerWrapper(AsyncHandler<T> asyncHandler) {
            this.asyncHandler = asyncHandler;
        }

        /**
         * {@inheritDoc}
         */
        /* @Override */
        public void onThrowable(Throwable t) {
            try {
                asyncHandler.onThrowable(t);
            } finally {
                available.release();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Current Throttling Status after onThrowable {}", available.availablePermits());
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        /* @Override */
        public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
            return asyncHandler.onBodyPartReceived(bodyPart);
        }

        /**
         * {@inheritDoc}
         */
        /* @Override */
        public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
            return asyncHandler.onStatusReceived(responseStatus);
        }

        /**
         * {@inheritDoc}
         */
        /* @Override */
        public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
            return asyncHandler.onHeadersReceived(headers);
        }

        /**
         * {@inheritDoc}
         */
        /* @Override */
        public T onCompleted() throws Exception {
            available.release();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Current Throttling Status {}", available.availablePermits());
            }
            return asyncHandler.onCompleted();
        }
    }
}
