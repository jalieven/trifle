package com.rizzo.trifle.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.rizzo.trifle.domain.CrawlProcess;
import com.rizzo.trifle.domain.CrawlTask;
import com.rizzo.trifle.filter.CrawlFilterAlgorithm;
import com.rizzo.trifle.filter.LevenshteinDistanceCrawlFilterAlgorithm;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Examples:
 *
 *  "^https?://.+?$" doesn't matter what url matches as long as it has text
 *  "^.+?/(\\d)+$" ends with slash digits
 *  "^https?://[^/@]*\.google\.be(/.*)?$" is of the google.be domain
 */
public class CrawlTaskTest {

    @Test
    public void shouldVisitAllTest() {
        CrawlTask crawlTask = new CrawlTask().setCrawlFilterPattern(".*");
        assertTrue(crawlTask.shouldVisit("http://www.google.be"));
        assertTrue(crawlTask.shouldVisit("ftp://belgium.be"));
    }

    @Test
    public void shouldVisitProtocolTest() {
        CrawlTask crawlTask = new CrawlTask().setCrawlFilterPattern("^(https?)://.*$");
        assertTrue(crawlTask.shouldVisit("http://www.google.be"));
    }

    @Test
    public void shouldVisitUrlTest() {
        CrawlTask crawlTask = new CrawlTask().setCrawlFilterPattern("^(https?)://www.google.be/.*$");
        assertTrue(crawlTask.shouldVisit("http://www.google.be/bla?one=value&two=value"));
        assertFalse(crawlTask.shouldVisit("http://www.google.be"));
        assertFalse(crawlTask.shouldVisit("http://www.stackoverflow.com"));
    }

    @Test
    public void shouldVisitDomainTest() {
        CrawlTask crawlTask = new CrawlTask().setCrawlFilterPattern("^https?://[^/@]*\\.google\\.be(/.*)?$");
        assertTrue(crawlTask.shouldVisit("http://www.google.be/bla?one=value&two=value"));
        assertTrue(crawlTask.shouldVisit("https://db.google.be"));
        assertFalse(crawlTask.shouldVisit("https://db.google.com"));
        assertFalse(crawlTask.shouldVisit("https://db.bla.google.com"));
        assertFalse(crawlTask.shouldVisit("http://www.stackoverflow.com"));
    }

    @Test
    public void shouldVisitDomainNumericPathTest() {
        CrawlTask crawlTask = new CrawlTask().setCrawlFilterPattern("^https?://[^/@]*\\.google\\.be/(\\d*)+$");
        assertFalse(crawlTask.shouldVisit("http://www.google.be/bla?one=value&two=value"));
        assertFalse(crawlTask.shouldVisit("https://db.google.be"));
        assertFalse(crawlTask.shouldVisit("https://db.google.com"));
        assertFalse(crawlTask.shouldVisit("https://db.bla.google.com"));
        assertFalse(crawlTask.shouldVisit("http://www.stackoverflow.com"));
        assertTrue(crawlTask.shouldVisit("http://www.google.be/2937672"));
    }

    @Test
    public void nextStepDomainNumericTest() {
        CrawlTask crawlTask1 = new CrawlTask()
                .setUrl("http://db.google.be/2937672")
                .addCrawlStepPattern(1, "^https?://[^/@]*\\.google\\.be/(\\d*)+$")
                .setCrawlStep(2);
        assertEquals(new Integer(1), crawlTask1.nextStep());
        CrawlTask crawlTask2 = new CrawlTask()
                .setUrl("http://db.google.be/bla")
                .addCrawlStepPattern(1, "^https?://[^/@]*\\.google\\.be/(\\d*)+$")
                .setCrawlStep(2);
        assertEquals(new Integer(3), crawlTask2.nextStep());
        CrawlTask crawlTask3 = new CrawlTask()
                .setUrl("http://db.google.be/2937672")
                .addCrawlStepPattern(1, "^https?://[^/@]*\\.google\\.be/(\\d*)+$")
                .addCrawlStepPattern(4, "^https?://[^/@]*\\.google\\.be/(\\d*)+$")
                .setCrawlStep(2);
        assertEquals(new Integer(1), crawlTask3.nextStep());
        CrawlTask crawlTask4 = new CrawlTask()
                .setUrl("http://db.google.be/5555")
                .addCrawlStepPattern(1, "^https?://[^/@]*\\.google\\.be/(\\d*)$")
                .setCrawlStep(2);
        assertEquals(new Integer(1), crawlTask4.nextStep());
        CrawlTask crawlTask5 = new CrawlTask()
                .setUrl("http://db.google.be/55/55")
                .addCrawlStepPattern(1, "^https?://[^/@]*\\.google\\.be/(\\d*)$")
                .setCrawlStep(2);
        assertEquals(new Integer(3), crawlTask5.nextStep());
    }

    @Test
    public void shouldVisitTest() {
        CrawlTask crawlTask1 = new CrawlTask();
        crawlTask1.setCrawlFilterPattern("^https?://[^/@]*\\.([a-zA-Z]+)*\\.com/(\\d*)$");
        assertTrue(crawlTask1.shouldVisit("http://www.google.com/099"));
        assertFalse(crawlTask1.shouldVisit("http://www.google.com/bookmark1"));

        CrawlTask crawlTask2 = new CrawlTask();
        crawlTask2.setCrawlFilterPattern("(^https?://(www\\.)?+google\\.org/(hd-)?+full-movies(/.*)?$|^https?://(www\\.)?+google\\.org/page/(\\d*)/$)");
        assertTrue(crawlTask2.shouldVisit("http://www.google.org/hd-full-movies/8413-rls-2014.html"));
        assertTrue(crawlTask2.shouldVisit("https://google.org/full-movies/8413-rls-2014.html"));
        assertTrue(crawlTask2.shouldVisit("https://google.org/page/4/"));
        assertFalse(crawlTask2.shouldVisit("http://www.google.org/pages"));

        CrawlTask crawlTask3 = new CrawlTask();
        crawlTask3.setCrawlFilterPattern("^.+?/(\\d)+$");
        assertTrue(crawlTask3.shouldVisit("http://www.google.com/099"));
        assertFalse(crawlTask3.shouldVisit("http://www.google.com/bookmark1"));
        assertFalse(crawlTask3.shouldVisit("http://www.google.com/0888?pretty=true"));

        CrawlTask crawlTask4 = new CrawlTask();
        crawlTask4.setCrawlFilterPattern("^https?://.*$");
        assertTrue(crawlTask4.shouldVisit("http://www.google.com/099"));
        assertTrue(crawlTask4.shouldVisit("http://www.google.com/0888?pretty=true&bla=1"));
        assertTrue(crawlTask4.shouldVisit("http://google.com"));


    }

    @Test
    public void serializationTest() throws IOException {
        final CrawlFilterAlgorithm filterAlgorithm = new LevenshteinDistanceCrawlFilterAlgorithm().setLowerDistance(1).setUpperDistance(2);
        Map<String, String> resultAttrQueries = new HashMap<>(ImmutableMap.of("src", "abs:src", "width", "width", "height", "height"));
        CrawlTask crawlTask = new CrawlTask().setUrl("http://www.google.be")
                .setPriority(50).setResultQuery("img[src$=jpg]")
                .setCrawlFilterPattern("^https?://[^/@]*\\.google\\.be(/.*)?$")
                .setResultAttrQueries(resultAttrQueries)
                .setCrawlFilterAlgorithm(filterAlgorithm);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping();
        final StringWriter stringWriter = new StringWriter();
        objectMapper.writeValue(stringWriter, crawlTask);
        final CrawlTask reifiedCrawlTask = objectMapper.readValue(stringWriter.toString().getBytes(), CrawlTask.class);
        assertEquals("http://www.google.be", reifiedCrawlTask.getUrl());
        assertEquals(new Integer(50), reifiedCrawlTask.getPriority());
        assertEquals("img[src$=jpg]", reifiedCrawlTask.getResultQuery());
        assertEquals("^https?://[^/@]*\\.google\\.be(/.*)?$", reifiedCrawlTask.getCrawlFilterPattern());
        assertEquals(new Integer(2), ((LevenshteinDistanceCrawlFilterAlgorithm) reifiedCrawlTask.getCrawlFilterAlgorithm()).getUpperDistance());
        reifiedCrawlTask.getResultAttrQueries();


        final CrawlProcess crawlProcess = new CrawlProcess();
        final CrawlFilterAlgorithm levensthein =
                new LevenshteinDistanceCrawlFilterAlgorithm()
                        .setLowerDistance(1)
                        .setUpperDistance(2);
        final List<CrawlTask> crawlSteps = Lists.newArrayList(
                new CrawlTask().setCrawlStep(0).setUrl("http://www.google.com").setPriority(100).setPoliteness(600000)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.google\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^https?://[^/@]*\\.google\\.com(/\\d*)+$"),
                new CrawlTask().setCrawlStep(1).setPriority(50).setPoliteness(600000)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.google\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^https?://[^/@]*\\.google\\.com(/\\d*)+$")
                        .setCrawlFilterAlgorithm(levensthein),
                new CrawlTask().setCrawlStep(2).setPriority(75).setPoliteness(600000)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.google\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^https?://[^/@]*\\.google\\.com(/\\d*)+$")
                        .setResultQuery("img[src$=jpg],img[src$=jpeg]").setResultAttrQueries(
                        new HashMap<>(ImmutableMap.of("src", "abs:src", "width", "width", "height", "height")))
        );
        crawlProcess.setCrawlSteps(crawlSteps);
        final StringWriter stringWriterProcess = new StringWriter();
        objectMapper.writeValue(stringWriterProcess, crawlProcess);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String asString = objectMapper.writeValueAsString(crawlProcess);
        System.out.print(asString);
        stringWriterProcess.toString();
        final CrawlProcess reifiedCrawlProcess = objectMapper.readValue(stringWriterProcess.toString().getBytes(), CrawlProcess.class);
        reifiedCrawlProcess.getCrawlSteps();
    }

    @Test
    public void generateCurlScript() throws IOException {
        final CrawlProcess crawlProcess1 = new CrawlProcess();
        final List<CrawlTask> crawlSteps1 = Lists.newArrayList(
                new CrawlTask().setCrawlStep(0).setUrl("http://www.google.com").setPriority(10).setPoliteness(100000)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.google\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^https?://[^/@]*\\.google\\.com/(\\d*)$"),
                new CrawlTask().setCrawlStep(1).setPriority(50).setPoliteness(100000)
                        .setCrawlFilterAlgorithm(new LevenshteinDistanceCrawlFilterAlgorithm()
                                .setLowerDistance(1)
                                .setUpperDistance(2))
                        .setCrawlFilterPattern("^https?://[^/@]*\\.google\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^https?://[^/@]*\\.google\\.com/(\\d*)$"),
                new CrawlTask().setCrawlStep(2).setPriority(75).setPoliteness(100000)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.google\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^https?://[^/@]*\\.google\\.com/(\\d*)$")
                        .setResultQuery("img[src$=jpg],img[src$=jpeg]").setResultAttrQueries(
                        new HashMap<>(ImmutableMap.of("src", "abs:src", "width", "width", "height", "height")))

        );
        crawlProcess1.setCrawlSteps(crawlSteps1);

        final CrawlProcess crawlProcess2 = new CrawlProcess();
        final List<CrawlTask> crawlSteps2 = Lists.newArrayList(
                new CrawlTask().setCrawlStep(0).setUrl("http://www.google.com").setPriority(10).setPoliteness(100000)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.google\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^https?://[^/@]*\\.google\\.com/(\\d*)$"),
                new CrawlTask().setCrawlStep(1).setPriority(50).setPoliteness(100000)
                        .setCrawlFilterAlgorithm(new LevenshteinDistanceCrawlFilterAlgorithm()
                                .setLowerDistance(1)
                                .setUpperDistance(2))
                        .setCrawlFilterPattern("^https?://[^/@]*\\.google\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^https?://[^/@]*\\.google\\.com/(\\d*)$"),
                new CrawlTask().setCrawlStep(2).setPriority(75).setPoliteness(100000)
                        .setCrawlFilterPattern("^https?://[^/@]*\\.google\\.com(/.*)?$")
                        .addCrawlStepPattern(0, "^https?://[^/@]*\\.google\\.com/(\\d*)$")
                        .setResultQuery("img[src$=jpg],img[src$=jpeg]").setResultAttrQueries(
                        new HashMap<>(ImmutableMap.of("src", "abs:src", "width", "width", "height", "height")))

        );
        crawlProcess2.setCrawlSteps(crawlSteps2);


        List<CrawlProcess> processes = ImmutableList.of(crawlProcess1, crawlProcess2);


        ObjectMapper objectMapper = new ObjectMapper();
        String port = "49281";

        File scriptFile = new File("/Users/janlievens/Code/Sandbox/trifle/scripts/gatling.sh");

        FileUtils.deleteQuietly(scriptFile);
        Files.touch(scriptFile);
        Files.append(new StringBuffer("#!/bin/bash\n"), scriptFile, Charset.forName("UTF-8"));

        for (CrawlProcess crawlProcess : processes) {
            StringBuffer one = new StringBuffer("curl -XPUT -H 'Content-Type: application/json' -d '" + objectMapper.writeValueAsString(crawlProcess) + "' 'http://localhost:" + port + "/seed'\n");
            Files.append(one, scriptFile, Charset.forName("UTF-8"));
        }

    }


}