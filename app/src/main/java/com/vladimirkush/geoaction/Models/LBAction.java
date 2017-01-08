package com.vladimirkush.geoaction.Models;


import com.google.android.gms.maps.model.LatLng;
import com.vladimirkush.geoaction.Utils.Constants;

public abstract class LBAction {
    public enum DirectionTrigger {ENTER, EXIT};

    private Constants.ActionType actionType;
    private String ID;
    private int radius;
    private DirectionTrigger directionTrigger;
    private LatLng triggerCenter;




    public LatLng getTriggerCenter() {
        return triggerCenter;
    }

    public void setTriggerCenter(LatLng triggerCenter) {
        this.triggerCenter = triggerCenter;
    }

    public Constants.ActionType getActionType() {
        return actionType;
    }

    public void setActionType(Constants.ActionType actionType) {
        this.actionType = actionType;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public DirectionTrigger getDirectionTrigger() {
        return directionTrigger;
    }

    public void setDirectionTrigger(DirectionTrigger directionTrigger) {
        this.directionTrigger = directionTrigger;
    }
}
