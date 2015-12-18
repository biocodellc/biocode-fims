package bcid;

import bcid.Renderer.JSONRenderer;
import bcid.Renderer.Renderer;
import fimsExceptions.BadRequestException;
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
    String suffix = null;        // The local identifier
    BigInteger element_id = null;
    Integer bcidsId = null;
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
        // Now decipher the shoulder and suffix in the next bit
        setShoulderAndSuffix(bits[2]);
        // Call setBcid() to set bcidsId
        setBcid();
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
                    "b.prefix as prefix \n" +
                    "from \n" +
                    "bcids b, expeditionsBCIDs eb, expeditions p \n" +
                    "where \n" +
                    "b.bcids_id=eb.bcids_id&& \n" +
                    "eb.expedition_id=p.expedition_id && \n" +
                    "p.expedition_code= ? && \n" +
                    "p.project_id = ? && \n" +
                    "(b.resourceType=? || b.resourceType= ?)";
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
    public Integer getBcidId () {
        return bcidsId;
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
     * Set the shoulder and suffix variables for this ARK
     *
     * @param a
     */
    private void setShoulderAndSuffix (String a) {
        boolean reachedShoulder = false;
        StringBuilder sbShoulder = new StringBuilder();
        StringBuilder sbSuffix = new StringBuilder();

        for (int i = 0; i < a.length(); i++) {
            char c = a.charAt(i);
            if (!reachedShoulder)
                sbShoulder.append(c);
            else
                sbSuffix.append(c);
            if (Character.isDigit(c))
                reachedShoulder = true;
        }
        shoulder = sbShoulder.toString();
        suffix = sbSuffix.toString();

        // String the slash between the shoulder and the suffix
        if (!sm.retrieveValue("divider").equals("")) {
            if (suffix.startsWith(sm.retrieveValue("divider"))) {
                suffix = suffix.substring(1);
            }
        }
    }

    /**
     * Attempt to resolve a particular ARK.
     *
     * @return URI content location URL
     */
    public URI resolveARK() throws URISyntaxException {
        bcid bcid;
        URI resolution;

        // First  option is check if bcid, then look at other options after this is determined
        if (!isValidBCID()) {
            throw new BadRequestException("Invalid identifier.");
        }

        bcid = new bcid(suffix, bcidsId);

        // A resolution target is specified AND there is a suffix AND suffixPassThrough
        if (bcid.getWebAddress() != null && !bcid.getWebAddress().equals("") &&
            suffix != null && !suffix.trim().equals("") && bcid.getBcidsSuffixPassthrough()) {
            forwardingResolution = true;

            // Immediately return resolution result
            resolution = new URI(bcid.getWebAddress() + suffix);
        } else {
            resolution = bcid.getMetadataTarget();

            this.project = getProjectID(bcidsId);
        }

        return resolution;
    }

    /**
     * Print Metadata for a particular ARK
     *
     * @return JSON String with content for the interface
     */
    public String printMetadata(Renderer renderer) {
        GenericIdentifier bcid = null;

        // First  option is check if bcid, then look at other options after this is determined
        if (setBcid()) {

            bcid = new bcid(bcidsId);

            // Has a registered, resolvable suffix
            //if (isResolvableSuffix(bcidsId)) {
            //    bcid = new bcid(element_id, ark);
            //}
            // Has a suffix, but not resolvable
            //else {
//            graph =
            try {
                if (suffix != null && bcid.getWebAddress() != null) {
                    bcid = new bcid(suffix, bcid.getWebAddress(), bcidsId);
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

    private boolean isValidBCID() {
        if (bcidsId != null)
            return true;
        else
            return false;
    }

    /**
     * Check if this is a bcid and set the bcidsId
     *
     * @return
     */
    private boolean setBcid() {
        // Test bcid is #1
        if (shoulder.equals("fk4") && naan.equals("99999")) {
            bcidsId = 1;
            return true;
        }

        // Decode a typical bcid
        bcidsId = new bcidEncoder().decode(shoulder).intValue();

        if (bcidsId == null) {
            return false;
        } else {
            // Now we need to figure out if this bcids_id exists or not in the database
            String select = "SELECT count(*) as count FROM bcids where bcids_id = ?";
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                stmt = conn.prepareStatement(select);
                stmt.setInt(1, bcidsId);
                rs = stmt.executeQuery();
                rs.next();
                int count = rs.getInt("count");
                if (count < 1) {
                    bcidsId = null;
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
     * Get the projectId given a bcidsId
     *
     * @param bcidsId
     */
    public String getProjectID(Integer bcidsId) {
        String project_id = "";
        String sql = "select p.project_id from projects p, expeditionsBCIDs eb, expeditions e, " +
                "bcids b where b.bcids_id = eb.bcids_id and e.expedition_id=eb.`expedition_id` " +
                "and e.`project_id`=p.`project_id` and b.bcids_id = ?";

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bcidsId);
            rs = stmt.executeQuery();
            rs.next();
            project_id = rs.getString("project_id");

        } catch (SQLException e) {
            // catch the exception and log it
            logger.warn("Exception retrieving projectCode for bcid: " + bcidsId, e);
        } finally {
            close(stmt, rs);
        }
        return project_id;
    }

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
            // suffixpassthrough = 1; no webAddress specified; has a Suffix
            /*r = new resolver("ark:/87286/U264c82d19-6562-4174-a5ea-e342eae353e8");
            expected = "http://biscicol.org/id/metadata/ark:/21547/U264c82d19-6562-4174-a5ea-e342eae353e8";
            System.out.println(r.resolveARK());
                  */
            // suffixPassthrough = 1; webaddress specified; has a Suffix
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
            // suffixPassthrough = 1; webaddress specified; no Suffix
            r = new resolver("ark:/21547/R2");
            expected = "http://biscicol.org/id/metadata/ark:/21547/R2";
            System.out.println(r.resolveARK());

            // suffixPassthrough = 0; no webaddress specified; no Suffix
            r = new resolver("ark:/21547/W2");
            expected = "http://biscicol.org/id/metadata/ark:/21547/W2";
            System.out.println(r.resolveARK());

            // suffixPassthrough = 0; webaddress specified; no Suffix
            r = new resolver("ark:/21547/Gk2");
            expected =  "http://biscicol.org:3030/ds?graph=urn:uuid:77806834-a34f-499a-a29f-aaac51e6c9f8";
            System.out.println(r.resolveARK());

               // suffixPassthrough = 0; webaddress specified;  suffix specified (still pass it through
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

    public String getExpeditionCode() {
        String expedition_code = "";

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "select e.expedition_code from expeditionsBCIDs eb, expeditions e, bcids b " +
                    "where b.bcids_id = eb.bcids_id and e.expedition_id=eb.`expedition_id` and b.bcids_id = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bcidsId);
            rs = stmt.executeQuery();
            rs.next();
            expedition_code = rs.getString("expedition_code");

        } catch (SQLException e) {
            // catch the exception and log it
            logger.warn("Exception retrieving expedition_code for bcid: " + bcidsId, e);
        } finally {
            close(stmt, rs);
        }
        return expedition_code;
    }

    public Integer getExpeditionId() {
        Integer expeditionId = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "select eb.expedition_id from expeditionsBCIDs eb, bcids b " +
                    "where b.bcids_id = eb.bcids_id and b.bcids_id = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bcidsId);
            rs = stmt.executeQuery();
            rs.next();
            expeditionId = rs.getInt("eb.expedition_id");

        } catch (SQLException e) {
            // catch the exception and log it
            logger.warn("Exception retrieving expedition_id for bcid: " + bcidsId, e);
        } finally {
            close(stmt, rs);
        }
        return expeditionId;
    }
}
