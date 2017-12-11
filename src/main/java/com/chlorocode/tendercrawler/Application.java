package com.chlorocode.tendercrawler;

import com.chlorocode.tendercrawler.crawler.Crawler;
import com.chlorocode.tendercrawler.crawler.CrawlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Application {

    final static Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("Tender Crawler Started");

        try {
            CrawlerFactory crawlerFactory = new CrawlerFactory();
            List<Crawler> crawlers = crawlerFactory.getAllCrawler();

            ExecutorService executor = Executors.newFixedThreadPool(20);

            for (Crawler c : crawlers) {
                executor.execute(c);
            }

            executor.shutdown();
            while (!executor.isTerminated()) {

            }
        } catch (Exception e) {
            logger.error("Main runner class exception", e);
        }

        logger.info("Tender Crawler Ended");
    }
}
