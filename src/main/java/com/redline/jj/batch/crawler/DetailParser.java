package com.redline.jj.batch.crawler;

import com.redline.jj.batch.crawler.dto.CrawledProduct;
import com.redline.jj.common.exception.BusinessException;

import java.util.List;

public interface DetailParser {

    CrawledProduct parse(String url) throws BusinessException;

    default List<CrawledProduct> parseAll(String url) throws BusinessException {
        return List.of(parse(url));
    }
}
