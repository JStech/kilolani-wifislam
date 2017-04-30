package net.stechschulte.kilolani;

import java.util.HashMap;

/**
 * Created by john on 4/24/17.
 */

public class Position {
    private double latitude;
    private double longitude;
    private float accuracy;
    private long time;
    private HashMap<String, Integer> wifiObservations;

    Position(double lat, double lon, float acc, long time) {
        this.latitude = lat;
        this.longitude = lon;
        this.accuracy = acc;
        this.time = time;
        this.wifiObservations = new HashMap<String, Integer>();
    }

    public void addObservation(String id, Integer rssi) {
        wifiObservations.put(id, rssi);
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public long getTime() {
        return time;
    }

    public String toHashString() {
        return String.format("%f %f %ld", latitude, longitude, time);
    }
    
    public HashMap<String, Integer> getWifiObservations() {
        return wifiObservations;
    }
}
