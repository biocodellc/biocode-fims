package auth.oauth2;

import bcid.database;
import bcidExceptions.OAUTHException;
import bcidExceptions.ServerErrorException;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.stringGenerator;

import java.sql.*;

/**
 * This class handles all aspects of Oauth2 support.
 */
public class provider {
    protected Connection conn;
    database db;
    private static Logger logger = LoggerFactory.getLogger(provider.class);

    public provider() {
        db = new database();
        conn = db.getConn();
    }

    public void close() {
        db.close();
    }

    /**
     * check that the given clientId is valid
     *
     * @param clientId
     *
     * @return
     */
    public Boolean validClientId(String clientId) {
        try {
            String selectString = "SELECT count(*) as count FROM oauthClients WHERE client_id = ?";
            PreparedStatement stmt = conn.prepareStatement(selectString);

            stmt.setString(1, clientId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") >= 1;
            }
        } catch (SQLException e) {
            throw new OAUTHException("server_error", "error validating clientId", 500, e);
        }
        return false;
    }

    /**
     * get the callback url stored for the given clientID
     *
     * @param clientID
     *
     * @return
     */
    public String getCallback(String clientID) {
        try {
            String selectString = "SELECT callback FROM oauthClients WHERE client_id = ?";
            PreparedStatement stmt = conn.prepareStatement(selectString);

            stmt.setString(1, clientID);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("callback");
            }
        } catch (SQLException e) {
            throw new OAUTHException("server_error", "SQLException while trying to retrieve callback url for oauth client_id: " + clientID, 500, e);
        }
        return null;
    }

    /**
     * generate a random 20 character code that can be exchanged for an access token by the client app
     *
     * @param clientID
     * @param redirectURL
     * @param username
     *
     * @return
     */
    public String generateCode(String clientID, String redirectURL, String username) {
        stringGenerator sg = new stringGenerator();
        String code = sg.generateString(20);

        Integer user_id = db.getUserId(username);
        if (user_id == null) {
            throw new OAUTHException("server_error", "null user_id returned for username: " + username, 500);
        }

        PreparedStatement stmt = null;
        String insertString = "INSERT INTO oauthNonces (client_id, code, user_id, redirect_uri) VALUES(?, \"" + code + "\",?,?)";
        try {
            stmt = conn.prepareStatement(insertString);

            stmt.setString(1, clientID);
            stmt.setInt(2, user_id);
            stmt.setString(3, redirectURL);

            stmt.execute();
        } catch (SQLException e) {
            throw new OAUTHException("server_error", "error saving oauth nonce to db", 500, e);
        } finally {
            db.close(stmt, null);
        }
        return code;
    }

    /**
     * generate a new clientId for a oauth client app
     *
     * @return
     */
    public static String generateClientId() {
        stringGenerator sg = new stringGenerator();
        return sg.generateString(20);
    }

    /**
     * generate a client secret for a oauth client app
     *
     * @return
     */
    public static String generateClientSecret() {
        stringGenerator sg = new stringGenerator();
        return sg.generateString(75);
    }

    /**
     * verify that the given clientId and client secret match what is stored in the db
     *
     * @param clientId
     * @param clientSecret
     *
     * @return
     */
    public Boolean validateClient(String clientId, String clientSecret) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String selectString = "SELECT count(*) as count FROM oauthClients WHERE client_id = ? AND client_secret = ?";

//            System.out.println("clientId = \'" + clientId + "\' clientSecret=\'" + clientSecret + "\'");

            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, clientId);
            stmt.setString(2, clientSecret);

            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") >= 1;
            }
        } catch (SQLException e) {
            throw new OAUTHException("server_error", "Server Error validating oauth client", 500, e);
        } finally {
            db.close(stmt, rs);
        }
        return false;
    }

    /**
     * verify that the given code was issued for the same client id that is trying to exchange the code for an access
     * token
     *
     * @param clientID
     * @param code
     * @param redirectURL
     *
     * @return
     */
    public Boolean validateCode(String clientID, String code, String redirectURL) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String selectString = "SELECT current_timestamp() as current,ts FROM oauthNonces WHERE client_id = ? AND code = ? AND redirect_uri = ?";
            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, clientID);
            stmt.setString(2, code);
            stmt.setString(3, redirectURL);

            rs = stmt.executeQuery();

            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("ts");
                // Get the current time from the database (in case the application server is in a different timezone)
                Timestamp currentTs = rs.getTimestamp("current");
                // 10 minutes previous
                Timestamp expiredTs = new Timestamp(currentTs.getTime() - 600000);

                // if ts is less then 10 mins old, code is valid
                if (ts != null && ts.after(expiredTs)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            throw new OAUTHException("server_error", "Server Error validating oauth code", 500, e);
        } finally {
            db.close(stmt, rs);
        }
        return false;
    }

    /**
     * get the id of the user that the given oauth code represents
     *
     * @param clientId
     * @param code
     *
     * @return
     */
    private Integer getUserId(String clientId, String code) {
        Integer user_id = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String selectString = "SELECT user_id FROM oauthNonces WHERE client_id=? AND code=?";
            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, clientId);
            stmt.setString(2, code);

            rs = stmt.executeQuery();
            if (rs.next()) {
                user_id = rs.getInt("user_id");
            }

        } catch (SQLException e) {
            throw new ServerErrorException("server_error",
                    "SQLException thrown while retrieving the userID that belongs to the oauth code: " + code, e);
        } finally {
            db.close(stmt, rs);
        }
        return user_id;
    }

    /**
     * Remove the given oauth code from the db. This is called when the code is exchanged for an access token,
     * as oauth codes are only usable once.
     *
     * @param clientId
     * @param code
     */
    private void deleteNonce(String clientId, String code) {
        PreparedStatement stmt = null;
        try {
            String deleteString = "DELETE FROM oauthNonces WHERE client_id = ? AND code = ?";
            stmt = conn.prepareStatement(deleteString);

            stmt.setString(1, clientId);
            stmt.setString(2, code);

            stmt.execute();
        } catch (SQLException e) {
            logger.warn("SQLException thrown while deleting oauth nonce with code: {}", code, e);
        } finally {
            db.close(stmt, null);
        }
    }

    /**
     * generate a new access token given a refresh token
     *
     * @param refreshToken
     *
     * @return
     */
    public String generateToken(String refreshToken) {
        Integer userId = null;
        String clientId = null;

        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT client_id, user_id FROM oauthTokens WHERE refresh_token = ?";
        try {
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, refreshToken);

            rs = stmt.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("user_id");
                clientId = rs.getString("client_id");
            }
        } catch (SQLException e) {
            throw new OAUTHException("server_error", "error retrieving oauth client information from db", 500, e);
        } finally {
            db.close(stmt, rs);
        }

        if (userId == null || clientId == null) {
            throw new OAUTHException("server_error", "userId or clientId was null for refreshToken: " + refreshToken, 500);
        }

        return generateToken(clientId, userId, null);

    }

    /**
     * generate an access token given a code, clientID, and state (optional)
     *
     * @param clientID
     * @param state
     * @param code
     *
     * @return
     */
    public String generateToken(String clientID, String state, String code) {
        Integer user_id = getUserId(clientID, code);
        deleteNonce(clientID, code);
        if (user_id == null) {
            throw new OAUTHException("server_error", "userId was null for oauthNonce with code: " + code, 500);
        }

        return generateToken(clientID, user_id, state);
    }

    /**
     * generate an oauth compliant JSON response with an access_token and refresh_token
     *
     * @param clientID
     * @param userId
     * @param state
     *
     * @return
     */
    private String generateToken(String clientID, Integer userId, String state) {
        stringGenerator sg = new stringGenerator();
        String token = sg.generateString(20);
        String refreshToken = sg.generateString(20);

        String insertString = "INSERT INTO oauthTokens (client_id, token, refresh_token, user_id) VALUE " +
                "(?, \"" + token + "\",\"" + refreshToken + "\", ?)";
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(insertString);

            stmt.setString(1, clientID);
            stmt.setInt(2, userId);
            stmt.execute();
        } catch (SQLException e) {
            throw new OAUTHException("server_error", "Server error while trying to save oauth access token to db.", 500, e);
        } finally {
            db.close(stmt, null);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"access_token\":\"" + token + "\",\n");
        sb.append("\"refresh_token\":\"" + refreshToken + "\",\n");
        sb.append("\"token_type\":\"bearer\",\n");
        sb.append("\"expires_in\":3600\n");
        if (state != null) {
            sb.append(("\"state\":\"" + state + "\""));
        }
        sb.append("}");

        return sb.toString();
    }

    /**
     * delete an access_token. This is called when a refresh_token has been exchanged for a new access_token.
     *
     * @param refreshToken
     */
    public void deleteAccessToken(String refreshToken) {
        PreparedStatement stmt = null;
        try {
            String deleteString = "DELETE FROM oauthTokens WHERE refresh_token = ?";
            stmt = conn.prepareStatement(deleteString);

            stmt.setString(1, refreshToken);

            stmt.execute();
        } catch (SQLException e) {
            logger.warn("SQLException while deleting oauth access token with the refreshToken: {}", refreshToken, e);
        } finally {
            db.close(stmt, null);
        }
    }

    /**
     * verify that a refresh token is still valid
     *
     * @param refreshToken
     *
     * @return
     */
    public Boolean validateRefreshToken(String refreshToken) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT current_timestamp() as current,ts FROM oauthTokens WHERE refresh_token = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, refreshToken);

            rs = stmt.executeQuery();

            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("ts");

                // Get the current time from the database (in case the application server is in a different timezone)
                Timestamp currentTs = rs.getTimestamp("current");
                // get a Timestamp instance for  for 24 hrs ago
                Timestamp expiredTs = new Timestamp(currentTs.getTime() - 86400000);

                // if ts is older 24 hrs, we can't proceed
                if (ts != null && ts.after(expiredTs)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            throw new OAUTHException("server_error", "server error validating refresh token", 500, e);
        } finally {
            db.close(stmt, rs);
        }
//        System.out.println(sql + refreshToken);
        return false;
    }

    /**
     * Verify that an access token is still valid. Access tokens are only good for 1 hour.
     *
     * @param token the access_token issued to the client
     *
     * @return username the token represents, null if invalid token
     */
    public String validateToken(String token) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String selectString = "SELECT current_timestamp() as current,t.ts as ts, u.username as username " +
                    "FROM oauthTokens t, users u WHERE t.token=? && u.user_id = t.user_id";
            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, token);

            rs = stmt.executeQuery();
            if (rs.next()) {

                Timestamp ts = rs.getTimestamp("ts");

                // Get the current time from the database (in case the application server is in a different timezone)
                Timestamp currentTs = rs.getTimestamp("current");
                // get a Timestamp instance for 1 hr ago
                Timestamp expiredTs = new Timestamp(currentTs.getTime() - 3600000);
                // if ts is older then 1 hr, we can't proceed
                if (ts != null && ts.after(expiredTs)) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            throw new OAUTHException("server_error", "error while validating access_token", 500, e);
        } finally {
            db.close(stmt, rs);
        }

        return null;
    }

    /**
     * Given a hostname, register a client app for oauth use. Will generate a new client id and client secret
     * for the client app
     *
     * @param args
     */
    public static void main(String args[]) {


        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        CommandLine cl;

        Options options = new Options();
        options.addOption("c", "callback url", true, "The callback url of the client app");

        try {
            cl = clp.parse(options, args);
        } catch (UnrecognizedOptionException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        } catch (ParseException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        if (!cl.hasOption("c")) {
            System.out.println("You must enter a callback url");
            return;
        }

        String host = cl.getOptionValue("c");
        String clientId = generateClientId();
        String clientSecret = generateClientSecret();


        String insertString = "INSERT INTO oauthClients (client_id, client_secret, callback) VALUES (\""
                + clientId + "\",\"" + clientSecret + "\",\"" + host + "\")";
        System.out.println("Use the following insert string:");
        System.out.println(insertString);
        System.out.println("Once that is done the oauth2 client app at host: " + host
                + ".\n will need the following information:\n\nclient_id: "
                + clientId + "\nclient_secret: " + clientSecret);

        /*
        provider p = null;
        PreparedStatement stmt = null;
        try {
            p = new provider();

            String clientId = p.generateClientId();
            String clientSecret = p.generateClientSecret();

            String insertString = "INSERT INTO oauthClients (client_id, client_secret, callback) VALUES (\""
                                  + clientId + "\",\"" + clientSecret + "\",?)";

//            System.out.println("USE THE FOLLOWING INSERT STATEMENT IN YOUR DATABASE:\n\n");
//            System.out.println("INSERT INTO oauthClients (client_id, client_secret, callback) VALUES (\""
//                    + clientId + "\",\"" + clientSecret + "\",\"" + host + "\")");
//            System.out.println(".\nYou will need the following information:\n\nclient_id: "
//                    + clientId + "\nclient_secret: " + clientSecret);
            stmt = p.conn.prepareStatement(insertString);

            stmt.setString(1, host);
            stmt.execute();

            System.out.println("Successfully registered oauth2 client app at host: " + host
                    + ".\nYou will need the following information:\n\nclient_id: "
                    + clientId + "\nclient_secret: " + clientSecret);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } finally {
            p.db.close(stmt, null);
            p.close();
        }
        */
    }

}
