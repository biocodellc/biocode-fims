package auth;

import bcid.database;
import bcid.projectMinter;
import bcidExceptions.ServerErrorException;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.SettingsManager;
import util.sendEmail;
import util.stringGenerator;

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
public class authenticator {
    private database db;
    protected Connection conn;
    SettingsManager sm;
    private static LDAPAuthentication ldapAuthentication;
    private static Logger logger = LoggerFactory.getLogger(authenticator.class);

    /**
     * Constructor that initializes the class level variables
     */
    public authenticator() {

        // Initialize database
        this.db = new database();
        this.conn = db.getConn();

        // Initialize settings manager
        sm = SettingsManager.getInstance();
        sm.loadProperties();
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
                // If authentication is good and user doesn't exist in bcid db, then insert account into database
                createLdapUser(username);

                // enable this user for all projects
                projectMinter p = new projectMinter();
                // get the user_id for this username
                int user_id = getUserId(username);
                // Loop projects and assign user to them
                ArrayList<Integer> projects = p.getAllProjects();
                Iterator projectsIt = projects.iterator();
                while (projectsIt.hasNext()) {
                    p.addUserToProject(user_id, (Integer) projectsIt.next());
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
                return passwordHash.validatePassword(password, hashedPass);
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
            String selectString = "SELECT user_id id FROM users WHERE username = ?";
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
            String sql = "SELECT username FROM users where pass_reset_token = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, token);
            rs = stmt.executeQuery();

            if (rs.next()) {
                username = rs.getString("username");
            }
            if (username != null) {
                db.close(stmt, null);
                String updateSql = "UPDATE users SET pass_reset_token = null, pass_reset_expiration = null WHERE username = \"" + username + "\"";
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
            return passwordHash.createHash(password);
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

            String insertString = "INSERT INTO users (username,set_password,institution,email,firstName,lastName,pass_reset_token,password,admin,IDlimit)" +
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
            stmt.setInt(10, 100000);

            stmt.execute();
            return true;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, null);
        }
    }


    /**
     * return the user_id given a username
     *
     * @param username
     *
     * @return
     */
    private Integer getUserId(String username) {
        Integer user_id = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String selectString = "SELECT user_id FROM users WHERE username=?";
            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, LDAPAuthentication.showShortUserName(username));

            rs = stmt.executeQuery();
            if (rs.next()) {
                user_id = rs.getInt("user_id");
            }

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
        return user_id;
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
        try {
            String selectString = "SELECT set_password FROM users WHERE username = ?";
            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, username);

            rs = stmt.executeQuery();

            if (rs.next()) {
                Integer set_password = rs.getInt("set_password");
                if (set_password == 1) {
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.warn("SQLException thrown", e);
        } finally {
            db.close(stmt, rs);
        }
        return false;
    }

    /**
     * In the case where a user has forgotten their password, generate a token that can be used to create a new
     * password. This method will send to the user's registered email a link that can be used to change their password.
     *
     * @param username
     *
     * @return
     */
    public String sendResetToken(String username) {
        String email = null;
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

            if (email != null) {
                stringGenerator sg = new stringGenerator();
                String token = sg.generateString(20);
                // set for 24hrs in future
                Timestamp ts = new Timestamp(Calendar.getInstance().getTime().getTime() + (1000 * 60 * 60 * 24));

                String updateSql = "UPDATE users SET " +
                        "pass_reset_token = \"" + token + "\", " +
                        "pass_reset_expiration = \"" + ts + "\" " +
                        "WHERE username = ?";
                stmt2 = conn.prepareStatement(updateSql);

                stmt2.setString(1, username);

                stmt2.executeUpdate();

                // Reset token path
                String resetToken = sm.retrieveValue("resetToken") + token;

                String emailBody = "You requested a password reset for your BCID account.\n\n" +
                        "Use the following link within the next 24 hrs to reset your password.\n\n" +
                        resetToken + "\n\n" +
                        "Thanks";

                // Initialize settings manager
                SettingsManager sm = SettingsManager.getInstance();
                sm.loadProperties();

                // Send an Email that this completed
                sendEmail sendEmail = new sendEmail(
                        sm.retrieveValue("mailUser"),
                        sm.retrieveValue("mailPassword"),
                        sm.retrieveValue("mailFrom"),
                        email,
                        "Reset Password",
                        emailBody);
                sendEmail.start();
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error while sending reset token.", "db error retrieving email for user "
                    + username, e);
        } finally {
            db.close(stmt, rs);
            db.close(stmt2, null);
        }
        return email;
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

        authenticator authenticator = new authenticator();

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

        // change set_password field to 0 so user has to create new password next time they login
        Statement stmt = null;
        try {
            stmt = authenticator.conn.createStatement();
            Integer result = stmt.executeUpdate("UPDATE users SET set_password=\"0\" WHERE username=\"" + username + "\"");

            if (result == 0) {
                System.out.println("Error updating set_password value to 0 for " + username);
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


