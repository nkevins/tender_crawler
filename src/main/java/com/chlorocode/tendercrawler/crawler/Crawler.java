package com.chlorocode.tendercrawler.crawler;

import com.chlorocode.tendercrawler.Util;
import com.chlorocode.tendercrawler.model.Tender;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public abstract class Crawler implements Runnable {

    final static Logger logger = LoggerFactory.getLogger(Crawler.class);
    protected int maxBulkSaveSize;

    public Crawler() {
        try {
            maxBulkSaveSize = Integer.parseInt(Util.getConfigValue("bulk_save_size"));
        } catch (IOException ex) {
            maxBulkSaveSize = 5;
        }
    }

    public abstract void run();

    public void updateToDatabase(List<Tender> tenders) throws IOException {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        String json = gson.toJson(tenders);

        String url = Util.getConfigValue("api_url");
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // Set request header
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; charset=utf8");

        logger.info("Sending 'POST' request to URL : " + url);

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(json);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        logger.info("Response Code : " + responseCode);

        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            logger.info("Return success: " + response.toString());
        } else {
            logger.error("Error returned from API server");
        }
    }
}
