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
 * Crawler specific implementation for Sesami STE.
 */
public class SesamiSTECrawler extends Crawler {

    final static Logger logger = LoggerFactory.getLogger(SesamiSTECrawler.class);
    final String URL = "https://sg.sesami.net/ste/gtprfqlist.jsp?frmPg=Login.jsp&docType=Tender";
    final int SOURCE_ID = 6;

    public void run() {
        logger.info("Starting Sesami STE Crawler");

        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

        DateFormat startDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        DateFormat closingDateFormat = new SimpleDateFormat("dd.MM.yyyy H:m");

        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());

        List<Tender> tenders = new ArrayList<>();

        try {
            HtmlPage page = webClient.getPage(URL);
            webClient.waitForBackgroundJavaScript(1000);

            HtmlTableCell headerCell = page.getFirstByXPath("//td[@class='TableHeader2']");
            HtmlTable table = (HtmlTable) headerCell.getParentNode().getParentNode().getParentNode();

            int rowCount = 0;
            int colCount = 0;
            for (HtmlTableRow row : table.getRows()) {
                try {
                    rowCount++;
                    // Skip header row
                    if (rowCount == 1) {
                        continue;
                    }

                    if (row.getCells().size() == 6) {
                        Tender tender = new Tender();
                        tender.setTenderSource(SOURCE_ID);
                        tender.setStatus(TenderStatus.OPEN);

                        for (HtmlTableCell cell : row.getCells()) {
                            colCount++;

                            switch (colCount) {
                                case 1:
                                    tender.setCompanyName(cell.asText());
                                    break;
                                case 2:
                                    tender.setReferenceNo(cell.asText());
                                    break;
                                case 3:
                                    tender.setTitle(cell.asText());
                                    break;
                                case 4:
                                    String startingDate = cell.asText();
                                    tender.setPublishedDate(startDateFormat.parse(startingDate));
                                    break;
                                case 5:
                                    String closingDate = cell.asText();
                                    tender.setClosingDate(closingDateFormat.parse(closingDate));
                                    break;
                                case 6:
                                    HtmlAnchor link = (HtmlAnchor) cell.getElementsByTagName("a").get(0);
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

        logger.info("Ending Sesami STE Crawler");
    }
}
