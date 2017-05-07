package com.vladimirkush.geoaction.Models;


import com.vladimirkush.geoaction.Utils.Constants;

public class LBReminder extends LBAction {
    private String title;

    public LBReminder() {
        setActionType(ActionType.REMINDER);
        setStatus(Status.ACTIVE);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
