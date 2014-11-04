package com.rizzo.trifle.image;

import com.rizzo.trifle.aop.LogPerformance;
import com.rizzo.trifle.domain.DownloadResult;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;

@Component
public class ImageCalculator implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCalculator.class);

    @Value("${image-calculation.metrics.dimension}")
    private Boolean dimension;

    @Value("${image-calculation.metrics.digest}")
    private Boolean digest;

    @Value("${image-calculation.digest-algoritm}")
    private String digestAlgorithm;

    private MessageDigest messageDigest;

    @Override
    public void afterPropertiesSet() throws Exception {
        messageDigest = MessageDigest.getInstance(digestAlgorithm);
    }

    @LogPerformance
    public void calculateDimension(DownloadResult downloadResult) {
        if (dimension) {
            try {
                final File imageFile = new File(downloadResult.getFileLocation());
                BufferedImage bufferedImage = ImageIO.read(imageFile);
                if (bufferedImage != null) {
                    int width = bufferedImage.getWidth();
                    int height = bufferedImage.getHeight();
                    if (width > height) {
                        downloadResult.getAttributes().put("orientation", "landscape");
                    } else if (width < height) {
                        downloadResult.getAttributes().put("orientation", "portrait");
                    } else {
                        downloadResult.getAttributes().put("orientation", "square");
                    }
                    downloadResult.getMetrics().put("width", (long) width);
                    downloadResult.getMetrics().put("height", (long) height);
                    downloadResult.getMetrics().put("file-size", imageFile.length());
                    downloadResult.getAttributes().put("readable", "true");
                } else {
                    downloadResult.getAttributes().put("readable", "false");
                }
            } catch (IOException | IllegalArgumentException e) {
                LOGGER.error("File " + downloadResult.getFileLocation() + "could not be read!", e);
                downloadResult.getAttributes().put("readable", "false");
            }
        }
    }

    @LogPerformance
    public String calculateDigest(DownloadResult downloadResult) {
        if(digest) {
            FileInputStream fileInputStream = null;
            try {
                final File imageFile = new File(downloadResult.getFileLocation());
                fileInputStream = new FileInputStream(imageFile);
                byte[] digest = this.messageDigest.digest(IOUtils.toByteArray(
                        fileInputStream));
                final String md5String = new String(Hex.encodeHex(digest));
                downloadResult.getAttributes().put(digestAlgorithm.toLowerCase(), md5String);
                downloadResult.getAttributes().put("readable", "true");
                return md5String;
            } catch (IOException e) {
                LOGGER.error("Could not calculate digest for file " + downloadResult.getFileLocation(), e);
                downloadResult.getAttributes().put("readable", "false");
            } finally {
                if(fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                        LOGGER.error("Could not close FileInputStream after digest calculation!", e);
                    }
                }
            }
        }
        return null;
    }

    public void setDimension(Boolean dimension) {
        this.dimension = dimension;
    }

    public void setDigest(Boolean digest) {
        this.digest = digest;
    }

    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

}
