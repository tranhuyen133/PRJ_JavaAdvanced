package entity;

public class PC {

    private int pcId;
    private String pcName;
    private String status;
    private double pricePerHour;

    public PC(){}

    public PC(String pcName,String status,double pricePerHour){
        this.pcName=pcName;
        this.status=status;
        this.pricePerHour=pricePerHour;
    }

    public String getPcName(){
        return pcName;
    }

    public String getStatus(){
        return status;
    }

    public double getPricePerHour(){
        return pricePerHour;
    }
    public int getPcId(){
        return pcId;
    }
    public void setPcName(String pcName){
        this.pcName = pcName;
    }

    public void setStatus(String status){
        this.status = status;
    }

    public void setPricePerHour(double pricePerHour){
        this.pricePerHour = pricePerHour;
    }
}