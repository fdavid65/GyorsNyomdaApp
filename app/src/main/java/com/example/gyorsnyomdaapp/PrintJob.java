package com.example.gyorsnyomdaapp;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class PrintJob {
    private String documentId;
    private String userId;
    private String fileName;
    private String fileUrl;
    private String status;
    private @ServerTimestamp Date timestamp;
    private int copies;
    private String colorMode;
    private String paperSize;
    private String paperType;
    private String notes;

    // Státusz konstansok
    public static final String STATUS_UPLOADED = "Feltöltve";
    public static final String STATUS_PRINT_ORDERED = "Nyomtatásra leadva";

    // Színmód konstansok
    public static final String COLOR_MODE_BW = "Fekete-fehér";
    public static final String COLOR_MODE_COLOR = "Színes";

    // Papírtípus konstansok
    public static final String PAPER_TYPE_SOFT = "Puha";
    public static final String PAPER_TYPE_HARD = "Kemény";

    // Papírméretek
    public static final String[] PAPER_SIZES = {"A6", "A5", "A4", "A3", "A2", "A1", "A0"};


    public PrintJob() {}

    public PrintJob(String userId, String fileName, String fileUrl, String status, int copies,
                    String colorMode, String paperSize, String paperType, String notes) {
        this.userId = userId;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.status = status;
        this.copies = copies;
        this.colorMode = colorMode;
        this.paperSize = paperSize;
        this.paperType = paperType;
        this.notes = notes;
    }

    // Getters
    public String getDocumentId() { return documentId; }
    public String getUserId() { return userId; }
    public String getFileName() { return fileName; }
    public String getFileUrl() { return fileUrl; }
    public String getStatus() { return status; }
    public Date getTimestamp() { return timestamp; }
    public int getCopies() { return copies; }
    public String getColorMode() { return colorMode; }
    public String getPaperSize() { return paperSize; }
    public String getPaperType() { return paperType; }
    public String getNotes() { return notes; }

    // Setters
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setStatus(String status) { this.status = status; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public void setCopies(int copies) { this.copies = copies; }
    public void setColorMode(String colorMode) { this.colorMode = colorMode; }
    public void setPaperSize(String paperSize) { this.paperSize = paperSize; }
    public void setPaperType(String paperType) { this.paperType = paperType; }
    public void setNotes(String notes) { this.notes = notes; }
}