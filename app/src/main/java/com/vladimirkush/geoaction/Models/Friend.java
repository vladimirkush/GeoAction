package com.vladimirkush.geoaction.Models;

import android.graphics.Bitmap;

public class Friend {
    public enum Status {TRACED, UNTRACED};

    private long    ID;
    private String  fbID;
    private String  name;
    private Status  status;
    private Bitmap  userIcon;
    private double  lat;
    private double  lon;
    private boolean isNear;



    private long lastNearTimeMillis;

    public Friend() {
        this.status = Status.TRACED;    //default status
        isNear = false;
    }

    public boolean isNear() {
        return isNear;
    }

    public void setNear(boolean near) {
        isNear = near;
    }

    public long getID() {
        return ID;
    }

    public void setID(long ID) {
        this.ID = ID;
    }

    public String getFbID() {
        return fbID;
    }

    public void setFbID(String fbID) {
        this.fbID = fbID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Bitmap getUserIcon() {
        return userIcon;
    }

    public void setUserIcon(Bitmap userIcon) {
        this.userIcon = userIcon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public long getLastNearTimeMillis() {
        return lastNearTimeMillis;
    }

    public void setLastNearTimeMillis(long lastNearTimeMillis) {
        this.lastNearTimeMillis = lastNearTimeMillis;
    }
}
