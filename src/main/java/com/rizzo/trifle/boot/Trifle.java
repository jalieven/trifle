package com.rizzo.trifle.boot;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.base.CaseFormat;
import com.rizzo.trifle.akka.actor.FetchSupervisor;
import com.rizzo.trifle.domain.CrawlProcess;
import com.rizzo.trifle.domain.CrawlResponse;
import com.rizzo.trifle.spring.SpringExtension;
import com.rizzo.trifle.web.TrifleController;
import com.rizzo.trifle.web.TrifleRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import javax.annotation.PreDestroy;

@EnableAutoConfiguration
@Configuration
@ComponentScan(basePackages = { "com.rizzo.trifle" })
@EnableElasticsearchRepositories(basePackages = {"com.rizzo.trifle.elasticsearch"})
@RestController
public class Trifle extends SpringBootServletInitializer implements CommandLineRunner, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(Trifle.class);

    private ApplicationContext applicationContext;
    private ActorSystem actorSystem;
    private ActorRef supervisor;

    @Value("${server.port}")
    private int nodePort;

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Trifle.class);
    }

    @Override
    public void run(String... args) throws Exception {
        LOGGER.info("Starting Trifle Actor System...");
        LOGGER.info("[[[ Seed entry-endpoint is located @ http://localhost:" + nodePort + "/seed ]]]");

        this.actorSystem = applicationContext.getBean(ActorSystem.class);

        final LoggingAdapter log = Logging.getLogger(this.actorSystem, Trifle.class);

        log.info("Starting up Trifle System");

        SpringExtension ext = applicationContext.getBean(SpringExtension.class);

        // Use the Spring Extension to create props for a named actor bean
        final String actorBeanName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, FetchSupervisor.ACTOR_NAME);
        this.supervisor = this.actorSystem.actorOf(
                ext.props(actorBeanName).withMailbox("akka.priority-mailbox"), FetchSupervisor.ACTOR_NAME);

    }

    public static void main(String[] args) {
        SpringApplication.run(new Object[]{Trifle.class}, args);
    }

//    @Bean
//    public SimpleMappingExceptionResolver simpleMappingExceptionResolver() {
//        final SimpleMappingExceptionResolver simpleMappingExceptionResolver = new SimpleMappingExceptionResolver();
//        simpleMappingExceptionResolver.setDefaultErrorView("exception");
//        simpleMappingExceptionResolver.setWarnLogCategory("warn");
//        return simpleMappingExceptionResolver;
//    }

    @Bean
    public TrifleRestController trifleRestController() {
        return new TrifleRestController(this.supervisor);
    }

    @Bean
    public TrifleController trifleController() {
        return new TrifleController();
    }

    @PreDestroy
    public void shutdownActorSystem() throws InterruptedException {
        this.supervisor.tell(PoisonPill.getInstance(), null);
        while (!this.supervisor.isTerminated()) {
            Thread.sleep(500);
        }
        LOGGER.info("Trifle Actor System is going down!");
        this.actorSystem.shutdown();
        this.actorSystem.awaitTermination();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
