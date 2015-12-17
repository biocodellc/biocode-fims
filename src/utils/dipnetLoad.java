package utils;

import auth.authenticator;
import run.process;
import run.processController;
import settings.fimsPrinter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Special purpose class for loading DIPNet data initially
 * Created by jdeck on 7/10/15.
 */
public class dipnetLoad {
    // output directory for processing and temp files
    static String output_directory = "/Users/jdeck/IdeaProjects/biocode-fims/tripleOutput";
    // Project_id
    static Integer project_id = 25;
    // Input directory storing all the loaded files
    static String input_directory = "/Users/jdeck/Google Drive/!DIPnet_DB/Repository/1-cleaned_QC2_mdfasta_files";

    static String password;

    static boolean triplify = false;
    static boolean upload = true;
    static boolean expeditioncheck = true;
    static boolean forceAll = true;  // force loading, no questions
    private static String username = "dipnetCurator";

    public static void main(String[] args) throws FileNotFoundException {

        // Redirect output to file
        PrintStream out = new PrintStream(new FileOutputStream(output_directory + File.separator + "dipnetloading_output.txt"));
        System.setOut(out);

        // only argument is password
        password = args[0];

        // Call the connection with password as single argument
        authenticator authenticator = new auth.authenticator();
        fimsPrinter.out.println("Authenticating ...");

        if (!authenticator.login(username, password)) {
            fimsPrinter.out.println("Unable to authenticate " + username +
                    " using the supplied credentials!");
            return;
        }

        // ONE-OFF Run the dataset Loader
        loadDataset("C2_acapla_CO1_all", "/Users/jdeck/Google Drive/!DIPnet_DB/Repository/1-cleaned_QC2_mdfasta_files/mdfastaQC2_acapla_CO1_all.txt");
        loadDataset("QC2_Eucmet_C01_HL_all","/Users/jdeck/Google Drive/!DIPnet_DB/Repository/1-cleaned_QC2_mdfasta_files/mdfastaQC2_Eucmet_CO1_HL.txt");
         /*
        // Loop all the files in the input directory
        Iterator it = FileUtils.iterateFiles(new File(input_directory), null, false);
        while (it.hasNext()) {
            File file = (File) it.next();
            String fileAbsolutePath = file.getAbsolutePath();
            String fileName = file.getName();
            String datasetCode;
            //Don't attempt to load conflicted files
            if (!fileName.contains("Conflict")) {
                if (fileName.contains(".txt")) {
                    datasetCode = fileName.replaceAll(".txt", "").replaceAll("mdfastaQC2_", "");

                    // Run the dataset Loader
                    loadDataset(datasetCode, fileAbsolutePath);
                }
            }
        }  */

    }

    /**
     * Load dataset
     */
    public static void loadDataset(String expedition_code, String input_file) {
        boolean readyToLoad = false;
        // Create the process controller object
        processController pc = new processController(project_id, expedition_code);
        pc.setUser_id(username);

        System.out.println("Initializing ...");

        // Build the process object
        process p = new process(
                input_file,
                output_directory,
                pc
        );

        System.out.println("\tinputFilename = " + input_file);

        // Run our validator
        p.runValidation();

        // If there is errors, tell the user and stop the operation
        if (pc.getHasErrors()) {
            System.out.println(pc.getCommandLineSB().toString());
            return;
        }

        // Check for warnings
        // first part is see if want to just force it, bypassing any warning messages
        if (forceAll) {
            fimsPrinter.out.println("NOT CHECKING FOR WARNINGS");
            readyToLoad = true;
            // Check for warnings if not forceload
        } else if (!pc.isValidated() && pc.getHasWarnings()) {
            System.out.println("WARNINGS PRESENT, NOT LOADING");
            System.out.println(pc.getCommandLineSB().toString());
        }

        // Run the loader
        if (readyToLoad) {
            p.runAllLocally(triplify, upload, expeditioncheck, forceAll);
        }
    }

}
