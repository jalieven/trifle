package com.rizzo.trifle.akka;

import akka.actor.ActorSystem;
import akka.dispatch.PriorityGenerator;
import akka.dispatch.UnboundedPriorityMailbox;
import com.rizzo.trifle.domain.CrawlTask;
import com.typesafe.config.Config;

public class PriorityMailbox extends UnboundedPriorityMailbox {

    public PriorityMailbox(ActorSystem.Settings settings, Config config) {

        // Create a new PriorityGenerator, lower priority means more important
        super(new PriorityGenerator() {

            @Override
            public int gen(Object message) {
                if (message instanceof CrawlTask) {
                    return ((CrawlTask) message).getPriority();
                } else {
                    // default
                    return 100;
                }
            }
        });

    }
}