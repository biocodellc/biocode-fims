package auth;

import bcid.Database;
import fimsExceptions.ServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Calendar;

/**
 * Class to check whether a user has certain permissions, such as if they are a project admin
 */
public class Authorizer {
    protected Connection conn;
    private Database db;
    private static Logger logger = LoggerFactory.getLogger(Authorizer.class);

    public Authorizer() {
        db = new Database();
        conn = db.getConn();
    }

    /**
     * determine if the user is an admin for any projects
     * @param username
     * @return
     */
    public Boolean userProjectAdmin(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            Integer userId = db.getUserId(username);
            String selectString = "SELECT count(*) as count FROM projects WHERE userId = ?";

            stmt = conn.prepareStatement(selectString);
            stmt.setInt(1, userId);

            rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("count") >= 1;

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * determine if the password reset token is still valid
     * @param token
     * @return
     */
    public Boolean validResetToken(String token) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT passwordResetExpiration as ts FROM users WHERE passwordResetToken = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, token);

            rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp expirationTs = rs.getTimestamp("ts");
                Timestamp ts = new Timestamp(Calendar.getInstance().getTime().getTime());
                if (expirationTs != null && expirationTs.after(ts)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error while validating reset token",
                    "db error retrieving reset token expiration", e);
        } finally {
            db.close(stmt, rs);
        }
        return false;
    }

    public void close() {
        db.close();
    }
}
