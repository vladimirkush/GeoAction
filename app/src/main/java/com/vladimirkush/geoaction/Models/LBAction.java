package com.vladimirkush.geoaction.Models;


import com.google.android.gms.maps.model.LatLng;

public abstract class LBAction {
    public enum DirectionTrigger {ENTER, EXIT};
    public enum Status {ACTIVE, PAUSED};
    public enum ActionType{REMINDER, SMS, EMAIL};

    private ActionType actionType;
    private long ID;
    private int radius;
    private DirectionTrigger directionTrigger;
    private LatLng triggerCenter;
    private Status status;




    public LatLng getTriggerCenter() {
        return triggerCenter;
    }

    public void setTriggerCenter(LatLng triggerCenter) {
        this.triggerCenter = triggerCenter;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public long getID() {
        return ID;
    }

    public void setID(long ID) {
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setDirectionTrigger(DirectionTrigger directionTrigger) {
        this.directionTrigger = directionTrigger;
    }
}
