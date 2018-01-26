package com.chlorocode.tendercrawler.model;

import java.util.Date;

/**
 * This is entity class to represent the external tender.
 */
public class Tender {

    private String referenceNo;
    private String title;
    private String companyName;
    private Date publishedDate;
    private Date closingDate;
    private String status;
    private String tenderURL;
    private int tenderSource;

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Date getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(Date publishedDate) {
        this.publishedDate = publishedDate;
    }

    public Date getClosingDate() {
        return closingDate;
    }

    public void setClosingDate(Date closingDate) {
        this.closingDate = closingDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTenderURL() {
        return tenderURL;
    }

    public void setTenderURL(String tenderURL) {
        this.tenderURL = tenderURL;
    }

    public int getTenderSource() {
        return tenderSource;
    }

    public void setTenderSource(int tenderSource) {
        this.tenderSource = tenderSource;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TenderNo: " + referenceNo);
        sb.append("\n");
        sb.append("Status: " + status);
        sb.append("\n");
        sb.append("Title: " + title);
        sb.append("\n");
        sb.append("Company Name: " + companyName);
        sb.append("\n");
        sb.append("Published Date: " + publishedDate);
        sb.append("\n");
        sb.append("Closing Date: " + closingDate);
        sb.append("\n");
        sb.append("Tender URL: " + tenderURL);
        sb.append("\n");
        sb.append("Source: " + tenderSource);
        sb.append("\n");
        sb.append("========================");

        return sb.toString();
    }
}
