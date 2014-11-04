package com.rizzo.trifle.akka.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rizzo.trifle.domain.CrawlDocument;
import com.rizzo.trifle.domain.CrawlResult;
import com.rizzo.trifle.domain.CrawlTask;
import com.rizzo.trifle.domain.DownloadTask;
import com.rizzo.trifle.elasticsearch.CrawlResultRepository;
import com.rizzo.trifle.http.CrawlHttpReponseHandler;
import com.rizzo.trifle.http.UrlFetcher;
import com.rizzo.trifle.parse.JSoupParser;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Scope(value = "prototype")
public class FetchActor extends CrawlActor implements CrawlHttpReponseHandler<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchActor.class);

    public static final String ACTOR_NAME = "Fetch-Actor";

    protected static Random random = new Random();

    @Value("${spring.redis.keys.weird-url-key}")
    private String redisWeirdUrlKey;

    @Value("${spring.redis.keys.error-key}")
    private String redisErrorKey;

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    @Autowired
    protected UrlFetcher urlFetcher;

    @Autowired
    protected JSoupParser jSoupParser;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected CrawlResultRepository crawlResultRepository;

    @Override
    protected String getActorName() {
        return ACTOR_NAME;
    }

    protected ActorRef parent;

    protected ActorSystem actorSystem;


    @Override
    public void preStart() throws Exception {
        this.parent = getContext().parent();
        this.actorSystem = getContext().system();
        LOGGER.debug("Starting up " + getSelf().path().toSerializationFormat());
        super.preStart();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof CrawlTask) {
            LOGGER.debug("Received CrawlTask for " + self().path().toSerializationFormat());
            super.incrementMessageCounter(CrawlTask.class);
            CrawlTask crawlTask = (CrawlTask) message;
            this.urlFetcher.getAsString(crawlTask, this);
        } else {
            LOGGER.error("Unable to handle message {}", message);
        }
    }

    @Override
    public void postStop() throws Exception {
        LOGGER.debug("Stopped " + getSelf().path().toSerializationFormat());
        super.postStop();
    }

    @Override
    public String onCompleted(CrawlTask crawlTask, String html, String baseUri) {
        try {
            parseHtmlAndFeedback(crawlTask, html, baseUri);
        } catch (IOException e) {
            LOGGER.error("Fail while serializing results...", e);
        }
        return html;
    }

    private void parseHtmlAndFeedback(CrawlTask crawlTask, String html, String baseUri) throws IOException {
        final CrawlDocument crawlDocument = jSoupParser.getCrawlDocument(crawlTask.getResultQuery(), html, baseUri);
        if (crawlTask.getDownload()) {
            final List<DownloadTask> downloadTasks = crawlDocument.getDownloadResults(crawlTask);
            if (downloadTasks.size() > 0) {
                for (DownloadTask downloadTask : downloadTasks) {
                    if(crawlTask.getPoliteness() != null) {
                        this.actorSystem.scheduler().scheduleOnce(
                                new FiniteDuration(random.nextInt(crawlTask.getPoliteness()), TimeUnit.MILLISECONDS),
                                this.parent, downloadTask, this.actorSystem.dispatcher(), null);
                    } else {
                        this.parent.tell(downloadTask, null);
                    }
                }
            }
        }
        final List<CrawlResult> crawlResults = crawlDocument.getCrawlResults(crawlTask);
        if (!crawlResults.isEmpty()) {
            this.crawlResultRepository.save(crawlResults);
        }
        String[] linksAsStringArray = crawlDocument.getLinksAsStringArray();
        if(crawlTask.getCrawlFilterAlgorithm() != null) {
            linksAsStringArray = crawlTask.getCrawlFilterAlgorithm().filterLinks(linksAsStringArray);
        }
        for (String link : linksAsStringArray) {
            if (StringUtils.isNotBlank(link) && crawlTask.shouldVisit(link)) {
                final CrawlTask feedbackCrawlTask = new CrawlTask();
                feedbackCrawlTask.setUrl(link);
                feedbackCrawlTask.setPriority(50);
                feedbackCrawlTask.setCrawlProcessId(crawlTask.getCrawlProcessId());
                feedbackCrawlTask.setCrawlStep(crawlTask.setUrl(link).nextStep());
                if(crawlTask.getPoliteness() != null) {
                    this.actorSystem.scheduler().scheduleOnce(
                            new FiniteDuration(random.nextInt(crawlTask.getPoliteness()), TimeUnit.MILLISECONDS),
                            this.parent, feedbackCrawlTask, this.actorSystem.dispatcher(), null);
                } else {
                    this.parent.tell(feedbackCrawlTask, null);
                }
            }
        }
    }

    @Override
    public void onThrowable(Throwable t) {
        try {
            stringRedisTemplate.opsForList().leftPush(redisErrorKey, "Fetch: " + t.getMessage());
        } catch (Exception e) {
            LOGGER.error("Fail while adding error to Redis...", e);
        }
    }

    @Override
    public void onWeirdUrl(String url) {
        try {
            stringRedisTemplate.opsForList().leftPush(redisWeirdUrlKey, url);
        } catch (Exception e) {
            LOGGER.error("Fail while adding weird url to Redis...", e);
        }
    }

}