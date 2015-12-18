package bcid;

import fimsExceptions.ServerErrorException;
import ezid.EZIDException;
import ezid.EZIDService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.SettingsManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Class to work with EZID creation from the bcid database.  requests to this class are controlled by
 * switches in the database indicating whether the intention is to create EZIDS for particular identifiers.
 */
public class manageEZID extends bcidMinter {

    private String publisher;
    private String creator;
    private static Logger logger = LoggerFactory.getLogger(manageEZID.class);

    public manageEZID() {
        super();
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();

        publisher = sm.retrieveValue("publisher");
        if (publisher == null || publisher.trim().equalsIgnoreCase("")) {
            publisher = "Biocode FIMS System";
        }

        creator = sm.retrieveValue("creator");
        if (creator.trim().equalsIgnoreCase("")) {
            creator = null;
        }
    }

    public HashMap<String, String> ercMap(String target, String what, String who, String when) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("_profile", "erc");

        // _target needs to be resolved by biscicol for now
        map.put("_target", target);
        // what is always dataset
        map.put("erc.what", what);
        // who is the user who loaded this
        map.put("erc.who", who);
        // when is timestamp of data loading
        map.put("erc.when", when);
        return map;
    }

    public HashMap<String, String> dcMap(String target, String creator, String title, String publisher, String when, String type) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("_profile", "dc");
        // _target needs to be resolved by biscicol for now
        map.put("_target", target);
        map.put("dc.creator", creator);
        map.put("dc.title", title);
        map.put("dc.publisher", publisher);
        map.put("dc.date", when);
        map.put("dc.type", type);
        return map;
    }

    /**
     * Update EZID bcid metadata for this particular ID
     */
    public void updateBcidsEZID(EZIDService ezid, int bcidId) throws EZIDException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {

            String sql = "SELECT " +
                    "b.bcidId as bcidId," +
                    "b.prefix as prefix," +
                    "b.title as title," +
                    "b.ts as ts," +
                    "b.resourceType as type," +
                    "concat_ws('',CONCAT_WS(' ',u.firstName, u.lastName),' <',u.email,'>') as creator " +
                    "FROM bcids b,users u " +
                    "WHERE ezidMade && b.userId=u.userId " +
                    "AND b.bcidId = ? " +
                    "LIMIT 1000";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bcidId);
            rs = stmt.executeQuery();

            rs.next();

            // Get creator, using any system defined creator to override the default which is based on user data
            if (creator == null) {
                creator = rs.getString("creator");
            }

            // Build the hashmap to pass to ezid
            HashMap<String, String> map = dcMap(
                    resolverTargetPrefix + rs.getString("prefix"),
                    creator,
                    rs.getString("title"),
                    publisher,
                    rs.getString("ts"),
                    rs.getString("type"));
            map.put("_profile", "dc");

            // The ID string to register with EZID
            String myIdentifier = rs.getString("prefix");

            try {
                ezid.setMetadata(myIdentifier, map);
                logger.info("  Updated Metadata for " + myIdentifier);
            } catch (EZIDException e1) {
                // After attempting to set the Metadata, if another exception is thrown then who knows,
                // probably just a permissions issue.
                throw new EZIDException("  Exception thrown in attempting to create EZID " + myIdentifier + ", likely a permission issue", e1);
            }


        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * Go through bcids table and create any ezid fields that have yet to be created.
     * This method is meant to be called via a cronjob on the backend.
     * <p/>
     * TODO: throw a special exception on this method so we can follow up why EZIDs are not being made if that is the
     * case
     *
     * @param ezid
     *
     * @throws java.net.URISyntaxException
     */
    public void createBcidsEZIDs(EZIDService ezid) throws EZIDException {
        // Grab a row where ezid is false
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ArrayList<String> idSuccessList = new ArrayList();
        try {
            String sql = "SELECT b.bcidId as bcidId," +
                    "b.prefix as prefix," +
                    "b.ts as ts," +
                    "b.resourceType as type," +
                    "b.title as title," +
                    "concat_ws('',CONCAT_WS(' ',u.firstName, u.lastName),' <',u.email,'>') as creator " +
                    "FROM bcids b,users u " +
                    "WHERE !ezidMade && ezidRequest && b.userId=u.userId && u.username != 'demo'" +
                    "LIMIT 1000";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            // Attempt to create an EZID for this row
            while (rs.next()) {
                URI identifier = null;

                // Get creator, using any system defined creator to override the default which is based on user data
                if (creator == null) {
                    creator = rs.getString("creator");
                }

                // Dublin Core metadata profile element
                HashMap<String, String> map = dcMap(
                        resolverTargetPrefix + rs.getString("prefix"),
                        creator,
                        rs.getString("title"),
                        publisher,
                        rs.getString("ts"),
                        rs.getString("type"));
                map.put("_profile", "dc");

                // The ID string to register with ezid
                String myIdentifier = rs.getString("prefix");

                // Register this as an EZID
                try {
                    identifier = new URI(ezid.createIdentifier(myIdentifier, map));
                    idSuccessList.add(rs.getString("bcidId"));
                    logger.info("{}", identifier.toString());
                } catch (EZIDException e) {
                    // Adding this for debugging
                    //e.printStackTrace();
                    // Attempt to set Metadata if this is an Exception
                    try {
                        ezid.setMetadata(myIdentifier, map);
                        idSuccessList.add(rs.getString("bcidId"));
                    } catch (EZIDException e1) {
                        //TODO should we silence this exception?
                        logger.warn("Exception thrown in attempting to create OR update EZID {}, a permission issue?", myIdentifier, e1);
                    }

                } catch (URISyntaxException e) {
                    throw new EZIDException("Bad uri syntax for " + myIdentifier + ", " + map, e);
                }

            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException when creating EZID", e);
        } finally {
            db.close(stmt, rs);
        }

        // Update the Identifiers Table and let it know that we've created the EZID
        try {
            updateEZIDMadeField(idSuccessList);
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "It appears we have created " + idSuccessList.size() +
                    " EZIDs but not able to update the bcids table", e);
        }

    }

    /**
     * Go through database, search for requests to make EZIDs and update the ezidMade (boolean) to true
     * This function works for bcids
     *
     * @param idSuccessList
     *
     * @throws java.sql.SQLException
     */
    private void updateEZIDMadeField(ArrayList idSuccessList) throws SQLException {

        // Turn off autocommits at beginning of the next block
        conn.setAutoCommit(false);
        PreparedStatement updateStatement = null;

        // Loop and update
        try {
            String updateString = "" +
                    "UPDATE bcids " +
                    "SET ezidMade=true " +
                    "WHERE bcidId=? && !ezidMade";
            updateStatement = conn.prepareStatement(updateString);
            Iterator ids = idSuccessList.iterator();
            int count = 0;
            while (ids.hasNext()) {
                String id = (String) ids.next();
                updateStatement.setString(1, id);
                updateStatement.addBatch();
                // Execute every 1000 rows
                if (count + 1 % 1000 == 0) {
                    updateStatement.executeBatch();
                    conn.commit();
                }
                count++;
            }
            updateStatement.executeBatch();
            conn.commit();

        } finally {
            db.close(updateStatement, null);
            conn.setAutoCommit(true);
        }
    }
}
