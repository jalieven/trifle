package com.rizzo.trifle.elasticsearch;

import com.rizzo.trifle.domain.CrawlResult;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CrawlResultRepository extends ElasticsearchRepository<CrawlResult, String> {
}
