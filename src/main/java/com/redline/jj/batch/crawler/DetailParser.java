package com.redline.jj.batch.crawler;

import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.common.exception.BusinessException;

public interface DetailParser {

    CrawledProduct parse(String url) throws BusinessException;
}
