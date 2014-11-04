package com.rizzo.trifle.akka.actor;

import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.Routee;
import akka.routing.Router;
import akka.routing.SmallestMailboxRoutingLogic;
import com.google.common.base.CaseFormat;
import com.rizzo.trifle.domain.CrawlProcess;
import com.rizzo.trifle.domain.CrawlTask;
import com.rizzo.trifle.domain.DownloadTask;
import com.rizzo.trifle.redis.RedisCache;
import com.rizzo.trifle.spring.SpringExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class FetchSupervisor extends CrawlActor {

    private static final Logger logger = LoggerFactory.getLogger(FetchSupervisor.class);

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), FetchSupervisor.class);

    public static final String ACTOR_NAME = "Fetch-Supervisor";

    @Autowired
    private SpringExtension springExtension;

    @Autowired
    private RedisCache redisCache;

    @Value("${actors.fetch-actor-count}")
    private int numberOfFetchActors;

    @Value("${actors.download-actor-count}")
    private int numberOfDownloadActors;

    private Router fetchRouter;

    private Router downloadRouter;

    @Override
    protected String getActorName() {
        return ACTOR_NAME;
    }

    @Override
    public void preStart() throws Exception {
        log.debug("Starting up " + getSelf().path().toSerializationFormat());

        // start fetchActors
        List<Routee> fetchRoutees = new ArrayList<Routee>();
        for (int i = 0; i < numberOfFetchActors; i++) {
            final String actorBeanName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, FetchActor.ACTOR_NAME);
            ActorRef actor = getContext().actorOf(springExtension.props(
                    actorBeanName), FetchActor.ACTOR_NAME + "-" + i);
            getContext().watch(actor);
            fetchRoutees.add(new ActorRefRoutee(actor));
        }
        fetchRouter = new Router(new SmallestMailboxRoutingLogic(), fetchRoutees);

        // start downloadActors
        List<Routee> downloadRoutees = new ArrayList<Routee>();
        for (int i = 0; i < numberOfDownloadActors; i++) {
            final String actorBeanName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, DownloadActor.ACTOR_NAME);
            ActorRef actor = getContext().actorOf(springExtension.props(
                    actorBeanName), DownloadActor.ACTOR_NAME + "-" + i);
            getContext().watch(actor);
            downloadRoutees.add(new ActorRefRoutee(actor));
        }
        downloadRouter = new Router(new SmallestMailboxRoutingLogic(), downloadRoutees);

        super.preStart();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof CrawlProcess) {
            super.incrementMessageCounter(CrawlProcess.class);
            CrawlProcess crawlProcess = (CrawlProcess) message;
            redisCache.cachePut(crawlProcess);
            fetchRouter.route(crawlProcess.getFirstCrawlTask(), getSender());
        } else if(message instanceof CrawlTask) {
            super.incrementMessageCounter(CrawlTask.class);
            CrawlTask crawlTask = (CrawlTask) message;
            final Integer crawlStep = crawlTask.getCrawlStep();
            final CrawlProcess cachedCrawlProcess = redisCache.cacheGet(crawlTask.getCrawlProcessId());
            final CrawlTask cacheCrawlTask = cachedCrawlProcess.getCrawlTask(crawlStep);
            if (cacheCrawlTask != null) {
                fetchRouter.route(crawlTask.mergeConfig(cacheCrawlTask), getSender());
            }
        } else if(message instanceof DownloadTask) {
            super.incrementMessageCounter(CrawlTask.class);
            DownloadTask downloadTask = (DownloadTask) message;
            downloadRouter.route(downloadTask, getSender());
        } else if (message instanceof Terminated) {
            super.incrementMessageCounter(Terminated.class);
            final Terminated terminated = (Terminated) message;
            if(terminated.getActor().path().toSerializationFormat().toLowerCase().contains("fetchactor")) {
                fetchRouter = fetchRouter.removeRoutee(terminated.actor());
                logger.debug("Received Terminated message: " + terminated.actor().path().toSerializationFormat());
                final String actorBeanName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, FetchActor.ACTOR_NAME);
                ActorRef actor = getContext().actorOf(springExtension.props(
                        actorBeanName));
                getContext().watch(actor);
                fetchRouter = fetchRouter.addRoutee(new ActorRefRoutee(actor));
            }
            if(terminated.getActor().path().toSerializationFormat().toLowerCase().contains("downloadactor")) {
                downloadRouter = downloadRouter.removeRoutee(terminated.actor());
                logger.debug("Received Terminated message: " + terminated.actor().path().toSerializationFormat());
                final String actorBeanName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, DownloadActor.ACTOR_NAME);
                ActorRef actor = getContext().actorOf(springExtension.props(
                        actorBeanName));
                getContext().watch(actor);
                downloadRouter = downloadRouter.addRoutee(new ActorRefRoutee(actor));
            }
        } else {
            log.error("Unable to handle message {}", message);
        }
    }

    @Override
    public void postStop() throws Exception {
        log.debug("Stopped " + getSelf().path().toSerializationFormat());
        super.postStop();
    }
}