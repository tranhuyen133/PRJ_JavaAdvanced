package entity;

import java.sql.Timestamp;

public class Booking {
    
    private int bookingId;
    private int userId;
    private int pcId;
    private Timestamp bookingDate;
    private Timestamp startTime;
    private Timestamp endTime;
    private String status; // PENDING, CONFIRMED, COMPLETED, CANCELLED
    private double totalCost;
    
    public Booking() {}
    
    public Booking(int userId, int pcId, Timestamp startTime, Timestamp endTime) {
        this.userId = userId;
        this.pcId = pcId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "PENDING";
        this.totalCost = 0;
    }
    
    // Getters and Setters
    public int getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public int getPcId() {
        return pcId;
    }
    
    public void setPcId(int pcId) {
        this.pcId = pcId;
    }
    
    public Timestamp getBookingDate() {
        return bookingDate;
    }
    
    public void setBookingDate(Timestamp bookingDate) {
        this.bookingDate = bookingDate;
    }
    
    public Timestamp getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }
    
    public Timestamp getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public double getTotalCost() {
        return totalCost;
    }
    
    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }
}

