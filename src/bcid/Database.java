package bcid;

import biocode.fims.fimsExceptions.ServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import biocode.fims.SettingsManager;

import java.sql.*;

/**
 * Creates the connection for the backend Bcid Database.
 * Settings come from the biocode.fims.SettingsManager/Property file defining the user/password/url/class
 * for the mysql Database where the data lives.
 */
public class Database {

    // Mysql Connection
    protected Connection conn;
    final static Logger logger = LoggerFactory.getLogger(Database.class);

    /**
     * Load settings for creating this Database connection from the bcidsettings.properties file
     */
    public Database() {
        try {
            SettingsManager sm = SettingsManager.getInstance();
            String bcidUser = sm.retrieveValue("bcidUser");
            String bcidPassword = sm.retrieveValue("bcidPassword");
            String bcidUrl = sm.retrieveValue("bcidUrl");
            String bcidClass = sm.retrieveValue("bcidClass");

            Class.forName(bcidClass);
            conn = DriverManager.getConnection(bcidUrl, bcidUser, bcidPassword);
        } catch (ClassNotFoundException e) {
            throw new ServerErrorException("Server Error","Driver issues accessing BCID system", e);
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error","SQL Exception accessing BCID system", e);
        }

    }

    public Connection getConn() {
        return conn;
    }

    public void close(Statement stmt, ResultSet rs) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.warn("SQLException while attempting to close PreparedStatement.", e);
            }
        }
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.warn("SQLException while attempting to close ResultSet.", e);
            }
        }
        return;
    }


    public void close(PreparedStatement stmt, ResultSet rs) {
       close((Statement)stmt,rs);
    }

    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            logger.warn("SQLException while attempting to close connection.", e);
        }
        return;
    }

    /**
     * Return the userID given a username
     * @param username
     * @return
     */
    public Integer getUserId(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "Select userId from users where username=?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("userId");
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error",
                    "SQLException attempting to getUserId when given the username: {}", e);
        } finally {
            close(stmt, rs);
        }
        return null;
    }
    /**
     * Return the username given a userId
     * @param userId
     * @return
     */
    public String getUserName(Integer userId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT username FROM users WHERE userId = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("username");
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error",
                    "SQLException attempting to getUserName when given the userId: {}", e);
        } finally {
            close(stmt, rs);
        }
        return null;
    }

}
