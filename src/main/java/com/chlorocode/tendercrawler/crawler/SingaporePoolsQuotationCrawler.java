package com.chlorocode.tendercrawler.crawler;

import com.chlorocode.tendercrawler.constant.TenderStatus;
import com.chlorocode.tendercrawler.model.Tender;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Crawler specific implementation for Singapore Pools Quotation.
 */
public class SingaporePoolsQuotationCrawler extends Crawler {

    final static Logger logger = LoggerFactory.getLogger(SingaporePoolsQuotationCrawler.class);
    final String URL = "http://www.singaporepools.com.sg/en/tnq/Pages/quotations.aspx";
    final int SOURCE_ID = 9;

    public void run() {
        logger.info("Starting Singapore Pools Quotation Crawler");

        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

        DateFormat publishDateFormat = new SimpleDateFormat("dd/MM/yy");
        DateFormat closingDateFormat = new SimpleDateFormat("dd/MM/yy H.m");

        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());

        List<Tender> tenders = new ArrayList<>();

        try {
            HtmlPage page = webClient.getPage(URL);
            webClient.waitForBackgroundJavaScript(1000);

            HtmlTable table = (HtmlTable) page.getElementById("tblTenderSummary");
            int rowCount = 0;
            int colCount = 0;
            for (HtmlTableRow row : table.getRows()) {
                try {
                    rowCount++;
                    // Skip header row
                    if (rowCount == 1) {
                        continue;
                    }

                    if (row.getCells().size() == 5) {
                        Tender tender = new Tender();
                        tender.setTenderSource(SOURCE_ID);
                        tender.setCompanyName("Singapore Pools");
                        tender.setStatus(TenderStatus.OPEN);

                        for (HtmlTableCell cell : row.getCells()) {
                            colCount++;

                            switch (colCount) {
                                case 2:
                                    tender.setReferenceNo(cell.asText());
                                    HtmlAnchor link = (HtmlAnchor) cell.getFirstChild();
                                    tender.setTenderURL("http://www.singaporepools.com.sg" + link.getHrefAttribute());
                                    break;
                                case 3:
                                    tender.setPublishedDate(publishDateFormat.parse(cell.asText()));
                                    break;
                                case 4:
                                    tender.setTitle(cell.asText());
                                    break;
                                case 5:
                                    List<HtmlElement> extendedDateSpan = cell.getElementsByAttribute("span", "style", "color:Red");
                                    if (extendedDateSpan.size() != 0) {
                                        // Tender is extended
                                        HtmlSpan span = (HtmlSpan) extendedDateSpan.get(0);
                                        String extendedDateString = StringUtils.substringBetween(span.asText(), "(", ")");
                                        tender.setClosingDate(closingDateFormat.parse(extendedDateString));
                                    } else {
                                        tender.setClosingDate(closingDateFormat.parse(cell.asText()));
                                    }
                                    break;
                            }
                        }
                        colCount = 0;

                        logger.debug(tender.toString());

                        tenders.add(tender);
                        if (tenders.size() > maxBulkSaveSize) {
                            logger.info("Reaching post limit of: " + maxBulkSaveSize + ", send data to API");

                            updateToDatabase(tenders);
                            tenders.clear();
                        }
                    } else {
                        logger.error("Table column size not expected. Expected: 5, Actual: " + row.getCells().size());
                    }
                } catch (Exception ex) {
                    logger.error("Exception while processing row", ex);
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

        logger.info("Ending Singapore Pools Quotation Crawler");
    }
}
