package com.rizzo.trifle.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LevenshteinDistanceCrawlFilterAlgorithm implements CrawlFilterAlgorithm {

    private static final Logger LOGGER = LoggerFactory.getLogger(LevenshteinDistanceCrawlFilterAlgorithm.class);

    private Integer upperDistance;
    private Integer lowerDistance;

    @Override
    @JsonIgnore
    public String[] filterLinks(String[] unfilteredLinks) {
        List<String> unfiltered = Lists.newArrayList(unfilteredLinks);
        Set<String> interestingForwardLinks = Sets.newHashSet();
        Set<String> interestingReverseLinks = Sets.newHashSet();
        Collections.sort(unfiltered);

        String memory = "";
        for (int i = 0; i < unfiltered.size(); i++) {
            String link = unfiltered.get(i);
            final int levenshteinDistance = StringUtils.getLevenshteinDistance(memory, link);
            if (levenshteinDistance < upperDistance && levenshteinDistance >= lowerDistance) {
                interestingForwardLinks.add(link);
            }
            memory = link;
        }
        for (int i = (unfiltered.size() - 1); i >= 0; i--) {
            String link = unfiltered.get(i);
            final int levenshteinDistance = StringUtils.getLevenshteinDistance(memory, link);
            if (levenshteinDistance < upperDistance && levenshteinDistance >= lowerDistance) {
                interestingReverseLinks.add(link);
            }
            memory = link;
        }
        final Sets.SetView<String> union = Sets.union(interestingForwardLinks, interestingReverseLinks);
        LOGGER.debug("Levenshtein filtering retains the following links: " + union);
        return union.toArray(new String[union.size()]);
    }

    public Integer getUpperDistance() {
        return upperDistance;
    }

    public LevenshteinDistanceCrawlFilterAlgorithm setUpperDistance(Integer upperDistance) {
        this.upperDistance = upperDistance;
        return this;
    }

    public Integer getLowerDistance() {
        return lowerDistance;
    }

    public LevenshteinDistanceCrawlFilterAlgorithm setLowerDistance(Integer lowerDistance) {
        this.lowerDistance = lowerDistance;
        return this;
    }
}
