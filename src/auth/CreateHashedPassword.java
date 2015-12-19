package auth;

/**
 * A quick and dirty class to create a password hash for inserting into the Database
 */
public class CreateHashedPassword {
    public static void main(String[] args) {
        String password = "demo";
        Authenticator authenticator = new Authenticator();
        System.out.println(authenticator.createHash(password));
        authenticator.close();
    }
}
