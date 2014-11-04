package com.rizzo.trifle.akka.actor;

import akka.actor.UntypedActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.CounterService;

public abstract class CrawlActor extends UntypedActor {

    protected static final String COUNTER_ACTOR_ACTIVE_SUFFIX = ".active";
    protected static final String ACTOR = ".actor.";

    protected abstract String getActorName();

    @Value("${node.name}")
    private String nodeName;

    @Autowired
    protected CounterService counterService;

    @Override
    public void preStart() throws Exception {
        counterService.increment(nodeName + "." + getActorName().toLowerCase() + COUNTER_ACTOR_ACTIVE_SUFFIX);
        super.preStart();
    }

    @Override
    public void postStop() throws Exception {
        counterService.decrement(nodeName + "." + getActorName().toLowerCase() + COUNTER_ACTOR_ACTIVE_SUFFIX);
        super.postStop();
    }

    protected String getActiveCounterKey() {
        return nodeName + ACTOR + getActorName().toLowerCase() + COUNTER_ACTOR_ACTIVE_SUFFIX;
    }

    protected void incrementMessageCounter(Class clazz) {
        counterService.increment(getMessageCounterKey(clazz));
    }

    private String getMessageCounterKey(Class clazz) {
        return nodeName + ACTOR + getActorName().toLowerCase() + ".messages.received." + clazz.getName().toLowerCase();
    }

}
