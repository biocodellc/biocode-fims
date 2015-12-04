package auth;

import bcid.database;
import bcidExceptions.ServerErrorException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.SettingsManager;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.security.GeneralSecurityException;
import java.sql.*;


/**
 * Authenticate names using LDAP
 */
public class LDAPAuthentication {

    public static int SUCCESS = 0;
    public static int ERROR = 1;
    public static int INVALID_CREDENTIALS = 2;

    private BindResult bindResult = null;
    private int status;

    private static Logger logger = LoggerFactory.getLogger(LDAPAuthentication.class);

    static SettingsManager sm;
    @Context
    static ServletContext context;
    static String ldapURI;
    static String defaultLdapDomain;

    private String shortUsername;
    private String longUsername;

    database db;
    protected Connection conn;

    /**
     * Load settings manager
     */
    static {
        // Initialize settings manager
        sm = SettingsManager.getInstance();
        sm.loadProperties();
        // Get the LDAP servers from property file
        // Property file format looks like "ldapServers = my    secureLDAPserver.net:636,myfailoverLDAPServer.net:636"
        ldapURI = sm.retrieveValue("ldapServers");
        defaultLdapDomain = sm.retrieveValue("defaultLdapDomain");
    }

    public static String showShortUserName(String username) {
            return  username.split("@")[0];
    }
    public static String showLongUsername(String username) {
            defaultLdapDomain = sm.retrieveValue("defaultLdapDomain");
          return username.split("@")[0] + "@" + defaultLdapDomain;
    }
    public LDAPAuthentication() {
        db = new database();
        conn = db.getConn();
    }

    /**
     * check if there is a ldapNonce for the given user. If so, return the number of login attempts
     * @param username
     * @return
     */
    public Integer getLoginAttempts(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Integer ldapTimeout = Integer.parseInt(sm.retrieveValue("ldapLockedAccountTimeout"));

        try {
            // only retrieve the row if the nonce isn't expired
            String selectString = "SELECT current_timestamp() as current, attempts, ts FROM ldapNonces WHERE username = " +
                    "? && ts > (NOW() - INTERVAL " + ldapTimeout + " MINUTE)";
            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, showLongUsername(username));

            rs = stmt.executeQuery();

            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("ts");
                // Get the current time from the database (in case the application server is in a different timezone)
                Timestamp currentTs = rs.getTimestamp("current");
                // convert minutes to miliseconds
                Timestamp expiredTs = new Timestamp(currentTs.getTime() - (ldapTimeout * 60 * 1000));

                // if nonce isn't expired, then return the number of attempts
                if (ts != null && ts.after(expiredTs)) {
                    return rs.getInt("attempts");
                }
            }
        } catch (SQLException e) {
            // silence the exception. This feature isn't necessary for ldap authentication, just a nice warning for the user.
            // if an exception is thrown, allow the user to attempt a login anyways
            logger.warn(null, e);
        } finally {
            db.close(stmt, rs);
        }
        return 0;
    }

    /**
     * delete any ldapNonce rows that are expired or for the logged in user
     */
    private void deleteExpiredNonces() {
        Integer ldapTimeout = Integer.parseInt(sm.retrieveValue("ldapLockedAccountTimeout"));
        PreparedStatement stmt = null;

        try {
            String sql = "DELETE FROM ldapNonces WHERE ts < (NOW() - INTERVAL ? MINUTE) OR username = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, ldapTimeout);
            stmt.setString(2, longUsername);

            stmt.execute();
        } catch (SQLException e) {
            logger.warn(null, e);
        } finally {
            db.close(stmt, null);
        }
    }

    private void updateNonce() {
        PreparedStatement stmt = null;
        Integer numLdapAttemptsAllowed = Integer.parseInt(sm.retrieveValue("ldapAttempts"));

        try {
            String insertString = "INSERT INTO ldapNonces SET username = ? ON DUPLICATE KEY UPDATE " +
                    "attempts = attempts + 1, ts = CASE WHEN attempts = ? THEN NOW() ELSE ts END";
            stmt = conn.prepareStatement(insertString);

            stmt.setString(1, longUsername);
            stmt.setInt(2, numLdapAttemptsAllowed);

            stmt.execute();
        } catch (SQLException e) {
            // silence the exception since the nonce is only used as a warning feature for users, not mandatory for
            // ldap login
            logger.warn(null, e);
        } finally {
            db.close(stmt, null);
        }

    }

    /**
     * Authenticate a user and password using LDAP
     *
     * @param username
     * @param password
     *
     * @return
     */
    public LDAPAuthentication(String username, String password, Boolean recognizeDemo) {

        db = new database();
        conn = db.getConn();
        
        // strip any domain extension that the user provided (we DON't want to store this)
        shortUsername = showShortUserName(username);
        longUsername = showLongUsername(username);

        if (recognizeDemo && shortUsername.equalsIgnoreCase("demo")) {
            status = SUCCESS;
            return;
        }
        // Create the connection at beginning... must be closed at finally
        LDAPConnection connection = null;

        // Set default status in case it is not specifically set.  This should be an error
        status = ERROR;

        // Creating an array of available servers
        String[] ldapServers = ldapURI.split(",");
        String[] serverAddresses = new String[ldapServers.length];
        int[] serverPorts = new int[ldapServers.length];
        for (int i = 0; i < ldapServers.length; i++) {
            serverAddresses[i] = ldapServers[i].split(":")[0];
            serverPorts[i] = (Integer.valueOf(ldapServers[i].split(":")[1]));
        }
        try {
            SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
            SSLSocketFactory socketFactory = sslUtil.createSSLSocketFactory();

            // Failoverset lets us query multiple servers looking for connection
            FailoverServerSet failoverSet = new FailoverServerSet(serverAddresses, serverPorts, socketFactory);
            // TODO: time this out quicker... Takes a LONG time if answer is no
            logger.info("initiating connection for " + longUsername);
            connection = failoverSet.getConnection();
            BindRequest bindRequest = new SimpleBindRequest(longUsername, password);

            try {
                bindResult = connection.bind(bindRequest);
            } catch (LDAPException e2) {
                // don't throw any exception if we fail here, this is just a non-passed attempt.
                logger.info("Failed LDAPAuthentication attempt", e2);
                connection.close();
                status = INVALID_CREDENTIALS;

                // update the nonce
                updateNonce();
            }
        } catch (LDAPException e) {
            throw new ServerErrorException("Problem with LDAP connection.  It is likely we cannot connect to LDAP server", e);
        } catch (GeneralSecurityException e) {
            throw new ServerErrorException(e);
        } finally {
            if (connection != null) connection.close();
        }
        if (bindResult != null && bindResult.getResultCode() == ResultCode.SUCCESS) {
            status = SUCCESS;
            deleteExpiredNonces();
        }
    }

    /**
     * Return a status message.  See constants as part of this class
     *
     * @return
     */
    public int getStatus() {
        return status;
    }

    public static void main(String[] args) throws Exception {

        //return sbEmail.toString();

        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        CommandLine cl;

        Options options = new Options();
        options.addOption("U", "username", true, "fully qualified username");
        options.addOption("P", "password", true, "the password");

        try {
            cl = clp.parse(options, args);
        } catch (UnrecognizedOptionException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        } catch (ParseException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        if (!cl.hasOption("U") || !cl.hasOption("P")) {
            System.out.println("You must enter a username and a password");
            return;
        }

        String username = cl.getOptionValue("U");
        String password = cl.getOptionValue("P");

        LDAPAuthentication t = new LDAPAuthentication(username, password, true);

        if (t.getStatus() == t.SUCCESS) {
            System.out.println("Passed!");
        } else if (t.getStatus() == t.INVALID_CREDENTIALS) {
            System.out.println("Invalid username or password, or expired account");
        } else {
            System.out.println("LDAP Error: ");
        }
    }


}
