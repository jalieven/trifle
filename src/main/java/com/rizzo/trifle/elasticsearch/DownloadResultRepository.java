package com.rizzo.trifle.elasticsearch;

import com.rizzo.trifle.domain.DownloadResult;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DownloadResultRepository extends ElasticsearchRepository<DownloadResult, String> {
}
