import settings.*;

/**
 * Code to create a project.
 * Currently this is meant to be run by an administrator in order to setup projects.  Ultimately, there will
 * be a web-interface for other administrators to create projects
 */
public class createProject {

    bcidConnector bcidConnector;
    String project_code;
    // References to the BCID group Elements (Resource Types)
    Integer[] groupElements = new Integer[]{34, 10, 24, 36, 37, 11, 15, 13, 2};


    /**
     * create authentication request object, which stores authentication credentials
     *
     * @param username
     * @param password
     * @throws Exception
     */
    public createProject(String project_code, String username, String password) throws Exception {
        this.project_code = project_code;

        // First, authenticate username/password here
        fimsPrinter.out.println("Authenticating ...");
        bcidConnector = new bcidConnector();
        boolean authenticationSuccess = bcidConnector.authenticate(username, password);
        if (!authenticationSuccess)
            throw new Exception("Unable to authenticate user " + username);

        fimsPrinter.out.println("\tUser " + username + " authenticated");

        // Now check that this project code is available
        String response = bcidConnector.createProject(project_code, project_code + " data group", null, "http://n2t.net/ark:/21547/Fm2");

        fimsPrinter.out.println("\t" + response);
        //throw new Exception("Project code " + project_code + " is unavailable");
    }


    /**
     * Command-line tool for creating projects.  Used by system administrator only
     *
     * @param args
     */
    public static void main(String[] args) {
        //TODO: Create command-line parser using the following arguments
        String username = "demo";
        String password = "demo";
        String project_code = "DEMOH";

        // Instantiate createProject object
        createProject p = null;
        try {
            p = new createProject(project_code, username, password);
        } catch (Exception e) {
            fimsPrinter.out.println("\tUnable to create project: " + e.getMessage());
            return;
        }

        fimsPrinter.out.println("Creating BCIDs:");
        for (Integer id : p.groupElements) {
            try {
                fimsPrinter.out.println("\t" + p.createBCIDAndAssociate(id));
            } catch (Exception e) {
                fimsPrinter.out.println("\tTrouble creating BCID: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return;

    }

    /**
     * Create an individual BCID and Associate
     *
     * @return
     * @throws Exception
     */
    private String createBCIDAndAssociate(Integer resourceType) throws Exception {
        String bcid = bcidConnector.createBCID("", project_code + " group element", resourceType);
        bcidConnector.associateBCID(project_code, bcid);
        return bcid;
    }

}
