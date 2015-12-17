package utils;

import bcid.bcidMinter;
import bcid.database;
import bcid.expeditionMinter;
import fimsExceptions.ServerErrorException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.HashMap;

/**
 * helper class to update existing expeditions to include a Collection bcid for the expedition
 */
public class expeditionUpdater {

    protected static database db;
    protected static Connection conn;

    static {
        db = new database();
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
            String sql = "SELECT expedition_id, users_id " +
                    "FROM expeditions";
            stmt = conn.prepareStatement(sql);

            rs = stmt.executeQuery();
            while (rs.next()) {
                expeditions.put(rs.getInt("expedition_id"), rs.getInt("users_id"));
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }

        return expeditions;
    }

    public static void main(String args[]) {
        expeditionUpdater expeditionUpdater = new expeditionUpdater();
        HashMap expeditions = expeditionUpdater.getAllExpeditions();

        for (Object expedition_id : expeditions.keySet()) {

            if (!expeditionUpdater.expeditionHasBCID((Integer) expedition_id)) {
                // if the collection bcid doesn't exist for the expedition, create it
                System.out.println("Creating bcid for expedition id: " + expedition_id);
                bcidMinter bcidMinter = new bcidMinter(false);
                bcidMinter.createEntityBcid((Integer) expeditions.get(expedition_id), "http://purl.org/dc/dcmitype/Collection",
                        null, null, null, false);

                // Associate this identifier with this expedition
                expeditionMinter expedition = new expeditionMinter();
                expedition.attachReferenceToExpedition((Integer) expedition_id, bcidMinter.getPrefix());
                bcidMinter.close();
                expedition.close();

            }
        }
    }

    private boolean expeditionHasBCID(Integer expedition_id ) {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT count(*) FROM bcids b, expeditionsBCIDs eB " +
                    "WHERE e.expedition_id = " + expedition_id + " AND b.bcids_id = eB.bcids_idAND b.resourceType = " + "\"http://purl.org/dc/dcmitype/Collection\"";
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
