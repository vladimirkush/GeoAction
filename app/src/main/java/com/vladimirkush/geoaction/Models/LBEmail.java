package com.vladimirkush.geoaction.Models;


import java.util.List;

public class LBEmail extends LBAction {
    private List<String> to;
    private String subject;
    private String message;

    public LBEmail() {
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
