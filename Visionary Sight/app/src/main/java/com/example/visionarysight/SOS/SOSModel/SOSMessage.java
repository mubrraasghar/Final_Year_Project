package com.example.visionarysight.SOS.SOSModel;

import com.google.firebase.firestore.DocumentId;

public class SOSMessage {

    @DocumentId
    private String documentId;

    private String content;
    private String timestamp;

    public SOSMessage() {}

    public SOSMessage(String content, String timestamp) {
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}
