package com.vladimirkush.geoaction.Models;


import android.text.TextUtils;

import com.vladimirkush.geoaction.Utils.Constants;

import java.util.List;

public class LBSms extends LBAction {
    private List<String> to;
    private String message;

    public LBSms() {
        setActionType(ActionType.SMS);
        setStatus(Status.ACTIVE);
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

    public String getToAsSingleString(){
        StringBuilder listString = new StringBuilder();
        int i;
        for (i =0; i<to.size()-1; i++){
            listString.append(to.get(i)+", ");
        }
        listString.append(to.get(i));
        return listString.toString();

    }
}
