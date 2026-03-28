package entity;

public class Food {

    private int foodId;
    private String name;
    private double price;
    private int stock;
    private String description;

    public Food(){}

    public Food(String name,double price,int stock,String description){
        this.name=name;
        this.price=price;
        this.stock=stock;
        this.description=description;
    }

    public String getName(){
        return name;
    }

    public double getPrice(){
        return price;
    }

    public int getStock(){
        return stock;
    }

    public String getDescription(){
        return description;
    }
}