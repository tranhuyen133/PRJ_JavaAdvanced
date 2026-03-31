package entity;

public class User {

    private int userId;
    private String username;
    private String password;
    private String fullName;
    private String phone;
    private String role;


    public User(){}

    public User(String username, String password, String role){
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public User(String username, String password, String fullName, String phone, String role){
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
    }

    public int getUserId(){
        return userId;
    }

    public String getUsername(){
        return username;
    }

    public String getPassword(){
        return password;
    }

    public String getFullName(){
        return fullName;
    }

    public String getPhone(){
        return phone;
    }

    public String getRole(){
        return role;
    }

    public void setUserId(int userId){
        this.userId = userId;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public void setFullName(String fullName){
        this.fullName = fullName;
    }

    public void setPhone(String phone){
        this.phone = phone;
    }

    public void setRole(String role){
        this.role = role;
    }
}