package utils;

import bcid.BcidMinter;
import bcid.Database;
import bcid.ExpeditionMinter;
import fimsExceptions.ServerErrorException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.HashMap;

/**
 * helper class to update existing expeditions to include a Collection Bcid for the expedition
 */
public class ExpeditionUpdater {

    protected static Database db;
    protected static Connection conn;

    static {
        db = new Database();
        conn = db.getConn();
    }
    /**
     * Return a JSON response of the user's expeditions in a project
     *
     * @return
     */
    public HashMap getAllExpeditions() {
        HashMap expeditions = new HashMap();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT expeditionId, userId " +
                    "FROM expeditions";
            stmt = conn.prepareStatement(sql);

            rs = stmt.executeQuery();
            while (rs.next()) {
                expeditions.put(rs.getInt("expeditionId"), rs.getInt("userId"));
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }

        return expeditions;
    }

    public static void main(String args[]) {
        ExpeditionUpdater expeditionUpdater = new ExpeditionUpdater();
        HashMap expeditions = expeditionUpdater.getAllExpeditions();

        for (Object expeditionId : expeditions.keySet()) {

            if (!expeditionUpdater.expeditionHasBCID((Integer) expeditionId)) {
                // if the collection Bcid doesn't exist for the expedition, create it
                System.out.println("Creating bcid for expedition id: " + expeditionId);
                BcidMinter bcidMinter = new BcidMinter(false);
                String identifier = bcidMinter.createEntityBcid((Integer) expeditions.get(expeditionId),
                        "http://purl.org/dc/dcmitype/Collection", null, null, null, false);
                bcidMinter.close();

                // Associate this Bcid with this expedition
                ExpeditionMinter expedition = new ExpeditionMinter();
                expedition.attachReferenceToExpedition((Integer) expeditionId, identifier);
                expedition.close();

            }
        }
    }

    private boolean expeditionHasBCID(Integer expeditionId ) {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT count(*) FROM bcids b, expeditionBcids eB " +
                    "WHERE e.expeditionId = " + expeditionId + " AND b.bcidId = eB.bcidId AND b.resourceType = " + "\"http://purl.org/dc/dcmitype/Collection\"";
            stmt = conn.prepareStatement(sql);

            rs = stmt.executeQuery();
            rs.next();
            if (rs.getInt("count(*)") > 0) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
    }
}
