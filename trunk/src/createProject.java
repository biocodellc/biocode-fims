import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.api.representation.Form;

  import settings.*;
import javax.ws.rs.core.Cookie;
import java.util.ArrayList;

/**
 * Code to create a project.
 * Currently this is meant to be run by an administrator in order to setup projects.  Ultimately, there will
 * be a web-interface for other administrators to create projects
 */
public class createProject {

    bcidConnector bcidConnector;


    /**
     * create authentication request object, which stores authentication credentials
     *
     * @param username
     * @param password
     * @throws Exception
     */
    public createProject(String username, String password) throws Exception {
        // First, authenticate username/password here
        fimsPrinter.out.println("Authenticating ...");
        bcidConnector = new bcidConnector();
        boolean authenticationSuccess = bcidConnector.authenticate(username, password);
        if (!authenticationSuccess)
            throw new Exception("Unable to authenticate");
    }

    /**
     *
     *   public static int DATASET = 1;
    public static int EVENT = 2;
    public static int IMAGE = 3;
    public static int MOVINGIMAGE = 4;
    public static int PHYSICALOBJECT = 5;
    public static int SERVICE = 6;
    public static int SOUND = 7;
    public static int TEXT = 8;
    public static int LOCATION = 10;
    public static int AGENT = 11;
    public static int SPACER2 = 12;
    public static int INFORMATIONCONTENTENTITY = 13;
    public static int MATERIALSAMPLE = 15;
    public static int PRESERVEDSPECIMEN = 17;
    public static int FOSSILSPECIMEN = 18;
    public static int LIVINGSPECIMEN = 19;
    public static int HUMANOBSERVATION = 20;
    public static int MACHINEOBSERVATION = 21;
    public static int OCCURRENCE = 23;
    public static int IDENTIFICATION = 24;
    public static int TAXON = 25;
    public static int RESOURCERELATIONSHIP = 26;
    public static int MEASUREMENTORFACT = 27;
    public static int GEOLOGICALCONTEXT = 28;
    public static int BIOME = 30;
    public static int FEATURE = 31;
    public static int MATERIAL = 32;
    public static int RESOURCE = 34;
     * @param args
     */
    public static void main(String[] args) {
        String username = "demo";
        String password = "demo";

        // Instantiate createProject object
        createProject createProject = null;
        try {
            createProject = new createProject(username, password);
        } catch (Exception e) {
            fimsPrinter.out.println("\tUnable to authenticate user = " + username);
            return;
        }
        fimsPrinter.out.println("\tUser " + username + " authenticated");

        fimsPrinter.out.println("Creating BCIDs:");
        try {
            fimsPrinter.out.println("\t" + createProject.bcidConnector.createBCID("", "project title", 1));
        } catch (Exception e) {
            fimsPrinter.out.println("\tTrouble creating BCID: " + e.getMessage());
            e.printStackTrace();
        }
        return;
    }

}
