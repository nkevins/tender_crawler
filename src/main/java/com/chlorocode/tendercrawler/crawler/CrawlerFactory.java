package com.chlorocode.tendercrawler.crawler;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is a factory class used to generate the list of all crawlers.
 */
public class CrawlerFactory {

    private List<Crawler> crawlers;

    /**
     * Constructor.
     */
    public CrawlerFactory() {
        crawlers = new LinkedList<>();
    }

    /**
     * This method is used to get list of all crawlers available.
     *
     * @return list of crawlers
     */
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
