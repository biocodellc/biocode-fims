package bcid;

import bcid.Renderer.JSONRenderer;
import bcid.Renderer.Renderer;
import fimsExceptions.ServerErrorException;
import ezid.EZIDException;
import ezid.EZIDService;
import utils.SettingsManager;
import utils.timer;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Resolves any incoming identifier to the BCID and/or EZID systems.
 * Resolver first checks if this is a data group.  If so, it then checks if there is a decodable BCID.  If not,
 * then check if there is a suffix and if THAT is resolvable.
 */
public class resolver extends database {
    String ark = null;
    String scheme = "ark:";
    String naan = null;
    String shoulder = null;     // The data group
    String sourceID = null;        // The local identifier
    BigInteger element_id = null;
    Integer datagroup_id = null;
    public boolean forwardingResolution = false;
    public String graph = null;
    static SettingsManager sm;

    /**
     * Load settings manager, set ontModelSpec.
     */
    static {
        // Initialize settings manager
        sm = SettingsManager.getInstance();
        sm.loadProperties();
    }

    private String project;

    /**
     * Pass an ARK identifier to the resolver
     *
     * @param ark
     */
    public resolver(String ark) {
        super();


        this.ark = ark;
        // Pull off potential last piece of string which would represent the local Identifier
        // The piece to decode is ark:/NAAN/bcidIdentifer (anything else after a last trailing "/" not decoded)
        StringBuilder stringBuilder = new StringBuilder();

        String bits[] = ark.split("/", 3);
        // just want the first chunk between the "/"'s
        naan = bits[1];
        // Now decipher the shoulder and sourceID in the next bit
        setShoulderAndSourceID(bits[2]);
        // Call setDataGroup() to set datagroup_id
        setDataGroup();
    }

    /**
     * Find the appropriate BCID ROOT for this expedition given an conceptAlias.
     *
     * @param expedition_code defines the BCID expedition_code to lookup
     * @param conceptAlias    defines the alias to narrow this,  a one-word reference denoting a BCID
     *
     * @return returns the BCID for this expedition and conceptURI combination
     */
    public resolver(String expedition_code, Integer project_id, String conceptAlias) {
        ResourceTypes resourceTypes = new ResourceTypes();
        ResourceType rt = resourceTypes.getByShortName(conceptAlias);
        String uri = rt.uri;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String query = "select \n" +
                    "d.prefix as prefix \n" +
                    "from \n" +
                    "datasets d, expeditionsBCIDs pb, expeditions p \n" +
                    "where \n" +
                    "d.datasets_id=pb.datasets_id && \n" +
                    "pb.expedition_id=p.expedition_id && \n" +
                    "p.expedition_code= ? && \n" +
                    "p.project_id = ? && \n" +
                    "(d.resourceType=? || d.resourceType= ?)";
            stmt = conn.prepareStatement(query);

            stmt.setString(1, expedition_code);
            stmt.setInt(2, project_id);
            stmt.setString(3, uri);
            stmt.setString(4, conceptAlias);


            //System.out.println("resolver query = " + query);
            rs = stmt.executeQuery();
            rs.next();
            this.ark = rs.getString("prefix");
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            close(stmt, rs);
        }
    }


    public String getArk() {
        return ark;
    }

    /**
     * Return an identifier representing a data set
     *
     * @return
     */
    public Integer getDataGroupID() {
        return datagroup_id;
    }

    /**
     * Return an identifier representing a data element
     *
     * @return
     */
    public BigInteger getElementID() {
        return element_id;
    }

    /**
     * Set the shoulder and sourceID variables for this ARK
     *
     * @param a
     */
    private void setShoulderAndSourceID(String a) {
        boolean reachedShoulder = false;
        StringBuilder sbShoulder = new StringBuilder();
        StringBuilder sbSourceID = new StringBuilder();

        for (int i = 0; i < a.length(); i++) {
            char c = a.charAt(i);
            if (!reachedShoulder)
                sbShoulder.append(c);
            else
                sbSourceID.append(c);
            if (Character.isDigit(c))
                reachedShoulder = true;
        }
        shoulder = sbShoulder.toString();
        sourceID = sbSourceID.toString();

        // String the slash between the shoulder and the sourceID
        if (!sm.retrieveValue("divider").equals("")) {
            if (sourceID.startsWith(sm.retrieveValue("divider"))) {
                sourceID = sourceID.substring(1);
            }
        }
    }

    /**
     * Attempt to resolve a particular ARK.  If there is no webaddress defined for resolution then
     * it points to the biscicol.org/bcid homepage.
     *
     * @return JSON String with content for the interface
     */
    public URI resolveARK() throws URISyntaxException {
        bcid bcid = null;
        URI resolution = null;

        // First  option is check if dataset, then look at other options after this is determined
        if (isDataGroup()) {
            bcid = new bcid(sourceID, datagroup_id);

            String resolutionTarget = "";
            if (bcid.getResolutionTarget() != null) {
                resolutionTarget = bcid.getResolutionTarget().toString().trim();
            }

            // Group has a specified resolution target
            if (bcid.getResolutionTarget() != null && !resolutionTarget.equals("")) {
                // A resolution target is specified AND there is a sourceID
                if (sourceID != null && bcid.getResolutionTarget() != null && !sourceID.trim().equals("") && !bcid.getResolutionTarget().equals("")) {
                    forwardingResolution = true;

                    // Immediately return resolution result
                    return new URI(bcid.getResolutionTarget() + sourceID);
                }
                // If the database indicates this is a suffixPassthrough dataset then return the MetadataTarget
                else if (bcid.getDatasetsSuffixPassthrough()) {
                    resolution = bcid.getMetadataTarget();
                }
                // If there is some resolution target then return that
                else if (bcid.getResolutionTarget() != null && !bcid.getResolutionTarget().toString().equalsIgnoreCase("null")) {
                    forwardingResolution = true;
                    return bcid.getResolutionTarget();
                }
                // All other cases just return metadata
                else {
                    resolution = bcid.getMetadataTarget();
                }
            }
            // This is a group and no resolution target is specified then just return metadata.
            else {
                resolution = bcid.getMetadataTarget();
            }

        }

        // Set the graph variable
        this.graph = bcid.getGraph();

        // Debugging:
        //System.out.println("datagroup_id = " + datagroup_id);

       // There are cases where project can be null, don't get caught on exception
        try {
            this.project = getProjectID(datagroup_id);
        } catch (Exception e) {
            // do nothing, project is just null?
        }

        // Project is empty after this call!
        //System.out.println("project = " + this.project);

        return resolution;
    }

    /**
     * Print Metadata for a particular ARK
     *
     * @return JSON String with content for the interface
     */
    public String printMetadata(Renderer renderer) {
        GenericIdentifier bcid = null;

        // First  option is check if dataset, then look at other options after this is determined
        if (setDataGroup()) {

            bcid = new bcid(datagroup_id);

            // Has a registered, resolvable suffix
            //if (isResolvableSuffix(datagroup_id)) {
            //    bcid = new bcid(element_id, ark);
            //}
            // Has a suffix, but not resolvable
            //else {
            try {
                if (sourceID != null && bcid.getResolutionTarget() != null) {
                    bcid = new bcid(sourceID, bcid.getResolutionTarget(), datagroup_id);
                }
            } catch (URISyntaxException e) {
                //TODO should we silence this exception?
                logger.warn("URISyntaxException thrown", e);
            }
            //}

        }
        return renderer.render(bcid);
    }

    /**
     * Determine if this ARK has a matching localID
     *
     * @return
     */
    /* private boolean isLocalID() {
     String select = "SELECT identifiers_id FROM identifiers " +
             "where i.localid = " + ark;
     Statement stmt = null;
     try {
         stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(select);
         rs.next();
         // TODO: enable returning multiple possible identifiers here
         element_id = new BigInteger(rs.getString("identifiers_id"));
         return true;
     } catch (SQLException e) {
         return false;
     }
 }   */

    /**
     * Resolve an EZID version of this ARK
     *
     * @param ezidService
     *
     * @return JSON string to send to interface
     */
    public String resolveEZID(EZIDService ezidService, Renderer renderer) {
        // First fetch from EZID, and populate a map
        GenericIdentifier ezid = null;

        try {
            ezid = new ezid(ezidService.getMetadata(ark));
        } catch (EZIDException e) {
            //TODO should we silence this exception?
            logger.warn("URISyntaxException thrown", e);
        }
        return renderer.render(ezid);
    }


    /**
     * Resolve identifiers through BCID AND EZID -- This method assumes JSONRenderer
     *
     * @param ezidService
     *
     * @return JSON string with information about BCID/EZID results
     */
    public String resolveAllAsJSON(EZIDService ezidService) {
        timer t = new timer();
        Renderer renderer = new JSONRenderer();
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        try {
            sb.append("  " + this.resolveARK().toString());
        } catch (URISyntaxException e) {
            //TODO should we silence this exception?
            logger.warn("URISyntaxException thrown", e);
        }
        t.lap("resolveARK");
        sb.append("\n  ,\n");
        sb.append("  " + this.resolveEZID(ezidService, renderer));
        t.lap("resolveEZID");
        sb.append("\n]");
        return sb.toString();
    }

    private boolean isDataGroup() {
        if (datagroup_id != null)
            return true;
        else
            return false;
    }

    /**
     * Check if this is a dataset and set the datasets_id
     *
     * @return
     */
    private boolean setDataGroup() {
        // Test Dataset is #1
        if (shoulder.equals("fk4") && naan.equals("99999")) {
            datagroup_id = 1;
            return true;
        }

        // Decode a typical dataset
        datagroup_id = new dataGroupEncoder().decode(shoulder).intValue();

        if (datagroup_id == null) {
            return false;
        } else {
            // Now we need to figure out if this datasets_id exists or not in the database
            String select = "SELECT count(*) as count FROM datasets where datasets_id = ?";
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                stmt = conn.prepareStatement(select);
                stmt.setInt(1, datagroup_id);
                rs = stmt.executeQuery();
                rs.next();
                int count = rs.getInt("count");
                if (count < 1) {
                    datagroup_id = null;
                    return false;
                } else {
                    return true;
                }
            } catch (SQLException e) {
                throw new ServerErrorException(e);
            } finally {
                close(stmt, rs);
            }
        }
    }

    /**
     * Get the projectId given a dataset_id
     *
     * @param datasets_id
     */
    public String getProjectID(Integer datasets_id) throws Exception {
        String project_id = "";
        String sql = "select p.project_id from projects p, expeditionsBCIDs eb, expeditions e, " +
                "datasets d where d.datasets_id = eb.datasets_id and e.expedition_id=eb.`expedition_id` " +
                "and e.`project_id`=p.`project_id` and d.datasets_id= ?";

        System.out.println("sql = " + sql + "    datasets_id = " + datasets_id);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, datasets_id);
            rs = stmt.executeQuery();
            rs.next();
            project_id = rs.getString("project_id");


        } catch (SQLException e) {
            throw new ServerErrorException("Server Error",
                    "Exception retrieving projectCode for dataset: " + datasets_id, e);
        } finally {
            close(stmt, rs);
        }

        System.out.println(project_id);
        return project_id;
    }

    /**
     * Tell us if this ARK is a BCID that has an individually resolvable suffix.  This means that the user has
     * registered the identifier and provided a specific target URL
     *
     * @return
     */
    private boolean isResolvableSuffix(Integer d) {
        // Only attempt this method if the sourceID has some content, else we know there is no suffix
        if (sourceID != null && !sourceID.equals("")) {
            // Establish database connection so we can lookup suffixes here
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                String select = "SELECT identifiers_id FROM identifiers where datasets_id = " + d + " && localid = ?";
                stmt = conn.prepareStatement(select);
                stmt.setString(1, sourceID);
                rs = stmt.executeQuery();
                rs.next();
                element_id = new BigInteger(rs.getString("identifiers_id"));
            } catch (SQLException e) {
                throw new ServerErrorException(e);
            } finally {
                close(stmt, rs);
            }
        }
        if (element_id == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Tell us if this ARK is a BCID by decoding the ARK itself and determining if we can
     * assign an integer to it.  This then is a native BCID that uses character encoding.
     *
     * @return
     */
    /*  private boolean isElement(int datasets_id) {

         String bow = scheme + "/" + naan + "/";
         String prefix = bow + shoulder;
         // if prefix and ark the same then just return false!
         if (prefix.equals(ark)) {
             return false;
         }
         BigInteger bigInt = null;

         // Look at Check Digit, a BCID should validate here... if the check-digit doesn't work its not a BCID
         // We do the check-digit function first since this is faster than looking it up in the database and
         // if it is bad, we will know right away.
         try {
             bigInt = new elementEncoder(prefix).decode(ark);
         } catch (Exception e) {
             return false;
         }

         // Now, see if this exists in the database
         try {
             element_id = bigInt;
             // First test is to see if this is a valid number
             if (bigInt.signum() == 1) {
                 // Now test to see if this actually exists in the database.
                 try {
                     String select = "SELECT count(*) as count FROM identifiers where identifiers_id = " + bigInt +
                             " && datasets_id = " + datasets_id;
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(select);
                     rs.next();
                     if (rs.getInt("count") > 0)
                         return true;
                     else
                         return false;
                 } catch (Exception e) {
                     return false;
                 }

             } else {
                 return false;
             }
         } catch (Exception e) {
             e.printStackTrace();
             return false;
         }
     }
    */

    /**
     * Main function for testing.
     *
     * @param args
     */
    public static void main(String args[]) {
        resolver r = null;
        SettingsManager sm = SettingsManager.getInstance();
        try {
            sm.loadProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //r = new resolver("ark:/21547/S2MBIO56");
            r = new resolver("ark:/21547/fR2");
            System.out.println("  " + r.resolveARK());
            System.out.println(r.resolveArkAs("tab"));

        } catch (Exception e) {
            e.printStackTrace();
        }
         /*
        try {
            r = new resolver("ark:/87286/C2/64c82d19-6562-4174-a5ea-e342eae353e8");
            System.out.println("  " + r.resolveARK());
        } catch (Exception e) {
            e.printStackTrace();
        }
          */

        try {
            String expected = "";
            // suffixpassthrough = 1; no webAddress specified; has a SourceID
            /*r = new resolver("ark:/87286/U264c82d19-6562-4174-a5ea-e342eae353e8");
            expected = "http://biscicol.org/id/metadata/ark:/21547/U264c82d19-6562-4174-a5ea-e342eae353e8";
            System.out.println(r.resolveARK());
                  */
            // suffixPassthrough = 1; webaddress specified; has a SourceID
            //r = new resolver("ark:/21547/R2");
            //System.out.println(r.printMetadata(new RDFRenderer()));
            //System.out.println(r.resolveARK());

            //System.out.println(r.resolveARK());
            //expected = "http://biocode.berkeley.edu/specimens/MBIO56";
            //System.out.println(r.printMetadata(new RDFRenderer()));

           /* r = new resolver("DEMO4",18,"Resource");
            //System.out.println(r.resolveARK());
           System.out.println(r.getArk());
           */
                 /*
            // suffixPassthrough = 1; webaddress specified; no SourceID
            r = new resolver("ark:/21547/R2");
            expected = "http://biscicol.org/id/metadata/ark:/21547/R2";
            System.out.println(r.resolveARK());

            // suffixPassthrough = 0; no webaddress specified; no SourceID
            r = new resolver("ark:/21547/W2");
            expected = "http://biscicol.org/id/metadata/ark:/21547/W2";
            System.out.println(r.resolveARK());

            // suffixPassthrough = 0; webaddress specified; no SourceID
            r = new resolver("ark:/21547/Gk2");
            expected =  "http://biscicol.org:3030/ds?graph=urn:uuid:77806834-a34f-499a-a29f-aaac51e6c9f8";
            System.out.println(r.resolveARK());

               // suffixPassthrough = 0; webaddress specified;  sourceID specified (still pass it through
            r = new resolver("ark:/21547/Gk2FOO");
            expected =  "http://biscicol.org:3030/ds?graph=urn:uuid:77806834-a34f-499a-a29f-aaac51e6c9f8FOO";
            System.out.println(r.resolveARK());
                 */
            // EZIDService service = new EZIDService();
            // service.login(sm.retrieveValue("eziduser"), sm.retrieveValue("ezidpass"));
            //System.out.println(r.);
            //Renderer ren = new RDFRenderer();
            //System.out.println(r.printMetadata(ren));

            //r = new resolver("DEMOH", 1, "Sequencing");
            //System.out.println(r.getArk());
            //System.out.println(r.resolveARK().toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            r.close();
        }


        /* String result = null;
        try {
            result = URLDecoder.decode("ark%3A%2F87286%2FC2", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            r = new resolver(result);
            r.resolveARK();
            System.out.println(r.ark + " : " + r.datasets_id);
            //    EZIDService service = new EZIDService();
            //    System.out.println("  " + r.resolveAll(service));
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

    }

    /**
     * Return a graph in a particular format
     *
     * @param format
     *
     * @return
     */
    public URI resolveArkAs(String format) throws URISyntaxException {
        // Example
        //http://biscicol.org:8179/biocode-fims/rest/query/tab?graphs=urn:uuid:ec90c3b6-cc75-4090-b03d-cf3d76a27783&project_id=1

        String contentResolutionRoot = sm.retrieveValue("contentResolutionRoot");
        return new URI(contentResolutionRoot + format + "?graphs=" + graph + "&project_id=" + project);
    }
}
