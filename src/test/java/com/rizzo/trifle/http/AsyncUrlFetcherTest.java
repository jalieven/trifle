package com.rizzo.trifle.http;

import com.google.common.base.Stopwatch;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class AsyncUrlFetcherTest {

    @Test
    public void testBaseUriParsing() throws MalformedURLException, URISyntaxException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (int t = 0; t < 1000; t++) {
            URI url = new URI("http://1.standaardcdn.be/Assets/Images_Upload/2014/10/13/bourgeois_blg.jpg?crop=(21.50,11.50,505.00,253.25)&cropxunits=505&cropyunits=357&maxheight=176&maxwidth=352&scale=both&format=jpg");
            String baseUri = url.getScheme() + "://" + url.getHost();
        }
        stopwatch.stop();
        System.out.println("Parse URI in milliseconds: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testBaseUrlParsing() throws MalformedURLException, URISyntaxException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (int t = 0; t < 1000; t++) {
            URL url = new URL("http://1.standaardcdn.be/Assets/Images_Upload/2014/10/13/bourgeois_blg.jpg?crop=(21.50,11.50,505.00,253.25)&cropxunits=505&cropyunits=357&maxheight=176&maxwidth=352&scale=both&format=jpg");
            String baseUrl = url.getProtocol() + "://" + url.getHost();
        }
        stopwatch.stop();
        System.out.println("Parse URL in milliseconds: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

}
