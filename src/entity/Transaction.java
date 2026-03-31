package entity;

import java.sql.Timestamp;

public class Transaction {

    private int transactionId;
    private int userId;
    private String type; // DEPOSIT, WITHDRAW, PC_USAGE, FOOD_ORDER
    private double amount;
    private String description;
    private Timestamp createdAt;

    public Transaction(){}

    public Transaction(int userId, String type, double amount, String description){
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.description = description;
    }

    // Getters and Setters
    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

