package com.rizzo.trifle.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.rizzo.trifle.domain.DownloadResult;
import com.rizzo.trifle.domain.DownloadTask;
import org.junit.Test;
import org.mvel2.MVEL;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class DownloadResultTest {

    @Test
    public void testToString() {
        DownloadResult downloadResult = new DownloadResult()
                .setId("http://www.google.be/img.jpg")
                .setBaseUrl("http://www.google.be")
                .setRedirected(false)
                .setFileLocation("/opt/some/where")
                .setContentType("application/jpeg")
                .setStatusCode(200)
                .setContentLength(15545L)
                .stamp();
        downloadResult.getAttributes().put("something", "bla");
        downloadResult.getAttributes().put("else", "bleh");
        downloadResult.getHeaders().put("header", ImmutableList.of("h1", "h2"));
        downloadResult.getMetrics().put("width", 44L);
        downloadResult.getMetrics().put("height", 44L);
        System.out.println(downloadResult);
    }

    @Test
    public void testMVELExpression() {
        DownloadTask downloadTask = new DownloadTask().setUrl("http://www.google.com")
                .setAttributes(ImmutableMap.of("one", "1", "two", "2"));
        assertTrue((Boolean) MVEL.eval("attributes.one == '1'", downloadTask));
        assertFalse((Boolean) MVEL.eval("attributes.two != '2'", downloadTask));
    }

    @Test
    public void testShouldDownload() {
        DownloadTask downloadTask1 = new DownloadTask()
                .setAttributes(ImmutableMap.of("one", "1", "two", "2")).setAttributeConditionals(ImmutableSet.of("two < 1"));
        assertFalse(downloadTask1.shouldDownload());

        DownloadTask downloadTask2 = new DownloadTask()
                .setAttributes(ImmutableMap.of("one", "1", "two", "2")).setAttributeConditionals(ImmutableSet.of("two == 2", "one < 100"));
        assertTrue(downloadTask2.shouldDownload());

        DownloadTask downloadTask3 = new DownloadTask()
                .setAttributes(ImmutableMap.of("one", "maple syrup", "two", "2")).setAttributeConditionals(ImmutableSet.of("two == 2", "one contains 'syrup'"));
        assertTrue(downloadTask3.shouldDownload());

        DownloadTask downloadTask4 = new DownloadTask()
                .setAttributes(ImmutableMap.of("one", "maple syrup", "two", "2"));
        assertTrue(downloadTask4.shouldDownload());

        DownloadTask downloadTask5 = new DownloadTask()
                .setAttributes(ImmutableMap.of("one", "maple syrup", "two", "1")).setAttributeConditionals(ImmutableSet.of("two == 2 || one contains 'syrup'"));
        assertTrue(downloadTask5.shouldDownload());

        DownloadTask downloadTask6 = new DownloadTask()
                .setAttributes(ImmutableMap.of("one", "1", "two", "2")).setAttributeConditionals(ImmutableSet.of("two > 1 && one == 2"));
        assertFalse(downloadTask6.shouldDownload());

        DownloadTask downloadTask8 = new DownloadTask()
                .setUrl("floosie").setUrlConditional("strsim 'floo' > 0.5");
        assertTrue(downloadTask8.shouldDownload());

        DownloadTask downloadTask9 = new DownloadTask()
                .setUrl("floosie").setUrlConditional("contains 'ddd'");
        assertFalse(downloadTask9.shouldDownload());

        DownloadTask downloadTask10 = new DownloadTask()
                .setUrl("http://www.google.com").setUrlConditional("~= '^https?://[^/@]*.google.com(/.*)?$'");
        assertTrue(downloadTask10.shouldDownload());
        downloadTask10.setUrl("https://www.google.com/sdfsdfds");
        assertTrue(downloadTask10.shouldDownload());
        downloadTask10.setUrl("https://www.google.be");
        assertFalse(downloadTask10.shouldDownload());
    }
}
