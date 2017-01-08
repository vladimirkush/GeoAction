package com.vladimirkush.geoaction.Models;


import com.vladimirkush.geoaction.Utils.Constants;

public class LBReminder extends LBAction {
    private String title;
    private String message;

    public LBReminder() {
        setActionType(Constants.ActionType.REMINDER);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
