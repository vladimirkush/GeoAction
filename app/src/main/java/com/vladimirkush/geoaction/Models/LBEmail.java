package com.vladimirkush.geoaction.Models;


import java.util.List;

public class LBEmail extends LBAction {
    private List<String> to;
    private String subject;

    public LBEmail() {
        setActionType(ActionType.EMAIL);
        setStatus(Status.ACTIVE);
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
