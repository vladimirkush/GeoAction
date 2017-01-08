package com.vladimirkush.geoaction.Models;


import com.vladimirkush.geoaction.Utils.Constants;

import java.util.List;

public class LBEmail extends LBAction {
    private List<String> to;
    private String subject;
    private String message;

    public LBEmail() {
        setActionType(Constants.ActionType.EMAIL);
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
