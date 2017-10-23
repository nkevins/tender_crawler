package com.chlorocode.tendercrawler.crawler;

import java.util.LinkedList;
import java.util.List;

public class CrawlerFactory {

    private List<Crawler> crawlers;

    public CrawlerFactory() {
        crawlers = new LinkedList<Crawler>();
    }

    public List<Crawler> getAllCrawler() {
        crawlers.add(new GeBizCrawler());
        crawlers.add(new SesamiNHGCrawler());
        crawlers.add(new SesamiCAGCrawler());
        crawlers.add(new SesamiSPCrawler());
        crawlers.add(new SingtelCrawler());
        crawlers.add(new SesamiSTECrawler());
        crawlers.add(new SATSCrawler());
        crawlers.add(new SingaporePoolsTenderCrawler());
        crawlers.add(new SingaporePoolsQuotationCrawler());

        return crawlers;
    }
}
