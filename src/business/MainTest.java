package business;

public class MainTest {
    public static void main(String[] args) {
        UserBUS userBUS = new UserBUS();
        String hash = userBUS.hashPassword("123456");
        System.out.println(hash);
    }
}
