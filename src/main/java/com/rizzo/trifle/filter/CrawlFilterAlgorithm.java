package com.rizzo.trifle.filter;

import java.io.Serializable;

public interface CrawlFilterAlgorithm extends Serializable {

    String[] filterLinks(String[] unfilteredLinks);

}
