package com.chlorocode.tendercrawler.crawler;

import com.chlorocode.tendercrawler.Util;
import com.chlorocode.tendercrawler.model.Tender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public abstract class Crawler implements Runnable {

    final static Logger logger = LoggerFactory.getLogger(Crawler.class);
    protected int maxBulkSaveSize;
    private String username;
    private String password;

    public Crawler() {
        try {
            maxBulkSaveSize = Integer.parseInt(Util.getConfigValue("bulk_save_size"));
            username = Util.getConfigValue("username");
            password = Util.getConfigValue("password");
        } catch (IOException ex) {
            maxBulkSaveSize = 5;
        }
    }

    public abstract void run();

    public void updateToDatabase(List<Tender> tenders) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(tenders);

        String url = Util.getConfigValue("api_url");
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // Set request header
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; charset=utf8");

        // Security authentication
        String encoded = Base64.getEncoder().encodeToString((username+":"+password).getBytes(StandardCharsets.UTF_8));
        con.setRequestProperty("Authorization", "Basic "+encoded);

        logger.info("Sending 'POST' request to URL : " + url);

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(wr, "UTF-8"));
        writer.write(json);
        writer.flush();
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
            logger.error("Request content: " + json);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getErrorStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            logger.error("Response returned: " + response.toString());
        }
    }
}
