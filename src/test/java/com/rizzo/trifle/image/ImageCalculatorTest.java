package com.rizzo.trifle.image;

import com.google.common.base.Stopwatch;
import com.rizzo.trifle.domain.DownloadResult;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

public class ImageCalculatorTest {

    @Test
    public void testDimensionCalculation() throws Exception {
        ImageCalculator imageCalculator = new ImageCalculator();
        imageCalculator.setDigest(true);
        imageCalculator.setDigestAlgorithm("MD5");
        imageCalculator.setDimension(true);
        imageCalculator.afterPropertiesSet();
        final DownloadResult downloadResult = new DownloadResult();
        ClassPathResource classPathResource = new ClassPathResource("/metrics.jpg");
        final Path tempFile = Files.createTempFile("testMessageDigestMD5Calculation", "");
        IOUtils.copy(classPathResource.getInputStream(), new FileOutputStream(tempFile.toFile()));
        downloadResult.setFileLocation(tempFile.toAbsolutePath().toString());

        Stopwatch stopwatch = Stopwatch.createStarted();
        imageCalculator.calculateDimension(downloadResult);
        assertEquals(new Long(536), downloadResult.getMetrics().get("width"));
        assertEquals(new Long(177), downloadResult.getMetrics().get("height"));
        assertEquals("true", downloadResult.getAttributes().get("readable"));
        assertEquals("landscape", downloadResult.getAttributes().get("orientation"));
        stopwatch.stop();
        System.out.println("Dimension calculation: took " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testDimensionNotConfigured() throws Exception {
        ImageCalculator imageCalculator = new ImageCalculator();
        imageCalculator.setDigest(false);
        imageCalculator.setDigestAlgorithm("MD5");
        imageCalculator.setDimension(false);
        imageCalculator.afterPropertiesSet();
        final DownloadResult downloadResult = new DownloadResult();
        ClassPathResource classPathResource = new ClassPathResource("/metrics.jpg");
        final Path tempFile = Files.createTempFile("testMessageDigestMD5Calculation", "");
        IOUtils.copy(classPathResource.getInputStream(), new FileOutputStream(tempFile.toFile()));
        downloadResult.setFileLocation(tempFile.toAbsolutePath().toString());

        imageCalculator.calculateDimension(downloadResult);
        assertNull(downloadResult.getMetrics().get("width"));
        assertNull(downloadResult.getMetrics().get("height"));
        assertNull(downloadResult.getAttributes().get("readable"));
        assertNull(downloadResult.getAttributes().get("orientation"));
    }

    @Test
    public void testMessageDigestMD5Calculation() throws Exception {
        ImageCalculator imageCalculator = new ImageCalculator();
        imageCalculator.setDigest(true);
        imageCalculator.setDigestAlgorithm("MD5");
        imageCalculator.setDimension(false);
        imageCalculator.afterPropertiesSet();
        final DownloadResult downloadResult = new DownloadResult();
        ClassPathResource classPathResource = new ClassPathResource("/metrics.jpg");
        final Path tempFile = Files.createTempFile("testMessageDigestMD5Calculation", "");
        IOUtils.copy(classPathResource.getInputStream(), new FileOutputStream(tempFile.toFile()));
        downloadResult.setFileLocation(tempFile.toAbsolutePath().toString());

        Stopwatch stopwatch = Stopwatch.createStarted();
        final String digest = imageCalculator.calculateDigest(downloadResult);
        stopwatch.stop();
        assertTrue(StringUtils.isNotBlank(digest));
        System.out.println("MD5: " + digest + " took " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testMessageDigestSHA1Calculation() throws Exception {
        ImageCalculator imageCalculator = new ImageCalculator();
        imageCalculator.setDigest(true);
        imageCalculator.setDigestAlgorithm("SHA1");
        imageCalculator.setDimension(false);
        imageCalculator.afterPropertiesSet();
        final DownloadResult downloadResult = new DownloadResult();
        ClassPathResource classPathResource = new ClassPathResource("/metrics.jpg");
        final Path tempFile = Files.createTempFile("testMessageDigestSHA1Calculation", "");
        IOUtils.copy(classPathResource.getInputStream(), new FileOutputStream(tempFile.toFile()));
        downloadResult.setFileLocation(tempFile.toAbsolutePath().toString());

        Stopwatch stopwatch = Stopwatch.createStarted();
        final String digest = imageCalculator.calculateDigest(downloadResult);
        stopwatch.stop();
        assertTrue(StringUtils.isNotBlank(digest));
        System.out.println("SHA1: " + digest + " took " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test(expected = FileNotFoundException.class)
    public void testMessageDigestMD5Fail() throws Exception {
        ImageCalculator imageCalculator = new ImageCalculator();
        imageCalculator.setDigest(true);
        imageCalculator.setDigestAlgorithm("MD5");
        imageCalculator.setDimension(false);
        imageCalculator.afterPropertiesSet();
        final DownloadResult downloadResult = new DownloadResult();
        ClassPathResource classPathResource = new ClassPathResource("/doesnotexist.jpg");
        final Path tempFile = Files.createTempFile("testMessageDigestMD5Calculation", "");
        IOUtils.copy(classPathResource.getInputStream(), new FileOutputStream(tempFile.toFile()));
        downloadResult.setFileLocation(tempFile.toAbsolutePath().toString());

        Stopwatch stopwatch = Stopwatch.createStarted();
        final String digest = imageCalculator.calculateDigest(downloadResult);
        stopwatch.stop();
        assertTrue(StringUtils.isNotBlank(digest));
    }

    @Test
    public void testMessageDigestNotConfigured() throws Exception {
        ImageCalculator imageCalculator = new ImageCalculator();
        imageCalculator.setDigest(false);
        imageCalculator.setDigestAlgorithm("MD5");
        imageCalculator.setDimension(false);
        imageCalculator.afterPropertiesSet();
        final DownloadResult downloadResult = new DownloadResult();
        ClassPathResource classPathResource = new ClassPathResource("/metrics.jpg");
        final Path tempFile = Files.createTempFile("testMessageDigestMD5Calculation", "");
        IOUtils.copy(classPathResource.getInputStream(), new FileOutputStream(tempFile.toFile()));
        downloadResult.setFileLocation(tempFile.toAbsolutePath().toString());

        final String digest = imageCalculator.calculateDigest(downloadResult);
        assertTrue(StringUtils.isBlank(digest));
    }

}
