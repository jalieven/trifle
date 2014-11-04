package com.rizzo.trifle.integration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.rizzo.trifle.boot.Trifle;
import com.rizzo.trifle.domain.CrawlProcess;
import com.rizzo.trifle.domain.CrawlResponse;
import com.rizzo.trifle.domain.CrawlTask;
import com.rizzo.trifle.filter.LevenshteinDistanceCrawlFilterAlgorithm;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(name = "testTrifle", classes = Trifle.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0", "management.port=0"})
public class SeedSubmitTest {

    @Value("${local.server.port}")
    private int nodePort;

    private RestTemplate restTemplate = new TestRestTemplate();

    @Test
    public void submitSeed() throws InterruptedException, IOException {

        String seedEntryPoint = "http://localhost:" + nodePort + "/seed";
        System.out.print(">>>>>>>>>>>> Test entry endpoint is located: " + seedEntryPoint + " <<<<<<<<<<<<<<<");
        Thread.sleep(1000);

        // launch a seed
        final CrawlProcess crawlProcess = new CrawlProcess();
        final ArrayList<CrawlTask> crawlSteps = Lists.newArrayList(
                new CrawlTask().setCrawlStep(0).setUrl("http://www.standaard.be").setPriority(100).setPoliteness(300000)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.standaard\\.be(/.*)?$")
                        .setResultQuery("img[src$=jpg],img[src$=gif]").setResultAttrQueries(ImmutableMap.of("src", "abs:src", "width", "width", "height", "height"))
                ,
                new CrawlTask().setCrawlStep(1).setPriority(50)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.standaard\\.be(/.*)?$")
                        .setResultQuery("img[src$=jpg],img[src$=gif]").setResultAttrQueries(ImmutableMap.of("src", "abs:src", "width", "width", "height", "height"))
        );
        crawlProcess.setCrawlSteps(crawlSteps);
        final ResponseEntity<CrawlResponse> seedEntity =
                restTemplate.exchange(seedEntryPoint, HttpMethod.PUT, new HttpEntity<>(crawlProcess), CrawlResponse.class);
        assertEquals("Seed swallowed!", seedEntity.getBody().getResponse());
        assertEquals(HttpStatus.OK, seedEntity.getStatusCode());


        Thread.sleep(1000000);
        // maybe assert there are some images somewhere?
    }

    @Test
    public void submitSeedEGM() throws InterruptedException, IOException {
        String seedEntryPoint = "http://localhost:" + nodePort + "/seed";
        System.out.print(">>>>>>>>>>>> Test entry endpoint is located: " + seedEntryPoint + " <<<<<<<<<<<<<<<");
        Thread.sleep(1000);

        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return !HttpStatus.OK.equals(response.getStatusCode());
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                fail(response.getStatusText());
            }
        });

        final CrawlProcess crawlProcess = new CrawlProcess();
        final List<CrawlTask> crawlSteps = Lists.newArrayList(
                // by setting a higher priority each step you go deep first...
                // search google.com and only retain google.com domain links
                // if we encounter some link like this: http://xxx.google.com/4154545 then go back to this step else go to next step
                new CrawlTask().setCrawlStep(0).setUrl("http://www.google.com")
                        .setPriority(10).setPoliteness(600000).setDownload(false)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.([a-zA-Z]+)*\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^.+?/(\\d)+$"),
                // search the html and only retain links that are within one Levenshtein distance of each other
                // then only retain those from the google.com domain
                // if we encounter some link like this: http://xxx.google.com/4154545 then go back to step 0 else go to next step
                new CrawlTask().setCrawlStep(1).setPriority(50).setPoliteness(600000)
                        .setDownload(false)
                        .setCrawlFilterAlgorithm(new LevenshteinDistanceCrawlFilterAlgorithm()
                                .setLowerDistance(1)
                                .setUpperDistance(2))
                        .setCrawlFilterPattern("^https?://[^/@]*\\.([a-zA-Z]+)*\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^.+?/(\\d)+$"),
                // search the html and only retain links that are from the google.com domain
                // extract all img elements with source ending in jpg or jpeg and retain their absolute src width and height attributes
                // if we encounter some link like this: http://xxx.google.com/4154545 then go back to step 0 else go to next step
                new CrawlTask().setCrawlStep(2).setPriority(75).setPoliteness(600000)
                        .setDownload(true)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.([a-zA-Z]+)*\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^.+?/(\\d)+$")
                        .setResultQuery("img[src$=jpg],img[src$=jpeg]").setResultAttrQueries(ImmutableMap.of("src", "abs:src", "width", "width", "height", "height"))

        );
        crawlProcess.setCrawlSteps(crawlSteps);
        final ResponseEntity<CrawlResponse> responseEntity =
                restTemplate.exchange(seedEntryPoint, HttpMethod.PUT, new HttpEntity<>(crawlProcess), CrawlResponse.class);
        assertEquals("Seed swallowed!", responseEntity.getBody().getResponse());
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Thread.sleep(1000000000);
        // maybe assert there are some images somewhere?
    }

    @Test
    public void submitSeedMZZ() throws InterruptedException, IOException {
        String seedEntryPoint = "http://localhost:" + nodePort + "/seed";
        System.out.println(">>>>>>>>>>>> Test entry endpoint is located: " + seedEntryPoint + " <<<<<<<<<<<<<<<");
        Thread.sleep(1000);

        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return !HttpStatus.OK.equals(response.getStatusCode());
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                fail(response.getStatusText());
            }
        });

        final CrawlProcess crawlProcess = new CrawlProcess();
        final List<CrawlTask> crawlSteps = Lists.newArrayList(
                // by setting a higher priority each step you go deep first...
                // search google.com and only retain google.com domain links
                // if we encounter some link like this: http://xxx.google.com/4154545 then go back to this step else go to next step
                new CrawlTask().setCrawlStep(0).setUrl("http://www.google.com").setPriority(10).setPoliteness(600000)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.google\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^https?://[^/@]*\\.google\\.com/(\\d*)$"),
                // search the html and only retain links that are within one Levenshtein distance of each other
                // then only retain those from the google.com domain
                // if we encounter some link like this: http://xxx.google.com/4154545 then go back to step 0 else go to next step
                new CrawlTask().setCrawlStep(1).setPriority(50).setPoliteness(600000)
                        .setCrawlFilterAlgorithm(new LevenshteinDistanceCrawlFilterAlgorithm()
                                .setLowerDistance(1)
                                .setUpperDistance(2))
                        .setCrawlFilterPattern("^https?://[^/@]*\\.google\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^https?://[^/@]*\\.google\\.com/(\\d*)$"),
                // search the html and only retain links that are from the google.com domain
                // extract all img elements with source ending in jpg or jpeg and retain their absolute src width and height attributes
                // if we encounter some link like this: http://xxx.google.com/4154545 then go back to step 0 else go to next step
                new CrawlTask().setCrawlStep(2).setPriority(75).setPoliteness(600000)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.google\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^https?://[^/@]*\\.google\\.com/(\\d*)$")
                        .setResultQuery("img[src$=jpg],img[src$=jpeg]")
                        .setResultAttrQueries(ImmutableMap.of("src", "abs:src", "width", "width", "height", "height"))
                        .setResultAttrConditionals(new String[]{"(width > 300 && height > 350) || (width > 300 && height > 350)"})

        );
        crawlProcess.setCrawlSteps(crawlSteps);
        final ResponseEntity<CrawlResponse> responseEntity =
                restTemplate.exchange(seedEntryPoint, HttpMethod.PUT, new HttpEntity<>(crawlProcess), CrawlResponse.class);
        assertEquals("Seed swallowed!", responseEntity.getBody().getResponse());
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Thread.sleep(1000000);
        // maybe assert there are some images somewhere?
    }

    @Test
    public void submitSeedStandaard() throws InterruptedException, IOException {
        String seedEntryPoint = "http://localhost:" + nodePort + "/seed";
        System.out.println(">>>>>>>>>>>> Test entry endpoint is located: " + seedEntryPoint + " <<<<<<<<<<<<<<<");
        Thread.sleep(1000);

        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return !HttpStatus.OK.equals(response.getStatusCode());
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                fail(response.getStatusText());
            }
        });

        final CrawlProcess crawlProcess = new CrawlProcess();
        final List<CrawlTask> crawlSteps = Lists.newArrayList(
                new CrawlTask().setCrawlStep(0).setUrl("http://www.standaard.be").setPriority(75).setPoliteness(6000)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.standaard\\.be(/.*)?$")
                        .setResultQuery("img[src$=jpg],img[src$=jpeg]")
                        .setResultAttrQueries(ImmutableMap.of("src", "abs:src", "width", "width", "height", "height"))
                        .setResultAttrConditionals(new String[]{"(width > 350 && width < 360 && height > 170) || (width > 350 && height > 300)"})

        );
        crawlProcess.setCrawlSteps(crawlSteps);
        final ResponseEntity<CrawlResponse> responseEntity =
                restTemplate.exchange(seedEntryPoint, HttpMethod.PUT, new HttpEntity<>(crawlProcess), CrawlResponse.class);
        assertEquals("Seed swallowed!", responseEntity.getBody().getResponse());
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Thread.sleep(1000000);
        // maybe assert there are some images somewhere?
    }

    @Test
    public void submitSeedStandaardText() throws InterruptedException, IOException {
        String seedEntryPoint = "http://localhost:" + nodePort + "/seed";
        System.out.println(">>>>>>>>>>>> Test entry endpoint is located: " + seedEntryPoint + " <<<<<<<<<<<<<<<");
        Thread.sleep(1000);

        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return !HttpStatus.OK.equals(response.getStatusCode());
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                fail(response.getStatusText());
            }
        });

        final CrawlProcess crawlProcess = new CrawlProcess();
        final List<CrawlTask> crawlSteps = Lists.newArrayList(
                new CrawlTask().setCrawlStep(0).setUrl("http://www.standaard.be").setPriority(75).setPoliteness(600000)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.standaard\\.be(/.*)?$")
                        .setResultQuery("article > a")
                        .setResultAttrQueries(ImmutableMap.of("text", "text()", "link", "href"))
                        //.setResultAttrConditionals(new String[]{"text contains 'Verkoudheid'"})
                        //.addCrawlStepPattern(0, "^https?://[^/@]*\\.standaard\\.be/(\\d*)$")

        );
        crawlProcess.setCrawlSteps(crawlSteps);
        final ResponseEntity<CrawlResponse> responseEntity =
                restTemplate.exchange(seedEntryPoint, HttpMethod.PUT, new HttpEntity<>(crawlProcess), CrawlResponse.class);
        assertEquals("Seed swallowed!", responseEntity.getBody().getResponse());
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Thread.sleep(1000000);
        // maybe assert there are some images somewhere?
    }

    @Test
    public void submitSeedMMXXText() throws InterruptedException, IOException {
        String seedEntryPoint = "http://localhost:" + nodePort + "/seed";
        System.out.println(">>>>>>>>>>>> Test entry endpoint is located: " + seedEntryPoint + " <<<<<<<<<<<<<<<");
        Thread.sleep(1000);

        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return !HttpStatus.OK.equals(response.getStatusCode());
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                fail(response.getStatusText());
            }
        });

        final CrawlProcess crawlProcess = new CrawlProcess();
        final List<CrawlTask> crawlSteps = Lists.newArrayList(
                new CrawlTask().setCrawlStep(0).setUrl("http://www.google.org").setPriority(75).setPoliteness(6000)
                        .setDownload(false)
                        .setResultQuery("div.shot-more > div > a")
                        .setResultAttrQueries(ImmutableMap.of("link", "href"))
                        .setCrawlFilterPattern("(^https?://(www\\.)?+google\\.org/(hd-)?+full-movies(/.*)?$|^https?://(www\\.)?+google\\.org/page/(\\d*)/$)")
                        .addCrawlStepPattern(0, "^https?://[^/@]*\\.google\\.org/page/(\\d*)$"),
                new CrawlTask().setCrawlStep(1).setPriority(75).setPoliteness(6000)
                        .setDownload(false)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.pixhost\\.org(/.*)?$")
                        .setResultQuery("div.shot-title")
                        .setResultAttrQueries(ImmutableMap.of("text", "text()")),
                new CrawlTask().setCrawlStep(2).setPriority(75).setPoliteness(6000)
                        .setDownload(true)
                        .setResultQuery("img#show_image")
                        .setCrawlFilterPattern("^https?://[^/@]*\\.pixhost\\.org(/.*)?$")
                        .setResultAttrQueries(ImmutableMap.of("alt", "alt)"))
        );
        crawlProcess.setCrawlSteps(crawlSteps);
        final ResponseEntity<CrawlResponse> responseEntity =
                restTemplate.exchange(seedEntryPoint, HttpMethod.PUT, new HttpEntity<>(crawlProcess), CrawlResponse.class);
        assertEquals("Seed swallowed!", responseEntity.getBody().getResponse());
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Thread.sleep(1000000);
        // maybe assert there are some images somewhere?
    }

    @Test
    public void justStart() throws InterruptedException, IOException {

        String seedEntryPoint = "http://localhost:" + nodePort + "/seed";
        System.out.print(">>>>>>>>>>>> Test entry endpoint is located: " + seedEntryPoint + " <<<<<<<<<<<<<<<");

        Thread.sleep(Long.MAX_VALUE);
    }

}
