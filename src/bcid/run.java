package bcid;

import ezid.EZIDException;
import ezid.EZIDService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.SettingsManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;


/**
 * This is a class used for running/testing Identifier creation methods.
 */
public class run {
    // a testData file to use for various tests in this class
    ArrayList testDatafile;

    private static Logger logger = LoggerFactory.getLogger(run.class);

    public run() {

    }

    public run(String path, bcidMinter bcidMinter) {
        // Set this to the TEST bcid

        // Create test data
        System.out.println("\nReading input file = " + path + " ...");
        /*String sampleInputStringFromTextBox = "" +
                "MBIO056\thttp://biocode.berkeley.edu/specimens/MBIO56\n" +
                "56\n" +
                "urn:uuid:1234-abcd-5678-efgh-9012-ijkl\n" +
                "38543e40-665f-11e2-89f3-001f29e2923c";
          */

        try {
            testDatafile = new inputFileParser(readFile(path), bcidMinter).elementArrayList;
        } catch (IOException e) {
            //TODO should we silence this exception?
            logger.warn("IOException thrown", e);
        } catch (URISyntaxException e) {
            //TODO should we silence this exception?
            logger.warn("URISyntaxException thrown", e);
        }
        System.out.println("  Successfully created test bcid");
    }

    private static void resolverResults(EZIDService ezidService, String identifier) {
        resolver r = new resolver(identifier);
        System.out.println("Attempting to resolve " + identifier);
        System.out.println(r.resolveAllAsJSON(ezidService));
        r.close();
    }

    /**
     * Test the Resolution Services
     *
     * @param ezidService
     */
    public static void resolver(EZIDService ezidService) {
        resolverResults(ezidService, "ark:/87286/C2/AOkI");
        resolverResults(ezidService, "ark:/87286/C2/64c82d19-6562-4174-a5ea-e342eae353e8");
        resolverResults(ezidService, "ark:/87286/C2/Foo");
        resolverResults(ezidService, "ark:/87286/Cddfdf2");
    }

    /**
     * Test creating a bunch of BCIDs as if it were being called from a REST service
     */
    private void runBCIDCreatorService(){

        // Initialize variables
        bcidMinter bcidMinter;
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();

        EZIDService ezidAccount = new EZIDService();
        Integer userId = 1;
        Integer ResourceType = ResourceTypes.PRESERVEDSPECIMEN;
        String doi = null;
        String webAddress = null;

        // Create a new bcid
        System.out.println("\nCreating a new bcid");
        bcidMinter = new bcidMinter(true, true);
        bcidMinter.createEntityBcid(userId, new ResourceTypes().get(ResourceType).uri, webAddress, null, doi, false);
        bcidMinter.close();

        /*
         // Create test data by using input file
        String path = Thread.currentThread().getContextClassLoader().getResource("bigfile.txt").getFile();
        System.out.println("\nReading input file = " + path + " ...");
        try {
            testDatafile = new inputFileParser(readFile(path), dataset).elementArrayList;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // Mint a list of identifiers
        System.out.println("\nPreparing to mint " + testDatafile.size()+ " identifiers");
        String datasetUUID = minter.mintList(testDatafile);

        // Return the list of identifiers that were made here
        System.out.println(JSONArray.fromObject(minter.getIdentifiers(datasetUUID)).toString());
        */
    }

    private static String readFile(String path) throws IOException {
        FileInputStream stream = new FileInputStream(new File(path));
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            /* Instead of using default, pass in a decoder. */
            return Charset.defaultCharset().decode(bb).toString();
        } finally {
            stream.close();
        }
    }

    public static void main(String[] args) {

        EZIDService ezidAccount = new EZIDService();

        // Initialize settings manager
        SettingsManager sm = SettingsManager.getInstance();
        try {
            sm.loadProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize ezid account
        try {
            // Setup EZID account/login information
            ezidAccount.login(sm.retrieveValue("eziduser"), sm.retrieveValue("ezidpass"));

        } catch (EZIDException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            // Create EZID metadata for all Outstanding Identifiers.
            manageEZID creator = new manageEZID();
            creator.createBcidsEZIDs(ezidAccount);
            creator.close();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        /*
        bcidMinter d = null;
        try {
            d = new bcidMinter();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        System.out.println( d.datasetTable("biocode"));
        */
    }
}
