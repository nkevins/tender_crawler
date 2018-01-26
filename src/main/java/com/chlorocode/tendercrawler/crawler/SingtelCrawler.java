package com.chlorocode.tendercrawler.crawler;

import com.chlorocode.tendercrawler.constant.TenderStatus;
import com.chlorocode.tendercrawler.model.Tender;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Crawler specific implementation for Singtel.
 */
public class SingtelCrawler extends Crawler {

    final static Logger logger = LoggerFactory.getLogger(SingtelCrawler.class);
    final String URL = "https://mybusiness.singtel.com/trading-board";
    final int SOURCE_ID = 5;

    public void run() {
        logger.info("Starting Singtel Crawler");

        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

        WebClient webClient = new WebClient();
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
                HtmlAnchor nextPageLink = null;
                if(page.getFirstByXPath("//li[@class='pager-next odd']") == null) {
                    nextPageAvailable = false;
                } else {
                    HtmlListItem listItem = page.getFirstByXPath("//li[@class='pager-next odd']");
                    nextPageLink = (HtmlAnchor) listItem.getFirstChild();
                }

                HtmlTable table = (HtmlTable)page.getByXPath("//table[@class='views-table cols-6']").get(0);
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
                            // Filter only type Buying
                            HtmlAnchor detailPageLink = (HtmlAnchor) row.getCell(2).getElementsByTagName("a").get(0);
                            String detailUrl = "https://mybusiness.singtel.com" + detailPageLink.getHrefAttribute();

                            WebClient webClient2 = new WebClient();
                            webClient2.getOptions().setJavaScriptEnabled(true);
                            webClient2.getOptions().setThrowExceptionOnScriptError(false);
                            webClient2.getOptions().setCssEnabled(false);
                            webClient2.setAjaxController(new NicelyResynchronizingAjaxController());

                            HtmlPage detailPage = webClient2.getPage(detailUrl);
                            HtmlDivision rightDiv = (HtmlDivision) detailPage.getElementById("td-content-right");
                            HtmlDivision divRow = (HtmlDivision) rightDiv.getElementsByAttribute("div", "id", "type").get(0);
                            String type = divRow.asText();
                            if (!type.contains("Buying")) {
                                continue;
                            }

                            Tender tender = new Tender();
                            tender.setTenderSource(SOURCE_ID);
                            tender.setStatus(TenderStatus.OPEN);

                            divRow = (HtmlDivision) rightDiv.getElementsByAttribute("div", "id", "type").get(2);
                            HtmlSpan closingDateSpan = (HtmlSpan) divRow.getElementsByAttribute("span", "class", "mtext").get(0);
                            String closingDateText = closingDateSpan.asText();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
                            try {
                                tender.setClosingDate(dateFormat.parse(closingDateText));
                            } catch (Exception ex) {
                                logger.error("Exception when parsing Closing Date", ex);
                            }

                            for (HtmlTableCell cell : row.getCells()) {
                                colCount++;

                                switch (colCount) {
                                    case 2:
                                        tender.setCompanyName(cell.asText());
                                        break;
                                    case 3:
                                        tender.setTitle(cell.asText());
                                        tender.setTenderURL(detailUrl);
                                        break;
                                    case 4:
                                        tender.setPublishedDate(parsePublishedDate(cell.asText()));
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

                if (nextPageAvailable) {
                    pageNo++;
                    logger.info("Going to next page: " + pageNo);
                    nextPageLink.click();
                    webClient.waitForBackgroundJavaScript(5000);
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

        logger.info("Ending Singtel Crawler");
    }

    private Date parsePublishedDate(String publishedDateTimeText) {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        String[] dateSplitPart = publishedDateTimeText.split("\\s+");
        if (dateSplitPart.length >= 2) {
            for (int i = 0; i < dateSplitPart.length -1; i += 2) {
                String datePart = dateSplitPart[i].trim() + " " + dateSplitPart[i + 1].trim();

                Pattern p = Pattern.compile("(\\d+)\\s+(.*?)s?");

                Map<String, Integer> fields = new HashMap<String, Integer>() {{
                    put("second", Calendar.SECOND);
                    put("min", Calendar.MINUTE);
                    put("hour",   Calendar.HOUR);
                    put("day",    Calendar.DATE);
                    put("week",   Calendar.WEEK_OF_YEAR);
                    put("month",  Calendar.MONTH);
                    put("year",   Calendar.YEAR);
                }};

                Matcher m = p.matcher(datePart);
                if (m.matches()) {
                    int amount = Integer.parseInt(m.group(1));
                    String unit = m.group(2);

                    cal.add(fields.get(unit), -amount);
                } else {
                    logger.error("Unable to parse Published Date: ", publishedDateTimeText);
                    return null;
                }
            }

            return cal.getTime();
        } else {
            logger.error("Unable to parse Published Date: ", publishedDateTimeText);
            return null;
        }
    }
}