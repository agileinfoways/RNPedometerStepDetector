package com.rnstepcounterdemo.steps.bean;

import com.litesuits.orm.db.annotation.Column;
import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.annotation.Table;
import com.litesuits.orm.db.enums.AssignType;

/**
 * Created by dylan on 2016/1/30.
 */
@Table("step")
public class StepData {

    // Specify auto increment, each object needs to have a primary key
    @PrimaryKey(AssignType.AUTO_INCREMENT)
    private int id;
    @Column("stepsDate")
    private String stepsDate;
    @Column("stepsCount")
    private String stepsCount;
    @Column("caloriesBurned")
    private String caloriesBurned;
    @Column("distanceWalked")
    private String distanceWalked;
    @Column("walkTime")
    private String walkTime;
    @Column("syncToServer")
    private boolean syncToServer ;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStepsDate() {
        return stepsDate;
    }

    public void setStepsDate(String stepsDate) {
        this.stepsDate = stepsDate;
    }

    public String getStepsCount() {
        return stepsCount;
    }

    public void setStepsCount(String stepsCount) {
        this.stepsCount = stepsCount;
    }

    public String getCaloriesBurned() {
        return caloriesBurned;
    }

    public void setCaloriesBurned(String caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
    }

    public String getDistanceWalked() {
        return distanceWalked;
    }

    public void setDistanceWalked(String distanceWalked) {
        this.distanceWalked = distanceWalked;
    }

    public String getWalkTime() {
        return walkTime;
    }

    public void setWalkTime(String walkTime) {
        this.walkTime = walkTime;
    }

    public boolean isSyncToServer() {
        return syncToServer;
    }

    public void setSyncToServer(boolean syncToServer) {
        this.syncToServer = syncToServer;
    }

    @Override
    public String toString() {
        return "StepData{" +
                "id=" + id +
                ", stepsDate='" + stepsDate + '\'' +
                ", stepsCount='" + stepsCount + '\'' +
                ", caloriesBurned='" + caloriesBurned + '\'' +
                ", distanceWalked='" + distanceWalked + '\'' +
                ", walkTime='" + walkTime + '\'' +
                ", syncToServer=" + syncToServer +
                '}';
    }
}
