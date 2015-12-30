package bcid;

import ezid.EzidService;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.SettingsManager;

import javax.ws.rs.core.MultivaluedMap;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Mint new expeditions.  Includes the automatic creation of a core set of entity types
 */
public class ExpeditionMinter {
    protected Connection conn;
    public ArrayList<Integer> expeditionResources;
    private SettingsManager sm;
    private EzidService ezidAccount;
    private String resolverTargetPrefix;
    private String resolverMetadataPrefix;
    Database db;

    private static Logger logger = LoggerFactory.getLogger(ExpeditionMinter.class);

    /**
     * The constructor defines the class-level variables used when minting Expeditions.
     * It defines a generic set of entities (process, information content, objects, agents)
     * that can be used for any expedition.
     */
    public ExpeditionMinter() {
        db = new Database();
        conn = db.getConn();

        // Initialize settings manager
        sm = SettingsManager.getInstance();
        sm.loadProperties();

        resolverTargetPrefix = sm.retrieveValue("resolverTargetPrefix");
        resolverMetadataPrefix = sm.retrieveValue("resolverMetadataPrefix");
    }

    public void close() {
        db.close();
    }

    /**
     * mint Expedition
     *
     * @param expeditionCode
     * @param expeditionTitle
     * @param userId
     *
     * @return
     */
    public Integer mint(
            String expeditionCode,
            String expeditionTitle,
            Integer userId,
            Integer projectId,
            Boolean isPublic) throws FimsException {

        Integer expeditionId = null;

        if (!userExistsInProject(userId, projectId)) {
            throw new ForbiddenRequestException("User ID " + userId + " is not authorized to create expeditions in this project");
        }

        /**
         *  Insert the values into the expeditions table
         */
        checkExpeditionCodeValid(expeditionCode);
        if (!isExpeditionCodeAvailable(expeditionCode, projectId)) {
            throw new BadRequestException("Expedition Code already exists");
        }

        // Generate an internal ID to track this submission
        UUID internalId = UUID.randomUUID();

        // Use auto increment in Database to assign the actual Bcid.. this is threadsafe this way
        String insertString = "INSERT INTO expeditions " +
                "(internalId, expeditionCode, expeditionTitle, userId, projectId,public) " +
                "values (?,?,?,?,?,?)";
//            System.out.println("INSERT string " + insertString);
        PreparedStatement insertStatement = null;
        try{
            insertStatement = conn.prepareStatement(insertString);
            insertStatement.setString(1, internalId.toString());
            insertStatement.setString(2, expeditionCode);
            insertStatement.setString(3, expeditionTitle);
            insertStatement.setInt(4, userId);
            insertStatement.setInt(5, projectId);
            insertStatement.setBoolean(6, isPublic);

            insertStatement.execute();

            // Get the expeditionId that was assigned
            expeditionId = getExpeditionIdentifier(internalId);

            // upon successful expedition creation, create the expedition Bcid
            BcidMinter bcidMinter = new BcidMinter(false);
            String identifier = bcidMinter.createEntityBcid(userId, "http://purl.org/dc/dcmitype/Collection", null, null,
                    null, false);
            bcidMinter.close();

            // Associate this Bcid with this expedition
            ExpeditionMinter expedition = new ExpeditionMinter();
            expedition.attachReferenceToExpedition(expeditionId, identifier);
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(insertStatement, null);
        }
        return expeditionId;
    }


    /**
     * Attach an individual URI reference to a expedition
     *
     * @param expeditionCode
     * @param bcid
     *
     */
    public void attachReferenceToExpedition(String expeditionCode, String bcid, Integer projectId) {
        Integer expeditionId = getExpeditionIdentifier(expeditionCode, projectId);
        Resolver r = new Resolver(bcid);
        Integer bcidsId = r.getBcidId();
        r.close();

        attachReferenceToExpedition(expeditionId, bcidsId);
    }

    private void attachReferenceToExpedition(Integer expeditionId, Integer bcidsId) {

        String insertString = "INSERT INTO expeditionBcids " +
                "(expeditionId, bcidId) " +
                "values (?,?)";

        PreparedStatement insertStatement = null;
        try {
            insertStatement = conn.prepareStatement(insertString);
            insertStatement.setInt(1, expeditionId);
            insertStatement.setInt(2, bcidsId);
            insertStatement.execute();
        } catch (SQLException e) {
            throw new ServerErrorException("Db error attaching Reference to Expedition", e);
        } finally {
            db.close(insertStatement, null);
        }
    }

    /**
     * Attach an individual URI reference to a expedition
     *
     * @param expeditionId
     * @param bcid
     */
    public void attachReferenceToExpedition(Integer expeditionId, String bcid) {
        Resolver r = new Resolver(bcid);
        Integer bcidsId = r.getBcidId();
        r.close();

        attachReferenceToExpedition(expeditionId, bcidsId);
    }

    /**
     * Return the expedition Bcid given the internalId
     *
     * @param expeditionUUID
     *
     * @return
     *
     * @throws java.sql.SQLException
     */
    private Integer getExpeditionIdentifier(UUID expeditionUUID) throws SQLException {
        String sql = "select expeditionId from expeditions where internalId = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, expeditionUUID.toString());
        ResultSet rs = stmt.executeQuery();
        try {
            rs.next();
            return rs.getInt("expeditionId");
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException while getting expedition Identifier", e);
        } finally {
            db.close(stmt, rs);
        }
    }

    private Integer getExpeditionIdentifier(String expeditionCode, Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT expeditionId " +
                    "FROM expeditions " +
                    "WHERE expeditionCode = ? AND " +
                    "projectId = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, expeditionCode);
            stmt.setInt(2, projectId);

            rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("expeditionId");
        } catch (SQLException e) {
            throw new ServerErrorException("Db error while retrieving expeditionId",
                    "SQLException while retrieving expeditionId from expeditions table with expeditionCode: " +
                    expeditionCode + " and projectId: " + projectId, e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /***
     *
     * @param expeditionCode
     * @param ProjectId
     * @return
     */
    public Boolean expeditionExistsInProject(String expeditionCode, Integer ProjectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "select expeditionId from expeditions " +
                    "where expeditionCode = ? && " +
                    "projectId = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, expeditionCode);
            stmt.setInt(2, ProjectId);

            rs = stmt.executeQuery();
            if (rs.next()) return true;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
        return false;
    }

    public String printMetadata(int id) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            StringBuilder sb = new StringBuilder();
            String sql = "select expeditionId,expeditionCode,expeditionTitle,username from expeditions,users where users.userId = expeditions.userId && expeditionId = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, id);

            rs = stmt.executeQuery();
            sb.append("***expedition***");

            // Get result set meta data
            ResultSetMetaData rsmd = rs.getMetaData();
            int numColumns = rsmd.getColumnCount();

            while (rs.next()) {
                // Loop mapped values, now we know the type
                for (int i = 1; i <= numColumns; i++) {
                    String val = rsmd.getColumnLabel(i);
                    sb.append("\n" + val + " = " + rs.getString(val));
                }
            }
            return sb.toString();
        } finally {
            db.close(stmt, rs);
        }
    }

    public String printMetadataHTML(int id) {
        StringBuilder sb = new StringBuilder();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT expeditionId,expeditionCode,expeditionTitle,username " +
                    "FROM expeditions,users " +
                    "WHERE users.userId = expeditions.userId " +
                    "&& expeditionId = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, id);

            rs = stmt.executeQuery();
            sb.append("<table>");

            // Get result set meta data
            ResultSetMetaData rsmd = rs.getMetaData();
            int numColumns = rsmd.getColumnCount();

            while (rs.next()) {
                // Loop mapped values, now we know the type
                for (int i = 1; i <= numColumns; i++) {
                    String val = rsmd.getColumnLabel(i);
                    sb.append("<tr><td>" + val + "</td><td>" + rs.getString(val) + "</td></tr>");
                }
            }
            sb.append("</table>");
            return sb.toString();
        } catch (SQLException e) {
            throw new ServerErrorException("Db error retrieving expedition metadata", e);
        } finally {
            db.close(stmt, rs);
        }
    }


    /**
     * Discover if a user owns this expedition or not
     *
     * @param userId
     * @param expeditionCode
     *
     * @return
     */
    public boolean userOwnsExpedition(Integer userId, String expeditionCode, Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            //String sql = "select expeditionId,expeditionCode,expeditionTitle,username from expeditions,users where users.userId = expeditions.userId && users.username =\"" + remoteUser + "\"";

            String sql = "SELECT " +
                    "   count(*) as count " +
                    "FROM " +
                    "   expeditions " +
                    "WHERE " +
                    "   expeditionCode= ? && " +
                    "   userId = ? && " +
                    "   projectId = ?";
//            System.out.println(sql);
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, expeditionCode);
            stmt.setInt(2, userId);
            stmt.setInt(3, projectId);

            rs = stmt.executeQuery();
            rs.next();
            if (rs.getInt("count") < 1)
                return false;
            else
                return true;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * Discover if a user belongs to an project
     *
     * @param userId
     * @param projectId
     *
     * @return
     */
    public boolean userExistsInProject(Integer userId, Integer projectId) {
        String selectString = "SELECT count(*) as count FROM userProjects WHERE userId = ? && projectId = ?";
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.prepareStatement(selectString);

            stmt.setInt(1, userId);
            stmt.setInt(2, projectId);

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
     * Generate a Deep Links Format data file for describing a set of root prefixes and associated concepts
     *
     * @param expeditionCode
     *
     * @return
     */
    public String getDeepRoots(String expeditionCode, Integer projectId) {
        // Get todays's date
        DateFormat dateFormat;
        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String expeditionTitle = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        StringBuilder sb = new StringBuilder();

        try {
            // Construct the query
            String sql =
                    "SELECT " +
                            " b.identifier, " +
                            " b.resourceType as resourceType," +
                            " b.title as alias, " +
                            " a.expeditionTitle as expeditionTitle " +
                            "FROM " +
                            " expeditions a, expeditionBcids eB, bcids b " +
                            "WHERE" +
                            " a.expeditionId = eB.expeditionId && " +
                            " eB.bcidId = b.bcidId && \n" +
                            " a.expeditionCode = ? && \n" +
                            " a.projectId = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, expeditionCode);
            stmt.setInt(2, projectId);

            // Write the concept/identifier elements section
            sb.append("[\n{\n\t\"data\": [\n");
            rs = stmt.executeQuery();
            while (rs.next()) {
                // Grap the expeditionTitle in the query
                if (expeditionTitle == null & !rs.getString("expeditionTitle").equals(""))
                    expeditionTitle = rs.getString("expeditionTitle");

                // Grap the prefixes and concepts associated with this
                sb.append("\t\t{\n");
                sb.append("\t\t\t\"identifier\":\"" + rs.getString("b.identifier") + "\",\n");
                sb.append("\t\t\t\"concept\":\"" + rs.getString("resourceType") + "\",\n");
                sb.append("\t\t\t\"alias\":\"" + rs.getString("alias") + "\"\n");
                sb.append("\t\t}");
                if (!rs.isLast())
                    sb.append(",");

                sb.append("\n");
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }

        sb.append("\t]\n},\n");

        // Write the metadata section
        sb.append("{\n");
        sb.append("\t\"metadata\": {\n");
        sb.append("\t\t\"name\": \" " + expeditionCode + "\",\n");
        if (expeditionTitle != null)
            sb.append("\t\t\"description\": \"" + expeditionTitle + "\",\n");
        sb.append("\t\t\"date\": \" " + dateFormat.format(date) + "\"\n");
        sb.append("\t}\n");
        sb.append("}\n");
        sb.append("]\n");
        return sb.toString();
    }

    /**
     * Get Metadata on a named graph
     *
     * @param graphName
     *
     * @return
     */
    public String getGraphMetadata(String graphName) {
        // Get todays's date
        DateFormat dateFormat;
        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String expeditionTitle = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        StringBuilder sb = new StringBuilder();

        try {
            // Construct the query
            String sql =
                    "SELECT " +
                            " b.graph as graph, " +
                            " a.projectId as projectId, " +
                            " u.username as username_generator, " +
                            " u2.username as username_upload," +
                            " b.ts as timestamp," +
                            " b.identifier, " +
                            " b.resourceType as resourceType," +
                            " b.finalCopy as finalCopy," +
                            " a.expeditionCode as expeditionCode, " +
                            " a.expeditionTitle as expeditionTitle, " +
                            " a.public as public " +
                            "FROM " +
                            " expeditions a, expeditionBcids eB, bcids b, users u, users u2 " +
                            "WHERE" +
                            " u2.userId = b.userId && " +
                            " u.userId = a.userId && " +
                            " a.expeditionId = eB.expeditionId && " +
                            " eB.bcidId = b.bcidId && \n" +
                            " b.graph = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, graphName);
            //System.out.println(sql);
            // Write the concept/identifier elements section
            sb.append("{\n\t\"data\": [\n");
            rs = stmt.executeQuery();
            while (rs.next()) {
                // Grap the expeditionTitle in the query
                if (expeditionTitle == null & !rs.getString("expeditionTitle").equals(""))
                    expeditionTitle = rs.getString("expeditionTitle");

                // Grab the prefixes and concepts associated with this
                sb.append("\t\t{\n");
                sb.append("\t\t\t\"graph\":\"" + rs.getString("graph") + "\",\n");
                sb.append("\t\t\t\"projectId\":\"" + rs.getInt("projectId") + "\",\n");
                sb.append("\t\t\t\"username_generator\":\"" + rs.getString("username_generator") + "\",\n");
                sb.append("\t\t\t\"username_upload\":\"" + rs.getString("username_upload") + "\",\n");
                sb.append("\t\t\t\"timestamp\":\"" + rs.getString("timestamp") + "\",\n");
                sb.append("\t\t\t\"bcid\":\"" + rs.getString("b.identifier") + "\",\n");
                sb.append("\t\t\t\"resourceType\":\"" + rs.getString("resourceType") + "\",\n");
                sb.append("\t\t\t\"finalCopy\":\"" + rs.getBoolean("finalCopy") + "\",\n");
                sb.append("\t\t\t\"public\":\"" + rs.getBoolean("public") + "\",\n");
                sb.append("\t\t\t\"expeditionCode\":\"" + rs.getString("expeditionCode") + "\",\n");
                sb.append("\t\t\t\"expeditionTitle\":\"" + rs.getString("expeditionTitle") + "\"\n");

                sb.append("\t\t}");
                if (!rs.isLast())
                    sb.append(",");

                sb.append("\n");
            }
            sb.append("\t]\n}");
            return sb.toString();
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
    }

    public String expeditionTable(String remoteUser) {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            StringBuilder sb = new StringBuilder();
            //String sql = "select expeditionId,expeditionCode,expeditionTitle,username from expeditions,users where users.userId = expeditions.userId && users.username =\"" + remoteUser + "\"";

            String sql = "SELECT " +
                    "   a.expeditionId as expeditionId," +
                    "   a.expeditionCode as expeditionCode," +
                    "   a.expeditionTitle as expeditionTitle," +
                    "   b.identifier," +
                    "   b.resourceType as resourceType " +
                    "FROM " +
                    "   expeditions a,expeditionBcids eB, bcids b,users u " +
                    "WHERE " +
                    "   a.expeditionId=b.expeditionId && " +
                    "   eB.bcidId = b.bcidId && " +
                    "   a.userId = u.userId && " +
                    "   u.username= ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, remoteUser);
            rs = stmt.executeQuery();

            // Get result set meta data

            sb.append("<table>\n");
            sb.append("\t<tr>\n");
            sb.append("\t\t<td><b>Expedition Details</b></td>\n");
            sb.append("\t\t<td><b>Expedition BCIDs</b></td>\n");
            sb.append("\t</tr>\n");

            Integer expeditionId = 0;
            Integer thisExpedition_id = 0;
            int count = 0;
            while (rs.next()) {

                thisExpedition_id = rs.getInt("expeditionId");

                // Structure the first column-- expeditions
                if (thisExpedition_id != expeditionId) {
                    if (count > 0) {
                        sb.append("\t\t\t</table>\n\t\t</td>\n");
                        sb.append("\t</tr>\n");
                    }

                    sb.append("\t<tr>\n");
                    sb.append("\t\t<td valign=top>\n");
                    sb.append("\t\t\t<table><tr><td>expeditionID " + rs.getString("expeditionId") + "</td></tr>" +
                            "<tr><td>" + rs.getString("expeditionCode") + "</td></tr>" +
                            "<tr><td>" + rs.getString("expeditionTitle") + "</td></tr></table>\n");
                    sb.append("\t\t</td>\n");

                    sb.append("\t\t<td valign=top>\n\t\t\t<table>\n");
                } else {
                    //sb.append("\n\t\t<td></td>\n");
                }

                // Structure the second column-- BCIDs associated with expeditions
                ResourceTypes rt = new ResourceTypes();
                String rtString;
                ResourceType resourceType = rt.get(rs.getString("resourceType"));
                if (resourceType != null) {
                    rtString = "<a href='" + rs.getString("resourceType") + "'>" + resourceType.string + "</a>";
                } else {
                    rtString = "<a href='" + rs.getString("resourceType") + "'>" + rs.getString("resourceType") + "</a>";
                }


                sb.append("\t\t\t\t<tr><td><a href='" + resolverTargetPrefix + rs.getString("b.identifier") + "'>" +
                        rs.getString("b.identifier") + "</a></td>" +
                        "<td>is_a</td><td>" +
                        rtString +
                        "</td></tr>\n");

                // Close the BCID section tag
                if (thisExpedition_id != expeditionId) {
                    //if (count > 0) {
                    //    sb.append("\n\t\t\t</table>");
                    //    sb.append("\n\t\t</td>");
                    //}
                    expeditionId = thisExpedition_id;
                }
                count++;
                if (rs.isLast())
                    sb.append("\t\t\t</table>\n\t\t</td>\n");
            }

            sb.append("\t</tr>\n</table>\n");

            return sb.toString();
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error","SQLException while retrieving expeditionTable for user: " + remoteUser, e);
        } finally {
            db.close(stmt, rs);
        }
    }


    public static void main(String args[]) {
        try {
            System.out.println("init ...");
            // See if the user owns this expedition or no
            ExpeditionMinter expedition = new ExpeditionMinter();
            //System.out.println(expedition.getGraphMetadata("_qNK_fuHVbRSTNvA_8pG.xlsx"));
           System.out.println("starting ...");
//            System.out.println(expedition.listExpeditionsAsTable(9,"trizna"));
            System.out.println(expedition.listExpeditionDatasetsAsTable(30));
            System.out.println("ending ...");
            //System.out.println(expedition.listExpeditions(8,"mwangiwangui25@gmail.com"));
            //expedition.checkExpeditionCodeValid("JBD_foo-))");
            //    System.out.println("Configuration File for project = " +expedition.getValidationXML(1));
            /*
            if (expedition.expeditionExistsInProject("DEMOH", 1)) {
                System.out.println("expedition exists in project");
            } else {
                System.out.println("expedition does not exist in project");
            }
            */
            /*System.out.println(expedition.getDeepRoots("HDIM"));

            if (expedition.userOwnsExpedition(8, "DEMOG")) {
                System.out.println("YES the user owns this expedition");
            } else {
                System.out.println("NO the user does not own this expedition");
            }

*/
            // System.out.println(expedition.getLatestGraphsByExpedition(1));
            // Test associating a BCID to a expedition
            /*
            expedition.attachReferenceToExpedition("DEMOH", "ark:/21547/Fu2");
            */

            // Test creating a expedition
            /*
            Integer expeditionId = expedition.mint(
                    "DEMOH",
                    "Test creating expedition under an project for which it already exists",
                    8, 4, false);

            System.out.println(expedition.printMetadata(expeditionId));
            */

            //System.out.println(p.expeditionTable("demo"));
            expedition.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Check that expedition code is between 4 and 50 characters
     *
     * @param expeditionCode
     *
     * @return
     */
    private void checkExpeditionCodeValid(String expeditionCode) throws FimsException {
        // Check expeditionCode length
        if (expeditionCode.length() < 4 || expeditionCode.length() > 50) {
            throw new FimsException("Expedition code " + expeditionCode + " must be between 4 and 50 characters long");
        }

        // Check to make sure characters are normal!
        if (!expeditionCode.matches("[a-zA-Z0-9_-]*")) {
            throw new FimsException("Expedition code " + expeditionCode + " contains one or more invalid characters. " +
                    "Expedition code characters must be in one of the these ranges: [a-Z][0-9][-][_]");
        }
    }

    /**
     * Check that expedition code is not already in the Database
     *
     * @param expeditionCode
     *
     * @return
     */
    private boolean isExpeditionCodeAvailable(String expeditionCode, Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT count(*) as count " +
                    "FROM expeditions " +
                    "WHERE expeditionCode = ? AND " +
                    "projectId = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, expeditionCode);
            stmt.setInt(2, projectId);

            rs = stmt.executeQuery();
            rs.next();
            Integer count = rs.getInt("count");
            if (count >= 1) {
                return false;
            }
            return true;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }

    }

    /**
     * Return a JSON response of the user's expeditions in a project
     *
     * @param projectId
     * @param username
     *
     * @return
     */
    public String listExpeditions(Integer projectId, String username) {
        StringBuilder sb = new StringBuilder();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        sb.append("{\n");
        sb.append("\t\"expeditions\": [\n");
        Integer userId = db.getUserId(username);

        try {
            String sql = "SELECT expeditionId, expeditionTitle, expeditionCode, public " +
                    "FROM expeditions " +
                    "WHERE projectId = ? && userId = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, projectId);
            stmt.setInt(2, userId);

            rs = stmt.executeQuery();
            while (rs.next()) {
                sb.append("\t\t{\n");
                sb.append("\t\t\t\"expeditionId\":\"" + rs.getString("expeditionId") + "\",\n");
                sb.append("\t\t\t\"expeditionCode\":\"" + rs.getString("expeditionCode") + "\",\n");
                sb.append("\t\t\t\"expeditionTitle\":\"" + rs.getString("expeditionTitle") + "\",\n");
                sb.append("\t\t\t\"public\":\"" + rs.getBoolean("public") + "\"\n");
                sb.append("\t\t}");
                if (!rs.isLast())
                    sb.append(",\n");
                else
                    sb.append("\n");
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }

        sb.append("\t]\n}");

        return sb.toString();
    }

   /**
     * Return an HTML table of an expedition's configuration
     *
     * @param expeditionId
     *
     * @return
     */
    public String listExpeditionConfigurationAsTable(Integer expeditionId) {
        String rootName = sm.retrieveValue("rootName");
        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n");
        sb.append("\t<tbody>\n");

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT b.identifier, e.public, e.expeditionCode, e.projectId " +
                    "FROM bcids b, expeditionBcids eB, expeditions e " +
                    "WHERE b.bcidId = eB.bcidId && eB.expeditionId = e.expeditionId && e.expeditionId = ? and " +
                    "b.resourceType = \"http://purl.org/dc/dcmitype/Collection\"";
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, expeditionId);

            rs = stmt.executeQuery();
            while (rs.next()) {

                sb.append("\t<tr>\n");
                sb.append("\t\t<td>");
                sb.append("Identifier:");
                sb.append("\t\t</td>\n");
                sb.append("\t\t<td>");
                sb.append("<a href=\"/" + rootName + "/lookup.jsp?id=");
                sb.append(rs.getString("b.identifier"));
                sb.append("\">");
                sb.append(rs.getString("b.identifier"));
                sb.append("</a>");
                sb.append("\t\t</td>\n");
                sb.append("\t</tr>\n");

                sb.append("\t<tr>\n");
                sb.append("\t\t<td>");
                sb.append("Public Expedition:");
                sb.append("\t\t</td>");
                sb.append("\t\t<td>");
                if (rs.getBoolean("e.public")) {
                    sb.append("yes");
                } else {
                    sb.append("no");
                }
                sb.append("&nbsp;&nbsp;");
                sb.append("<a href='#' onclick=\"editExpedition('");
                sb.append(rs.getInt("e.projectId"));
                sb.append("', '");
                sb.append(rs.getString("e.expeditionCode"));
                sb.append("', this)\">edit</a>");
                sb.append("\t\t</td>");
                sb.append("\t</tr>\n");
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }

        sb.append("\t</tbody>\n");
        sb.append("</table>\n");
        return sb.toString();
    }

    /**
     * Return an HTML table of an expedition's resources
     *
     * @param expeditionId
     *
     * @return
     */
    public String listExpeditionResourcesAsTable(Integer expeditionId) {
        String rootName = sm.retrieveValue("rootName");
        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n");
        sb.append("\t<tbody>\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<th>Identifier</th>\n");
        sb.append("\t\t<th>Resource Type</th>\n");
        sb.append("\t</tr>\n");

        PreparedStatement stmt = null;
        ResultSet rs = null;
        ResourceTypes rts = new ResourceTypes();

        try {
            String sql = "SELECT b.identifier, b.resourceType " +
                    "FROM bcids b, expeditionBcids eB " +
                    "WHERE b.bcidId = eB.bcidId && eB.expeditionId = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, expeditionId);

            ResourceTypes rt = new ResourceTypes();

            rs = stmt.executeQuery();
            while (rs.next()) {
                String rtString;
                ResourceType resourceType = rt.get(rs.getString("b.resourceType"));
                if (resourceType != null) {
                    rtString = resourceType.string;
                } else {
                    rtString = rs.getString("b.resourceType");
                }

                // if the resourceType is a dataset or collection, don't add to table
                if (rts.get(1).equals(resourceType) || rts.get(38).equals(resourceType)) {
                    continue;
                }

                sb.append("\t<tr>\n");
                sb.append("\t\t<td>");
                sb.append("<a href=\"/" + rootName + "/lookup.jsp?id=");
                sb.append(rs.getString("b.identifier"));
                sb.append("\">");
                sb.append(rs.getString("b.identifier"));
                sb.append("</a>");
                sb.append("</td>\n");
                sb.append("\t\t<td>");
                // only display a hyperlink if http: is specified under resource type
                if (rs.getString("b.resourceType").contains("http:")) {
                    sb.append("<a href=\"" + rs.getString("b.resourceType") + "\">" + rtString + "</a>");
                } else {
                    sb.append(rtString);
                }
                sb.append("</td>\n");
                sb.append("\t</tr>\n");
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }

        sb.append("\t</tbody>\n");
        sb.append("</table>\n");
        return sb.toString();
    }

    /**
     * return an HTML table of an expedition's datasets
     *
     * @param expeditionId
     *
     * @return
     */
    public String listExpeditionDatasetsAsTable(Integer expeditionId) {
        String rootName = sm.retrieveValue("rootName");
        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<th>Date</th>\n");
        sb.append("\t\t<th>Identifier</th>\n");
        sb.append("\t</tr>\n");

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT b.ts, b.identifier, b.webAddress, b.graph, e.projectId " +
                    "FROM bcids b, expeditionBcids eB, expeditions e " +
                    "WHERE b.bcidId = eB.bcidId && eB.expeditionId = ? && e.expeditionId = eB.expeditionId " +
                    "AND b.resourceType = \"http://purl.org/dc/dcmitype/Dataset\" " +
                    "ORDER BY b.ts DESC";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, expeditionId);

            rs = stmt.executeQuery();
            while (rs.next()) {
                sb.append("\t<tr>\n");
                sb.append("\t\t<td>");
                sb.append(rs.getTimestamp("b.ts").toString());
                sb.append("\t\t</td>");

                sb.append("\t\t<td>");
                sb.append("<a href=\"/" + rootName + "/lookup.jsp?id=");
                sb.append(rs.getString("b.identifier"));
                sb.append("\">");
                sb.append(rs.getString("b.identifier"));
                sb.append("</a>");
                sb.append("\t\t</td>");
                sb.append("\t</tr>\n");
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }

        sb.append("</table>\n");
        return sb.toString();
    }

    /**
     * Return an HTML Table of the expeditions associated with a project. Includes who owns the expedition,
     * the expedition title, and whether the expedition is public.  This information is returned as information
     * typically viewed by an Admin who wants to see details about what datasets are as part of an expedition
     *
     * @param projectId
     * @param username  the project's admins username
     *
     * @return
     */
    public String listExpeditionsAsTable(Integer projectId, String username) {
        StringBuilder sb = new StringBuilder();
        sb.append("<form method=\"POST\">\n");
        sb.append("<table>\n");
        sb.append("<tbody>\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<th>Username</th>\n");
        sb.append("\t\t<th>Expedition Title</th>\n");
        sb.append("\t\t<th>Public</th>\n");
        sb.append("\t</tr>\n");

        PreparedStatement stmt = null;
        ResultSet rs = null;

        ProjectMinter p = new ProjectMinter();
        try {
            Integer userId = db.getUserId(username);

            if (!p.userProjectAdmin(userId, projectId)) {
                throw new ForbiddenRequestException("You must be this project's admin to view its expeditions.");
            }

            String sql = "SELECT max(b.bcidId) bcidId, e.expeditionTitle, e.expeditionId, e.public, u.username \n" +
                    " FROM expeditions as e, users as u, bcids b, expeditionBcids eB \n" +
                    " WHERE \n" +
                    " \te.projectId = ? \n" +
                    " \tAND u.userId = e.userId \n" +
                    " \tAND b.bcidId = eB.bcidId\n" +
                    " \tAND eB.expeditionId = e.expeditionId \n" +
                    " \tAND b.resourceType = \"http://purl.org/dc/dcmitype/Dataset\" \n" +
                    " GROUP BY eB.expeditionId";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);

            rs = stmt.executeQuery();

            while (rs.next()) {
                sb.append("\t<tr>\n");
                sb.append("\t\t<td>");
                sb.append(rs.getString("u.username"));
                sb.append("</td>\n");
                sb.append("\t\t<td>");
                sb.append(rs.getString("e.expeditionTitle"));
                sb.append("</td>\n");
                sb.append("\t\t<td><input name=\"");
                sb.append(rs.getInt("e.expeditionId"));
                sb.append("\" type=\"checkbox\"");
                if (rs.getBoolean("e.public")) {
                    sb.append(" checked=\"checked\"");
                }
                sb.append("/></td>\n");
                sb.append("\t</tr>\n");
            }

            sb.append("\t<tr>\n");
            sb.append("\t\t<td></td>\n");
            sb.append("\t\t<td><input type=\"hidden\" name=\"projectId\" value=\"" + projectId + "\" /></td>\n");
            sb.append("\t\t<td><input id=\"expeditionForm\" type=\"button\" value=\"Submit\"></td>\n");
            sb.append("\t</tr>\n");

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            p.close();
            db.close(stmt, rs);
        }

        sb.append("</tbody>\n");
        sb.append("</table>\n");
        sb.append("</form>\n");
        return sb.toString();
    }

    /**
     * Update the public status of a specific expedition
     */
    public Boolean updateExpeditionPublicStatus(Integer userId, String expeditionCode, Integer projectId, Boolean publicStatus) {
        // Check to see that this user owns this expedition
        if (!userOwnsExpedition(userId, expeditionCode, projectId)) {
            throw new ForbiddenRequestException("You must be the owner of this expedition to update the public status.");
        }

        PreparedStatement updateStatement = null;

        try {
            String updateString = "UPDATE expeditions SET public = ?" +
                    " WHERE expeditionCode = \"" + expeditionCode + "\" AND projectId = " + projectId;

            updateStatement = conn.prepareStatement(updateString);
            updateStatement.setBoolean(1, publicStatus);

            updateStatement.execute();
            if (updateStatement.getUpdateCount() > 0)
                return true;
            else
                return false;

        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException while updating expedition public status.", e);
        } finally {
            db.close(updateStatement, null);
        }
    }

    /**
     * Update the public attribute of each expedition in the expeditions MultivaluedMap
     *
     * @param expeditions
     * @param projectId
     *
     * @return
     */
    public void updateExpeditionsPublicStatus(MultivaluedMap<String, String> expeditions, Integer projectId) {
        List<String> updateExpeditions = new ArrayList<String>();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT expeditionId, public FROM expeditions WHERE projectId = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);

            rs = stmt.executeQuery();

            while (rs.next()) {
                String expeditionId = rs.getString("expeditionId");
                if (expeditions.containsKey(expeditionId) &&
                        !expeditions.getFirst(expeditionId).equals(String.valueOf(rs.getBoolean("public")))) {
                    updateExpeditions.add(expeditionId);
                }
            }

            if (!updateExpeditions.isEmpty()) {
                String updateString = "UPDATE expeditions SET" +
                        " public = CASE WHEN public ='0' THEN '1' WHEN public = '1' THEN '0' END" +
                        " WHERE expeditionId IN (" + updateExpeditions.toString().replaceAll("[\\[\\]]", "") + ")";

//                System.out.print(updateString);
                db.close(stmt, null);
                stmt = conn.prepareStatement(updateString);

                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new ServerErrorException("Db error while updating Expeditions public status.", e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * checks the status of a new expedition code on the server and directing consuming
     * applications on whether this user owns the expedition and if it exists within an project or not.
     * Responses are error, update, or insert
     *
     * @param expeditionCode
     * @param projectId
     * @param ignoreUser
     * @param userId
     * @return
     */
    public String validateExpedition(String expeditionCode, Integer projectId, Boolean ignoreUser, Integer userId) {
        // Default the lIgnore_user variable to false.  Set if true only if user specified it
        Boolean lIgnore_user = false;
        if (ignoreUser != null && ignoreUser) {
            lIgnore_user = true;
        }

        //Check that the user exists in this project
        if (!userExistsInProject(userId, projectId)) {
            // If the user isn't in the project, then we can't update or create a new expedition
            throw new ForbiddenRequestException("User is not authorized to update/create expeditions in this project.");
        }

        // If specified, ignore the user.. simply figure out whether we're updating or inserting
        if (lIgnore_user) {
            if (expeditionExistsInProject(expeditionCode, projectId)) {
                return "{\"update\": \"update this expedition\"}";
            } else {
                return "{\"insert\": \"insert new expedition\"}";
            }
        }

        // Else, pay attention to what user owns the initial project
        else {
            if (userOwnsExpedition(userId, expeditionCode, projectId)) {
                // If the user already owns the expedition, then great--- this is an update
                return "{\"update\": \"user owns this expedition\"}";
                // If the expedition exists in the project but the user does not own the expedition then this means we can't
            } else if (expeditionExistsInProject(expeditionCode, projectId)) {
                throw new ForbiddenRequestException("The expedition code '" + expeditionCode +
                        "' exists in this project already and is owned by another user. " +
                        "Please choose another expedition code.");
            } else {
                return "{\"insert\": \"the expedition does not exist with project and nobody owns it\"}";
            }
        }
    }

    public boolean isPublic(String expeditionCode, Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT public FROM expeditions WHERE expeditionCode = ? AND projectId = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, expeditionCode);
            stmt.setInt(2, projectId);

            rs = stmt.executeQuery();
            rs.next();
            return rs.getBoolean("public");
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
    }
}
