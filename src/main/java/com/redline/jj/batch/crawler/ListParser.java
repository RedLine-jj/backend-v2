package com.redline.jj.batch.crawler;

import com.redline.jj.common.exception.BusinessException;

import java.util.List;

public interface ListParser {

    List<String> parseProductUrls(int page) throws BusinessException;
}
