package bcid;

import bcidExceptions.BCIDException;
import bcidExceptions.ServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.SettingsManager;
import util.timer;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

/**
 * The elementMinter class brokers interactions between elements and groups and a mysql backend database.
 * The  elementMinter extends dataset and thus relies
 * on the notion of a dataset to be in existence before creating ANY identifier.
 */
public class elementMinter extends dataGroupMinter {


    // some number to start with
    // This could be 0 in most cases, but for testing and in cases where we already created a test encoded EZID
    // this can be set manually here to bypass existing EZIDS
    private Integer startingNumber;
    private String publisher;
    private String creator;

    private static Logger logger = LoggerFactory.getLogger(elementMinter.class);

    static int TRUE = 1;
    static int FALSE = 0;
    static SettingsManager sm;

    static {
        sm = SettingsManager.getInstance();
        sm.loadProperties();
    }

    /**
     * TEST Case, uses test case dataset.
     */
    public elementMinter(boolean ezidRequest, Boolean suffixPassThrough) {
        super(ezidRequest, suffixPassThrough);
        init();
    }

    public elementMinter() {
        super();
        init();
    }

    public String getPublisher() {
        return publisher;
    }

    public String getCreator() {
        return creator;
    }


    /**
     * This constructor is used to prepare for minting BCID data elements when the dataset is known by
     * its NAAN + shoulder
     *
     * @param NAAN
     * @param shoulder
     * @param ezidRequest
     */
    public elementMinter(Integer NAAN, String shoulder, boolean ezidRequest, Boolean suffixPassThrough) {
        super(NAAN, shoulder, ezidRequest, suffixPassThrough);
        init();
    }

    /**
     * This constructor is used to prepare for minting BCID data elements when the dataset is known by its dataset_id
     */
    public elementMinter(Integer dataset_id) {
        super(dataset_id);
        init();
    }

    /**
     * Initialize stuff
     */
    private void init() {
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        startingNumber = Integer.parseInt(sm.retrieveValue("bcidStartingNumber"));

        publisher = sm.retrieveValue("publisher");
        if (publisher == null || publisher.trim().equalsIgnoreCase("")) {
            publisher = "Biocode FIMS System";
        }

        creator = sm.retrieveValue("creator");
        if (creator.trim().equalsIgnoreCase("")) {
            creator = null;
        }
    }


    /**
     * Set the starting number for AutoIncrement
     */
    private void setAutoIncrement() {
        String alterString = "ALTER TABLE identifiers AUTO_INCREMENT = ?";
        PreparedStatement alterStatement = null;
        try {
            alterStatement = conn.prepareStatement(alterString);
            alterStatement.setInt(1, startingNumber);
            alterStatement.executeUpdate();
        } catch (SQLException e) {
            //TODO should we silence this exception?
            logger.warn("SQLException trying to set AUTO_INCREMENT for identifiers table.", e);
        } finally {
            db.close(alterStatement, null);
        }
    }


    /**
     * Get a UniqueDataSetID to identify the set of elements inserted at one time
     *
     * @return
     */
    private String generateUUIDString() {
        return UUID.randomUUID().toString();
    }

    /**
     * Delete identifiers in table for a particular loadedSetuuid
     *
     * @param uuid
     *
     * @return an int representing results from the executeUpdate command, i believe indicating the number
     * of items deleted.
     */
    public int deleteLoadedSetUUID(String uuid) {
        PreparedStatement stmt = null;
        try {
            String sql = "DELETE FROM identifiers WHERE loadedSetUUID=?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, uuid);

            return stmt.executeUpdate();
        } catch (SQLException e) {
            //TODO should we silence this exception?
            logger.warn("SQLException trying to delete loadedSetUUID: {} from identifiers table.", uuid, e);
        } finally {
            db.close(stmt, null);
        }
        return 0;
    }

    /**
     * Mint a Single element
     * Takes an element object and inserts these objects into database.
     * Inserts element class members: DOI, webAddress, sourceID, resourceType
     *
     * @param b
     */
    public void mint(bcid b) {
        ArrayList<bcid> arrayList = new ArrayList<bcid>();
        arrayList.add(b);

        mintList(arrayList);
    }

    /**
     * Mint a group of elements (see single element for further explanation)
     *
     * @param elementList
     *
     * @return returns a DatasetIdentifier String
     */
    public String mintList(ArrayList elementList) {
        timer t = new timer();

        t.lap("begin mintList");

        // First validate the list before doing anything if this is uuids
        // TODO: check for slashes and bad characters in the suffix-- these are not allowed
        if (this.getSuffixPassThrough()) {
            Iterator validateIds = elementList.iterator();
            while (validateIds.hasNext()) {
                bcid id = (bcid) validateIds.next();
                // TODO: add back in validation of UUIDs, for now, this issue presents problems in determing what is a UUID or not!
                //if (!validateUUID(id.sourceID)) {
                //    throw new Exception("One or more invalid Identifiers, violating either checksum, uuid construction, or Uniqueness of uuid rules: " + id.sourceID);
                //}
            }
        }

        // A single ID to refer to this list of elements that are being inserted in the database
        String loadedSetUUID = this.generateUUIDString();

        // Turn off autocommits just for this method
        //conn.setAutoCommit(false);
        PreparedStatement insertStatement = null;
        try {
            int insertSize = 1000;
            if (insertSize > elementList.size()) {
                insertSize = elementList.size();
            }

            // Begin prepared statement logic
            insertStatement = conn.prepareStatement(formatSQL(insertSize));
            Iterator ids = elementList.iterator();
            int rowcount = 0;
            int paramcount = 1;
            // Loop through identifiers
            while (ids.hasNext()) {
                bcid id = (bcid) ids.next();

                if (id.webAddress != null)
                    insertStatement.setString(paramcount++, id.webAddress.toString());
                else
                    insertStatement.setString(paramcount++, null);

                insertStatement.setString(paramcount++, id.sourceID);
                insertStatement.setString(paramcount++, loadedSetUUID.toString());
                insertStatement.setInt(paramcount++, id.dataset_id);

                // Execute a commit at every so many number of rows
                if ((rowcount + 1) % insertSize == 0) {
                    insertStatement.execute();
                    insertStatement.clearParameters();
                    // Adjust the size of the next prepared statement
                    if (insertSize >= (elementList.size() - rowcount)) {
                        int lastSectionSize = elementList.size() - (rowcount + 1);
                        insertStatement = conn.prepareStatement(formatSQL(lastSectionSize));
                    }
                    paramcount = 1;
                }
                rowcount++;
            }
            // Finish up, last statement
            if (paramcount > 1) {
                insertStatement.execute();
            }

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(insertStatement, null);
        }
        t.lap("end mintList");
        return loadedSetUUID.toString();
    }

    /**
     * Special function to prepare SQL statement
     *
     * @param rowcount
     *
     * @return
     */
    private String formatSQL(Integer rowcount) {
        StringBuffer sql = new StringBuffer("INSERT INTO identifiers ( webaddress, localid, loadedSetUUID, datasets_id) " +
                "values (?,?,?,?)");
        for (int i = 1; i < rowcount; i++) {
            sql.append(",(?,?,?,?)");
        }
        return sql.toString();
    }

    /**
     * Returns an arrayList of encoded identifiers given a dataset identifier
     * The dataset identifier indicates a batch of identifers added all at the same time
     *
     * @param datasetUUID
     *
     * @return An ArrayList of identifiers
     */

    public ArrayList getIdentifiers(String datasetUUID) {
        ArrayList results = new ArrayList();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT " +
                    "i.identifiers_id as id," +
                    "d.prefix as prefix," +
                    "i.localid as localid" +
                    " FROM identifiers as i, datasets as d " +
                    " WHERE i.loadedSetUUID = ?" +
                    " AND i.datasets_id=d.datasets_id";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, datasetUUID);
            rs = stmt.executeQuery();

            while (rs.next()) {
                // If this is suffixPassthrough then use prefix + localid
                if (this.getSuffixPassThrough()) {
                    results.add(rs.getString("prefix") + sm.retrieveValue("divider") + rs.getString("localid"));
                    // else use the current encode function
                } else {
                    results.add(new elementEncoder(prefix).encode(new BigInteger(rs.getString("id"))));
                }
            }

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
        return results;
    }

    /**
     * Return the next available start number from the mysql database as a BigInteger
     * Note that this method is probably not needed with the Mysql Auto_Increment
     *
     * @return
     *
     * @throws BCIDException
     */
    private BigInteger start() throws BCIDException {
        BigInteger big = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT max(identifiers_id) as maxid from identifiers";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            if (rs.next()) {

                try {
                    big = new BigInteger(rs.getString("maxid"));
                } catch (NullPointerException e) {
                    // In case this is the very first, returning NULL means there are no integers here
                    // start with some number, set by class
                    big = new BigInteger(startingNumber.toString());
                }

            } else {
                throw new BCIDException();
            }

            // Add 1 to the start value
            return big.add(new BigInteger("1"));

        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "Unable to find start", e);
        } finally {
            db.close(stmt, rs);
        }
    }


    /**
     * Create a bunch of bcids.  These are not yet EZIDs but we will create a place-holder for them
     * in our database.  If the user that requests these returns to the triplifier then we will turn them
     * into EZIDs for them at that time.  As such, all that is needed to register these is an email address
     * of a responsible party.
     *
     * @param numIdentifiers
     *
     * @return An ArrayList of all the GUIDs
     *
     * @throws java.net.URISyntaxException
     */
    public String createBCIDs(int numIdentifiers, URI what) throws URISyntaxException {
        String datasetIdentifier = this.generateUUIDString();

        PreparedStatement insertStatement = null;
        try {
            // Turn off autocommits just for this method
            conn.setAutoCommit(false);

            // Use auto increment in database to assign the actual identifier.. this is threadsafe this way
            // Also, use auto date assignment feature for when this was applied.
            String insertString = "INSERT INTO identifiers(ezidRequest,loadedSetUUID, datasets_id) " +
                    "values (?,?,?)";
            insertStatement = conn.prepareStatement(insertString);

            int count = 0;
            while (count < numIdentifiers) {
                insertStatement.setInt(1, FALSE);
                // insertStatement.setString(2, what.toString());
                insertStatement.setString(2, datasetIdentifier.toString());
                insertStatement.setInt(3, this.getDatasets_id());
                insertStatement.addBatch();
                // Execute a commit at every 1000 rows
                if (count + 1 % 1000 == 0) {
                    insertStatement.executeBatch();
                    conn.commit();
                }
                count++;
            }
            // Execute remainder as batch
            insertStatement.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(insertStatement, null);
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.warn("SQLException thrown while trying to setAutoCommit(true).", e);
            }
        }

        return datasetIdentifier.toString();
    }


    /**
     * validate uuid, to make sure it conforms to the generic structure expected of uuids
     *
     * @param uuid
     *
     * @return
     */
    private boolean validateUUID(String uuid) {
        if (uuid == null) return false;
        if (uuid.matches("[0-9a-zA-Z]{8}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{12}"))
            return true;
        return false;
    }

    /**
     * Generate an ARK version for this uuid, by appending the uuid onto the ARK prefix
     *
     * @param uuidAsString
     *
     * @return A full ARK representation of this ID
     *
     * @throws BCIDException
     */
    public String createUUIDARK(String uuidAsString) throws BCIDException {
        // Validate this UUID
        if (!validateUUID(uuidAsString)) {
            throw new BCIDException("Invalid uuid: " + uuidAsString);
        }
        return prefix + sm.retrieveValue("divider") + UUID.fromString(uuidAsString).toString();
    }

    /**
     * Use main function for testing
     *
     * @param args
     */
    public static void main(String args[]) {
        SettingsManager sm = SettingsManager.getInstance();
        try {
            sm.loadProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Integer naan = new Integer(sm.retrieveValue("bcidNAAN"));

        // Create the shoulder
        dataGroupMinter minterDataset = null;

        /*
minterDataset = new dataset();
minterDataset.mint(
        naan,
        1,
        new ResourceTypes().RESOURCE,
        null,
        null,
        null);
        */
        minterDataset = new dataGroupMinter();
        System.out.println("Using dataset  = " + minterDataset.prefix);
        //} catch (URISyntaxException e) {
        //    e.printStackTrace();

        // Check to see if this exists
        System.out.println(minterDataset.getDatasetId(minterDataset.prefix));

        // Encode an BigInteger
        String value = "105";
        System.out.println("Encode BigInteger = " + value);

        String myIdentifier = new elementEncoder(minterDataset.prefix).encode(new BigInteger(value));
        System.out.println("  " + myIdentifier);
        //myIdentifier = "ark:/99999/fk4/aQH";
        // Decode an Identifier
        System.out.println("Decode Identifier = " + myIdentifier);
        try {
            BigInteger bigInt = new elementEncoder(minterDataset.prefix).decode(myIdentifier);
            System.out.println("  Decoded BCID Value = " + bigInt);
            System.out.println("  Decoded dataset Value = " + new dataGroupEncoder().decode(myIdentifier));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Decode an Identifier with LocalID
        System.out.println("Decode element = " + myIdentifier);
        try {
            BigInteger bigInt = new elementEncoder(minterDataset.prefix).decode(myIdentifier);
            System.out.println("  Decoded Value = " + bigInt);
        } catch (Exception e) {
            e.printStackTrace();
        }

        elementMinter minter = null;
        // Create EZID w/ uuid on end
        try {
            minter = new elementMinter(naan, minterDataset.shoulder, false, true);
            String uuid = "09d5a2c3-e166-4bc0-a3ab-69ed0e3e9616";
            System.out.println("Creating an ARK w/ uuid: " + uuid);
            String uuidArk = minter.createUUIDARK(uuid);
            System.out.println("  " + uuidArk);
        } catch (Exception e) {
            e.printStackTrace();
        }

        minter.close();
        minterDataset.close();
    }
}
