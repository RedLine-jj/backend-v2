package com.redline.jj.batch.job;

import com.redline.jj.batch.crawler.ListParser;
import org.springframework.batch.item.ItemReader;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CrawlItemReader implements ItemReader<String> {

    private final ListParser listParser;
    private final Queue<String> urlQueue = new LinkedList<>();
    private int currentPage = 1;
    private boolean exhausted = false;

    public CrawlItemReader(ListParser listParser) {
        this.listParser = listParser;
    }

    @Override
    public String read() {
        if (urlQueue.isEmpty() && !exhausted) {
            List<String> urls = listParser.parseProductUrls(currentPage);
            if (urls.isEmpty()) {
                exhausted = true;
            } else {
                currentPage++;
                urlQueue.addAll(urls);
            }
        }
        return urlQueue.poll();
    }
}
