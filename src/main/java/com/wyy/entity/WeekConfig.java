package com.wyy.entity;


import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class WeekConfig {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int id;
    private int year;
    private int weekNum;
    private Date startDate;
    private Date endDate;


    public int getId() {
        return id;
    }


    public void setId(int id) {
        this.id = id;
    }


    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getWeekNum() {
        return weekNum;
    }

    public void setWeekNum(int weekNum) {
        this.weekNum = weekNum;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return "weekConfig{" +
                "id=" + id +
                ", year=" + year +
                ", weekNum=" + weekNum +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}
