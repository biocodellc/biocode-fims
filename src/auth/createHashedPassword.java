package auth;

/**
 * A quick and dirty class to create a password hash for inserting into the database
 */
public class createHashedPassword {
    public static void main(String[] args) {
        String password = "demo";
        authenticator authenticator = new authenticator();
        System.out.println(authenticator.createHash(password));
        authenticator.close();
    }
}
