package com.chlorocode.tendercrawler.crawler;

import com.chlorocode.tendercrawler.constant.TenderStatus;
import com.chlorocode.tendercrawler.model.Tender;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Crawler specific implementation for GeBiz.
 */
public class GeBizCrawler extends Crawler {

    final static Logger logger = LoggerFactory.getLogger(GeBizCrawler.class);
    final String URL = "https://www.gebiz.gov.sg/ptn/opportunity/BOListing.xhtml?origin=menu";
    final int SOURCE_ID = 1;

    public void run() {
        logger.info("Starting GeBiz Crawler");

        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());

        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy K:m a");

        List<Tender> tenders = new ArrayList<>();

        try {
            // Get Open Opportunities
            logger.info("Start Crawling Open Opportunities");
            HtmlPage page = webClient.getPage(URL);

            boolean nextPageAvailable = true;
            int pageNo = 1;

            do {
                logger.info("Processing page: " + pageNo);
                HtmlInput nextButtonInput = page.getFirstByXPath("//input[@type='submit' and @value='Next']");
                if (nextButtonInput.getAttribute("disabled").equals("disabled")) {
                    nextPageAvailable = false;
                }

                List<?> tenderNoDivs = page.getByXPath("//div[@class='formSectionHeader6_TEXT']");

                for (int i = 0; i < tenderNoDivs.size(); i++) {
                    try {
                        HtmlDivision tenderNoDiv = (HtmlDivision) tenderNoDivs.get(i);
                        String tenderNo = tenderNoDiv.getTextContent().trim().replaceAll("\\s{2,}", " ");
                        String[] tenderNoRawParts = tenderNo.split(" - ");
                        String[] tenderNoParts = tenderNoRawParts[1].split("/");
                        tenderNo = tenderNoParts[0];

                        HtmlAnchor title = page.getFirstByXPath("//div[text()='" + tenderNoDiv.getTextContent() + "']/following::a[@class='commandLink_TITLE-BLUE']");
                        HtmlDivision status = page.getFirstByXPath("//div[text()='" + tenderNoDiv.getTextContent() + "']/following::div[@class='formSectionHeader6_CHILD-DIV']");
                        HtmlDivision closingDate = page.getFirstByXPath("//div[text()='" + tenderNoDiv.getTextContent() + "']/following::div[text()='Closing on']/following::div[@class='formOutputText_HIDDEN-LABEL outputText_DATE-GREEN']");
                        HtmlDivision agency = page.getFirstByXPath("//div[text()='" + tenderNoDiv.getTextContent() + "']/following::span[text()='Agency']/following::div[@class='formOutputText_VALUE-DIV ']");
                        HtmlDivision publishedDate = page.getFirstByXPath("//div[text()='" + tenderNoDiv.getTextContent() + "']/following::span[text()='Published']/following::div[@class='formOutputText_VALUE-DIV ']");

                        Tender t = new Tender();
                        t.setReferenceNo(tenderNo);
                        t.setTitle(title.getTextContent().trim().replaceAll("\\s{2,}", " "));
                        t.setCompanyName(agency.getTextContent().trim());
                        t.setPublishedDate(dateFormat.parse(publishedDate.getTextContent().trim()));
                        t.setStatus(mapTenderStatus(status.getTextContent().trim()));
                        t.setTenderSource(SOURCE_ID);
                        t.setTenderURL("https://www.gebiz.gov.sg/ptn/opportunityportal/opportunityDetails.xhtml?code=" + tenderNo);

                        String formattedClosingDate = closingDate.getFirstChild().getTextContent().trim() + " " + closingDate.getLastChild().getTextContent().trim();
                        formattedClosingDate = formattedClosingDate.replace("PM", " PM");
                        formattedClosingDate = formattedClosingDate.replace("AM", " AM");
                        t.setClosingDate(dateFormat.parse(formattedClosingDate));

                        logger.debug(t.toString());

                        tenders.add(t);

                        if (tenders.size() > maxBulkSaveSize) {
                            logger.info("Reaching post limit of: " + maxBulkSaveSize + ", send data to API");

                            updateToDatabase(tenders);
                            tenders.clear();
                        }
                    } catch (Exception ex) {
                        logger.error("Exception while processing row", ex);
                    }
                }

                if (nextPageAvailable) {
                    int delay = ThreadLocalRandom.current().nextInt(1000, 5000 + 1);
                    Thread.sleep(delay);
                    pageNo++;
                    logger.info("Going to next page: " + pageNo);
                    nextButtonInput.click();
                    webClient.waitForBackgroundJavaScript(1000);
                }
            } while (nextPageAvailable);
            logger.info("End Crawling Open Opportunities");

            // Get closed opportunities
            logger.info("Start Crawling Closed Opportunities");
            HtmlInput closedOpportunitiesLink = page.getFirstByXPath("//input[@type='submit' and contains(@value, 'Closed (')]");
            closedOpportunitiesLink.click();

            pageNo = 1;
            nextPageAvailable = true;

            do {
                logger.info("Processing page: " + pageNo);
                HtmlInput nextButtonInput = page.getFirstByXPath("//input[@type='submit' and @value='Next']");
                if (nextButtonInput.getAttribute("disabled").equals("disabled")) {
                    nextPageAvailable = false;
                }

                List<?> tenderNoDivs = page.getByXPath("//div[@class='formSectionHeader6_TEXT']");

                for (int i = 0; i < tenderNoDivs.size(); i++) {
                    try {
                        HtmlDivision tenderNoDiv = (HtmlDivision) tenderNoDivs.get(i);
                        String tenderNo = tenderNoDiv.getTextContent().trim().replaceAll("\\s{2,}", " ");

                        HtmlAnchor title = page.getFirstByXPath("//div[text()='" + tenderNoDiv.getTextContent() + "']/following::a[@class='commandLink_TITLE-BLUE']");
                        HtmlDivision status = page.getFirstByXPath("//div[text()='" + tenderNoDiv.getTextContent() + "']/following::div[@class='formSectionHeader6_CHILD-DIV']");
                        HtmlDivision closingDate = page.getFirstByXPath("//div[text()='" + tenderNoDiv.getTextContent() + "']/following::div[text()='Closed']/following::div[@class='formOutputText_HIDDEN-LABEL outputText_DATE-GREEN']");
                        HtmlDivision agency = page.getFirstByXPath("//div[text()='" + tenderNoDiv.getTextContent() + "']/following::span[text()='Agency']/following::div[@class='formOutputText_VALUE-DIV ']");
                        HtmlDivision publishedDate = page.getFirstByXPath("//div[text()='" + tenderNoDiv.getTextContent() + "']/following::span[text()='Published']/following::div[@class='formOutputText_VALUE-DIV ']");

                        Tender t = new Tender();
                        t.setReferenceNo(tenderNo);
                        t.setTitle(title.getTextContent().trim().replaceAll("\\s{2,}", " "));
                        t.setCompanyName(agency.getTextContent().trim());
                        t.setPublishedDate(dateFormat.parse(publishedDate.getTextContent().trim()));
                        t.setStatus(mapTenderStatus(status.getTextContent().trim()));
                        t.setTenderSource(SOURCE_ID);
                        t.setTenderURL("https://www.gebiz.gov.sg/ptn/opportunityportal/opportunityDetails.xhtml?code=" + tenderNo);

                        String formattedClosingDate = closingDate.getFirstChild().getTextContent().trim() + " " + closingDate.getLastChild().getTextContent().trim();
                        formattedClosingDate = formattedClosingDate.replace("PM", " PM");
                        formattedClosingDate = formattedClosingDate.replace("AM", " AM");
                        t.setClosingDate(dateFormat.parse(formattedClosingDate));

                        logger.debug(t.toString());

                        tenders.add(t);

                        if (tenders.size() > maxBulkSaveSize) {
                            logger.info("Reaching post limit of: " + maxBulkSaveSize + ", send data to API");

                            updateToDatabase(tenders);
                            tenders.clear();
                        }
                    } catch (Exception ex) {
                        logger.error("Exception while processing row", ex);
                    }
                }

                if (nextPageAvailable) {
                    int delay = ThreadLocalRandom.current().nextInt(1000, 5000 + 1);
                    Thread.sleep(delay);
                    pageNo++;
                    logger.info("Going to next page: " + pageNo);
                    nextButtonInput.click();
                    webClient.waitForBackgroundJavaScript(1000);
                }
            } while (nextPageAvailable);
            logger.info("Start Crawling Closed Opportunities");

            // Send to API for the remaining tenders
            if (tenders.size() != 0) {
                logger.info("Post remaining tenders (" + tenders.size() + ")");

                updateToDatabase(tenders);
                tenders.clear();
            }
        } catch(Exception ex) {
            logger.error("Exception occured in main function", ex);
        }

        logger.info("Ending GeBiz Crawler");
    }

    private String mapTenderStatus(String status) {
        switch (status) {
            case "OPEN":
                return TenderStatus.OPEN;
            case "CLOSED":
                return TenderStatus.CLOSED;
            case "PENDING AWARD":
                return TenderStatus.CLOSED;
            case "AWARDED":
                return TenderStatus.AWARDED;
            case "NO AWARD":
                return TenderStatus.NO_AWARD;
            default:
                return TenderStatus.OTHERS;
        }
    }
}
