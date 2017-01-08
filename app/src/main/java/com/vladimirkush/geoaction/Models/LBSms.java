package com.vladimirkush.geoaction.Models;


import java.util.List;

public class LBSms extends LBAction {
    private List<String> to;
    private String message;

    public LBSms() {
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
