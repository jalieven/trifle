package com.rizzo.trifle.akka.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.collect.Iterables;
import com.ning.http.client.Response;
import com.rizzo.trifle.domain.DownloadResult;
import com.rizzo.trifle.domain.DownloadTask;
import com.rizzo.trifle.elasticsearch.DownloadResultRepository;
import com.rizzo.trifle.http.DownloadHttpReponseHandler;
import com.rizzo.trifle.http.UrlFetcher;
import com.rizzo.trifle.image.ImageCalculator;
import net.logstash.logback.encoder.org.apache.commons.io.FileUtils;
import net.logstash.logback.encoder.org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
@Scope(value = "prototype")
public class DownloadActor extends CrawlActor implements DownloadHttpReponseHandler {

    private static final Logger logger = LoggerFactory.getLogger(DownloadActor.class);

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), DownloadActor.class);

    public static final String CONTENT_TYPE = "Content-Type";

    public static final String CONTENT_LENGTH = "Content-Length";

    public static final String ACTOR_NAME = "Download-Actor";

    protected ActorRef parent;

    protected ActorSystem actorSystem;

    @Autowired
    protected UrlFetcher urlFetcher;

    @Autowired
    private DownloadResultRepository downloadResultRepository;

    @Autowired
    private ImageCalculator imageCalculator;

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    @Value("${downloads.folder-location}")
    private Resource downloads;

    @Value("${spring.redis.keys.duplicate-key}")
    private String redisDuplicatesKey;

    @Value("${spring.redis.keys.duplicate-key}")
    private String duplicateDigestAlgoritm;

    @Override
    protected String getActorName() {
        return ACTOR_NAME;
    }

    @Override
    public void preStart() throws Exception {
        this.parent = getContext().parent();
        this.actorSystem = getContext().system();
        log.debug("Starting up " + getSelf().path().toSerializationFormat());
        super.preStart();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof DownloadTask) {
            logger.debug("Received DownloadTask for " + self().path().toSerializationFormat());
            super.incrementMessageCounter(DownloadTask.class);
            DownloadTask downloadTask = (DownloadTask) message;
            this.urlFetcher.download(downloadTask.getUrl(), downloadTask.getAttributes(), this);
        } else {
            log.error("Unable to handle message {}", message);
        }
    }

    @Override
    public void postStop() throws Exception {
        log.debug("Stopped " + getSelf().path().toSerializationFormat());
        super.postStop();
    }

    @Override
    public Integer onCompleted(String url, Response response, Map<String, String> attributes, String baseUrl, String extension) {
        FileOutputStream output = null;
        InputStream input = null;
        try {
            final File file;
            final DownloadResult downloadResult = new DownloadResult().setId(url)
                    .setStatusCode(response.getStatusCode())
                    .setBaseUrl(baseUrl)
                    .setRedirected(response.isRedirected())
                    .stamp();
            downloadResult.getAttributes().putAll(attributes);

            if (response.hasResponseBody()) {
                // add response headers
                for (Map.Entry<String, List<String>> keyValueEntry : response.getHeaders().entrySet()) {
                    if(CONTENT_TYPE.toLowerCase().equals(keyValueEntry.getKey().toLowerCase())) {
                        downloadResult.setContentType(Iterables.getFirst(keyValueEntry.getValue(), "n/a"));
                    } else if(CONTENT_LENGTH.toLowerCase().equals(keyValueEntry.getKey().toLowerCase())) {
                        downloadResult.setContentLength(Long.parseLong(Iterables.getFirst(keyValueEntry.getValue(), "0")));
                    } else {
                        downloadResult.getHeaders().put(keyValueEntry.getKey().toLowerCase(), keyValueEntry.getValue());
                    }
                }
                // save the file
                file = new File(this.downloads.getFile(), RandomStringUtils.randomAlphanumeric(20) + "." + extension);
                output = new FileOutputStream(file);
                input = response.getResponseBodyAsStream();
                IOUtils.copy(input, output);
                downloadResult.setFileLocation(file.getAbsolutePath());
                // calculate digest and dimensions
                final String digest = imageCalculator.calculateDigest(downloadResult);
                if (digest != null) {
                    // detect duplicate
                    if(stringRedisTemplate.opsForSet().isMember(this.redisDuplicatesKey, digest)) {
                        logger.debug("Image already downloaded: " + downloadResult.toString());
                        FileUtils.deleteQuietly(file);
                    } else {
                        logger.debug("Downloading: " + downloadResult.toString());
                        this.stringRedisTemplate.opsForSet().add(this.redisDuplicatesKey, digest);
                        this.imageCalculator.calculateDimension(downloadResult);
                        this.downloadResultRepository.save(downloadResult);
                    }
                } else {
                    this.imageCalculator.calculateDimension(downloadResult);
                    this.downloadResultRepository.save(downloadResult);
                }
            }
        } catch (Exception e) {
            logger.error("Exception onCompleted!", e);
        } finally {
            if(input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.error("Could not close InputStream, but ignoring this...", e);
                }
            }
            if(output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    logger.error("Could not close InputStream, but ignoring this...", e);
                }
            }
        }
        return response.getStatusCode();
    }

    @Override
    public void onThrowable(Throwable t) {

    }
}
