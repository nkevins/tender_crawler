package com.chlorocode.tendercrawler.crawler;

import com.chlorocode.tendercrawler.constant.TenderStatus;
import com.chlorocode.tendercrawler.model.Tender;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Crawler specific implementation for Sesami CAG.
 */
public class SesamiCAGCrawler extends Crawler {

    final static Logger logger = LoggerFactory.getLogger(SesamiCAGCrawler.class);
    final String URL = "https://sg.sesami.net/cag/businessOpportunities.jsp";
    final int SOURCE_ID = 3;

    public void run() {
        logger.info("Starting Sesami CAG Crawler");

        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy H:m");

        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());

        List<Tender> tenders = new ArrayList<>();

        try {
            HtmlPage page = webClient.getPage(URL);
            webClient.waitForBackgroundJavaScript(1000);

            boolean nextPageAvailable = true;
            int pageNo = 1;

            do {
                logger.info("Processing page: " + pageNo);
                HtmlSpan nextPageLink = page.getHtmlElementById("rfqTender_next");
                if (nextPageLink.getAttribute("class").contains("paginate_button_disabled")) {
                    nextPageAvailable = false;
                }

                HtmlTable table = page.getHtmlElementById("rfqTender");
                int rowCount = 0;
                int colCount = 0;
                for (HtmlTableRow row : table.getRows()) {
                    try {
                        rowCount++;
                        // Skip header row
                        if (rowCount == 1) {
                            continue;
                        }

                        if (row.getCells().size() == 8) {
                            Tender tender = new Tender();
                            tender.setTenderSource(SOURCE_ID);
                            tender.setCompanyName("CAG");
                            tender.setStatus(TenderStatus.OPEN);

                            for (HtmlTableCell cell : row.getCells()) {
                                colCount++;

                                switch (colCount) {
                                    case 2:
                                        tender.setReferenceNo(cell.asText());
                                        break;
                                    case 5:
                                        tender.setTitle(cell.asText());
                                        break;
                                    case 6:
                                        String startDate = cell.asText();
                                        tender.setPublishedDate(dateFormat.parse(startDate));
                                        break;
                                    case 7:
                                        String closingDate = cell.asText();
                                        tender.setClosingDate(dateFormat.parse(closingDate));
                                        break;
                                    case 8:
                                        HtmlAnchor link = cell.getOneHtmlElementByAttribute("a", "class", "links");
                                        String onclickValue = link.getAttribute("onclick");
                                        String parametersValue = onclickValue.substring(onclickValue.indexOf("(") + 1, onclickValue.indexOf(")"));
                                        String[] parameters = parametersValue.split(",");
                                        String url = parameters[0].replace("'", "");
                                        tender.setTenderURL(url);
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
                            logger.error("Table column size not expected. Expected: 8, Actual: " + row.getCells().size());
                        }
                    } catch (Exception ex) {
                        logger.error("Exception while processing row", ex);
                    }
                }

                if (nextPageAvailable) {
                    pageNo++;
                    logger.info("Going to next page: " + pageNo);
                    nextPageLink.click();
                    webClient.waitForBackgroundJavaScript(1000);
                }
            } while (nextPageAvailable);

            // Send to API for the remaining tenders
            if (tenders.size() != 0) {
                logger.info("Post remaining tenders (" + tenders.size() + ")");

                updateToDatabase(tenders);
                tenders.clear();
            }
        } catch (Exception ex) {
            logger.error("Exception occured in main function", ex);
        }

        logger.info("Ending Sesami CAG Crawler");
    }
}
