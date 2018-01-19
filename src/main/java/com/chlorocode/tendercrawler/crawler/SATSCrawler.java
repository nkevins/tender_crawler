package com.chlorocode.tendercrawler.crawler;

import com.chlorocode.tendercrawler.constant.TenderStatus;
import com.chlorocode.tendercrawler.model.Tender;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

public class SATSCrawler extends Crawler {

    final static Logger logger = LoggerFactory.getLogger(SATSCrawler.class);
    final String URL = "https://www.sats.com.sg/Tenders/Pages/Tenders.aspx";
    final int SOURCE_ID = 7;

    public void run() {
        logger.info("Starting Sesami SATS Crawler");

        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());

        List<Tender> tenders = new ArrayList<Tender>();

        try {
            HtmlPage page = webClient.getPage(URL);
            webClient.waitForBackgroundJavaScript(1000);

            HtmlDivision tenderListDiv = (HtmlDivision) page.getElementById("tender-list");
            List<HtmlDivision> tenderDivisions = tenderListDiv.getByXPath("//div[@class='tender']");
            for (HtmlDivision t : tenderDivisions) {
                try {
                    Tender tender = new Tender();
                    tender.setTenderSource(SOURCE_ID);
                    tender.setCompanyName("SATS");
                    tender.setStatus(TenderStatus.OPEN);
                    tender.setTenderURL("https://www.sats.com.sg/Tenders/Pages/Tenders.aspx");

                    String tenderRefNo = t.getElementsByTagName("h6").get(0).asText();
                    String rawInfoContent = t.getElementsByTagName("p").get(0).asText();
                    String tenderTitle = rawInfoContent.split("\\r?\\n")[0];
                    String rawClosingTime = rawInfoContent.split("\\r?\\n")[1];
                    Date closingTime = parseClosingTime(rawClosingTime);

                    tender.setReferenceNo(tenderRefNo);
                    tender.setTitle(tenderTitle);
                    tender.setClosingDate(closingTime);

                    logger.debug(tender.toString());

                    tenders.add(tender);
                    if (tenders.size() > maxBulkSaveSize) {
                        logger.info("Reaching post limit of: " + maxBulkSaveSize + ", send data to API");

                        updateToDatabase(tenders);
                        tenders.clear();
                    }
                } catch (Exception ex) {
                    logger.error("Exception while processing tender item", ex);
                }
            }

            // Send to API for the remaining tenders
            if (tenders.size() != 0) {
                logger.info("Post remaining tenders (" + tenders.size() + ")");

                updateToDatabase(tenders);
                tenders.clear();
            }
        } catch (Exception ex) {
            logger.error("Exception occured in main function", ex);
        }

        logger.info("Ending Sesami SATS Crawler");
    }

    private Date parseClosingTime(String rawDate) {
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy H:m");

        String timePart = rawDate.substring(14, 18);
        String datePart = StringUtils.substringAfter(rawDate, "On ");
        String completeDate = datePart + " " + timePart.substring(0, 2) + ":" + timePart.substring(2, 4);

        try {
            Date convertedDate = dateFormat.parse(completeDate);
            return convertedDate;
        } catch (ParseException ex) {
            logger.error("Exception in parsing closing time " + rawDate, ex);
            return null;
        }
    }
}
