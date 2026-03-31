package entity;

public class PC {

    private int pcId;
    private String name;
    private String area;
    private String config;
    private String status;
    private double pricePerHour;

    public PC(){}

    public PC(String name, String status, double pricePerHour){
        this.name = name;
        this.area = "Khu A";
        this.config = "Standard";
        this.status = status;
        this.pricePerHour = pricePerHour;
    }

    public PC(String name, String area, String config, String status, double pricePerHour){
        this.name = name;
        this.area = area;
        this.config = config;
        this.status = status;
        this.pricePerHour = pricePerHour;
    }

    public int getPcId(){
        return pcId;
    }

    public String getName(){
        return name;
    }

    public String getArea(){
        return area;
    }

    public String getConfig(){
        return config;
    }

    public String getStatus(){
        return status;
    }

    public double getPricePerHour(){
        return pricePerHour;
    }

    public void setPcId(int pcId){
        this.pcId = pcId;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setArea(String area){
        this.area = area;
    }

    public void setConfig(String config){
        this.config = config;
    }

    public void setStatus(String status){
        this.status = status;
    }

    public void setPricePerHour(double pricePerHour){
        this.pricePerHour = pricePerHour;
    }
}