package com.redline.jj.batch.matching;

import com.redline.jj.batch.crawler.dto.CrawledProduct;

import java.util.Optional;

public interface LlmMatchClient {

    Optional<LlmMatchResult> match(CrawledProduct product);
}
