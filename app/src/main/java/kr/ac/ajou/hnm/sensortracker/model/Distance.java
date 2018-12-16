package kr.ac.ajou.hnm.sensortracker.model;

public class Distance {
    public int id;
    public int distance;
    public int seq;

    public Distance(int seq, int id, int distance) {
        this.seq = seq;
        this.id = id;
        this.distance = distance;
    }
}
