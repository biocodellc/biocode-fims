package auth;

import bcid.Database;
import bcid.ProjectMinter;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.apache.commons.cli.*;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.StringGenerator;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Used for all authentication duties such as login, changing passwords, creating users, resetting passwords, etc.
 */
public class Authenticator {
    private Database db;
    protected Connection conn;
    SettingsManager sm;
    private static LDAPAuthentication ldapAuthentication;
    private static Logger logger = LoggerFactory.getLogger(Authenticator.class);

    /**
     * Constructor that initializes the class level variables
     */
    public Authenticator() {

        // Initialize Database
        this.db = new Database();
        this.conn = db.getConn();

        // Initialize settings manager
        sm = SettingsManager.getInstance();
    }

    public static LDAPAuthentication getLdapAuthentication() {
        return ldapAuthentication;
    }

    /**
     * Process 2-factor login as LDAP first and then entrust QA
     *
     * @param username
     * @param password
     * @param recognizeDemo
     *
     * @return
     */
    public String[] loginLDAP(String username, String password, Boolean recognizeDemo) {
        ldapAuthentication = new LDAPAuthentication(username, password, recognizeDemo);

        // If ldap authentication is successful, then retrieve the challange questions from the entrust server
        if (ldapAuthentication.getStatus() == ldapAuthentication.SUCCESS) {
            EntrustIGAuthentication igAuthentication = new EntrustIGAuthentication();
            // get the challenge questions from entrust IG server
            String [] challengeQuestions = igAuthentication.getGenericChallenge(username);

            // challengeQuestions should never return null from here since the ldap authentication was successful.
            // However entrust IG server didn't provide any challenge questions, so throw an exception.
            if (challengeQuestions == null || challengeQuestions.length == 0) {
                throw new ServerErrorException("Server Error.", "No challenge questions provided");
            }

            return challengeQuestions;
        } else {
            // return null if the ldap authentication failed
            return null;
        }
    }

    /**
     * respond to a challenge from the Entrust Identity Guard Server
     * @param username
     * @param challengeResponse
     * @return
     */
    public boolean entrustChallenge(String username, String[] challengeResponse) {
        EntrustIGAuthentication igAuthentication = new EntrustIGAuthentication();
        // verify the user's responses to the challenge questions
        boolean isAuthenticated = igAuthentication.authenticateGenericChallange(username, challengeResponse);

        if (isAuthenticated) {
            if (!validUser(username)) {
                // If authentication is good and user doesn't exist in Bcid db, then insert account into Database
                createLdapUser(username);

                // enable this user for all projects
                ProjectMinter p = new ProjectMinter();
                // get the userId for this username
                int userId = getUserId(username);
                // Loop projects and assign user to them
                ArrayList<Integer> projects = p.getAllProjects();
                Iterator projectsIt = projects.iterator();
                while (projectsIt.hasNext()) {
                    p.addUserToProject(userId, (Integer) projectsIt.next());
                }
                p.close();
            }
            return true;
        }

        return false;
    }

    /**
     * Public method to verify a users password
     *
     * @return
     */
    public Boolean login(String username, String password) {

        String hashedPass = getHashedPass(username);

        if (!hashedPass.isEmpty()) {
            try {
                return PasswordHash.validatePassword(password, hashedPass);
            } catch (InvalidKeySpecException e) {
                throw new ServerErrorException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new ServerErrorException(e);
            }
        }

        return false;
    }

    /**
     * retrieve the user's hashed password from the db
     *
     * @return
     */
    private String getHashedPass(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String selectString = "SELECT password FROM users WHERE username = ?";
            //System.out.println(selectString + " " + username);
            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, username);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password");
            }

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
        return null;
    }

    /**
     * retrieve the user's hashed password from the db
     *
     * @return
     */
    private boolean validUser(String username) {
        int count = 0;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String selectString = "SELECT userId id FROM users WHERE username = ?";
            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, username);
            rs = stmt.executeQuery();
            if (rs.next()) {
                rs.getInt("id");
                count++;
            }

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        }
        if (count == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Takes a new password for a user and stores a hashed version
     *
     * @param password
     *
     * @return true upon successful update, false when nothing was updated (most likely due to user not being found)
     */
    public Boolean setHashedPass(String username, String password) {
        PreparedStatement stmt = null;

        String hashedPass = createHash(password);

        // Store the hashed password in the db
        try {
            String updateString = "UPDATE users SET password = ? WHERE username = ?";
            stmt = conn.prepareStatement(updateString);

            stmt.setString(1, hashedPass);
            stmt.setString(2, username);
            Integer result = stmt.executeUpdate();

            if (result == 1) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, null);
        }
    }

    /**
     * Update the user's password associated with the given token.
     *
     * @param token
     * @param password
     *
     * @return
     */
    public Boolean resetPass(String token, String password) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String username = null;
            String sql = "SELECT username FROM users where passwordResetToken = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, token);
            rs = stmt.executeQuery();

            if (rs.next()) {
                username = rs.getString("username");
            }
            if (username != null) {
                db.close(stmt, null);
                String updateSql = "UPDATE users SET passwordResetToken = null, passwordResetExpiration = null WHERE username = \"" + username + "\"";
                stmt = conn.prepareStatement(updateSql);
                stmt.executeUpdate();

                return setHashedPass(username, password);
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error resetting password.", e);
        } finally {
            db.close(stmt, rs);
        }
        return false;
    }

    /**
     * create a hash of a password string to be stored in the db
     *
     * @param password
     *
     * @return
     */
    public String createHash(String password) {
        try {
            return PasswordHash.createHash(password);
        } catch (NoSuchAlgorithmException e) {
            throw new ServerErrorException(e);
        } catch (InvalidKeySpecException e) {
            throw new ServerErrorException(e);
        }
    }

    /**
     * create a user given a username and password
     *
     * @param username
     *
     * @return
     */
    public Boolean createLdapUser(String username) {
        PreparedStatement stmt = null;

        try {

            String insertString = "INSERT INTO users (username,hasSetPassword,institution,email,firstName,lastName,passwordResetToken,password,admin)" +
                    " VALUES(?,?,?,?,?,?,?,?,?,?)";
            stmt = conn.prepareStatement(insertString);

            stmt.setString(1, username);
            stmt.setInt(2, 1);
            stmt.setString(3, "Smithsonian Institution");
            stmt.setString(4, "");
            stmt.setString(5, "");
            stmt.setString(6, "");
            stmt.setString(7, "");
            stmt.setString(8, "");
            stmt.setInt(9, 0);

            stmt.execute();
            return true;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, null);
        }
    }


    /**
     * return the userId given a username
     *
     * @param username
     *
     * @return
     */
    private Integer getUserId(String username) {
        Integer userId = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String selectString = "SELECT userId FROM users WHERE username=?";
            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, LDAPAuthentication.showShortUserName(username));

            rs = stmt.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("userId");
            }

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
        return userId;
    }

    /**
     * create a user given a username and password
     *
     * @param userInfo
     *
     * @return
     */
    public void createUser(Hashtable<String, String> userInfo) {
        PreparedStatement stmt = null;
        String hashedPass = createHash(userInfo.get("password"));

        try {
            String insertString = "INSERT INTO users (username, password, email, firstName, lastName, institution)" +
                    " VALUES(?,?,?,?,?,?)";
            stmt = conn.prepareStatement(insertString);

            stmt.setString(1, userInfo.get("username"));
            stmt.setString(2, hashedPass);
            stmt.setString(3, userInfo.get("email"));
            stmt.setString(4, userInfo.get("firstName"));
            stmt.setString(5, userInfo.get("lastName"));
            stmt.setString(6, userInfo.get("institution"));

            stmt.execute();
            return;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, null);
        }
    }

    /**
     * Check if the user has set their own password or if they are using a temporary password
     *
     * @return
     */
    public Boolean userSetPass(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Boolean hasSetPassword = false;
        try {
            String selectString = "SELECT hasSetPassword FROM users WHERE username = ?";
            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, username);

            rs = stmt.executeQuery();

            if (rs.next()) {
                hasSetPassword = rs.getBoolean("hasSetPassword");
            }
        } catch (SQLException e) {
            logger.warn("SQLException thrown", e);
        } finally {
            db.close(stmt, rs);
        }
        return hasSetPassword;
    }

    /**
     * In the case where a user has forgotten their password, generate a token that can be used to create a new
     * password.
     *
     * @param username
     *
     * @return JSONObject containing the user's email and the resetToken
     */
    public JSONObject generateResetToken(String username) {
        String email = null;
        JSONObject resetToken = new JSONObject();
        String sql = "SELECT email FROM users WHERE username = ?";

        PreparedStatement stmt = null;
        PreparedStatement stmt2 = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, username);
            rs = stmt.executeQuery();

            if (rs.next()) {
                email = rs.getString("email");
            }

            if (email == null) {
                throw new BadRequestException("User not found", "email is null for user: " + username);
            }
            StringGenerator sg = new StringGenerator();
            String token = sg.generateString(20);
            // set for 24hrs in future
            Timestamp ts = new Timestamp(Calendar.getInstance().getTime().getTime() + (1000 * 60 * 60 * 24));

            String updateSql = "UPDATE users SET " +
                    "passwordResetToken = \"" + token + "\", " +
                    "passwordResetExpiration = \"" + ts + "\" " +
                    "WHERE username = ?";
            stmt2 = conn.prepareStatement(updateSql);

            stmt2.setString(1, username);

            stmt2.executeUpdate();

            resetToken.put("email", email);
            resetToken.put("resetToken", token);

            return resetToken;
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error while generating reset token.", "db error retrieving email for user "
                    + username, e);
        } finally {
            db.close(stmt, rs);
            db.close(stmt2, null);
        }
    }

    /**
     * This will update a given users password. Better to use the web interface
     *
     * @param args username and password
     */
    public static void main(String args[]) {




        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        CommandLine cl;

        Options options = new Options();
        options.addOption("U", "username", true, "Username you would like to set a password for");
        options.addOption("P", "password", true, "The temporary password you would like to set");
        options.addOption("ldap", false, "Use LDAP to set username");




        try {
            cl = clp.parse(options, args);
        } catch (UnrecognizedOptionException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        } catch (ParseException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        if (!cl.hasOption("U") || (!cl.hasOption("P") && cl.hasOption("ldap"))) {
            System.out.println("You must enter a username and a password");
            return;
        }

        String username = cl.getOptionValue("U");
        String password = cl.getOptionValue("P");

        Authenticator authenticator = new Authenticator();

        // LDAP option
        if (cl.hasOption("ldap")) {
            System.out.println("authenticating using LDAP");
            String[] challengeQuestions = authenticator.loginLDAP(username, password, true);
            if (challengeQuestions == null) {
                System.out.println("Error logging in using LDAP");
            }
            return;
        }

        Boolean success = authenticator.setHashedPass(username, password);

        if (!success) {
            System.out.println("Error updating password for " + username);
            return;
        }

        // change hasSetPassword field to 0 so user has to create new password next time they login
        Statement stmt = null;
        try {
            stmt = authenticator.conn.createStatement();
            Integer result = stmt.executeUpdate("UPDATE users SET hasSetPassword=\"0\" WHERE username=\"" + username + "\"");

            if (result == 0) {
                System.out.println("Error updating hasSetPassword value to 0 for " + username);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            authenticator.close();
        }

        System.out.println("Successfully set new password for " + username);

    }

    public void close() {
        db.close();
    }
}


