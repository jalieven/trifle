package com.rizzo.trifle.filter;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class LevenshteinDistanceCrawlFilterAlgorithmTest {

    @Test
    public void testLevenshtein(){
        LevenshteinDistanceCrawlFilterAlgorithm levenshteinDistanceCrawlFilterAlgorithm =
                new LevenshteinDistanceCrawlFilterAlgorithm();
        levenshteinDistanceCrawlFilterAlgorithm.setUpperDistance(2);
        levenshteinDistanceCrawlFilterAlgorithm.setLowerDistance(1);
        final String[] filtered = levenshteinDistanceCrawlFilterAlgorithm.filterLinks(
                new String[]{
                        "http://www.google.be",
                        "http://www.google.be/01.jpg",
                        "http://www.google.be/02.jpg",
                        "http://www.google.be/03.jpg",
                        "http://www.google.be/04.jpg",
                        "http://www.standaard.be/04.jpg",
                        "http://www.google.be/05.jpg",
                        "http://www.google.be/15.jpg",
                        "http://www.google.be/blabla",
                        "http://www.google.be/someother/path"
                });
        assertEquals(6, filtered.length);
        for (String jpegLink : filtered) {
            assertTrue(jpegLink.endsWith(".jpg"));
            assertTrue(jpegLink.contains("google.be"));
        }
    }

}
