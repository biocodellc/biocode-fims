package utils;

import auth.Authenticator;
import run.Process;
import run.ProcessController;
import settings.FimsPrinter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Special purpose class for loading DIPNet data initially
 * Created by jdeck on 7/10/15.
 */
public class DipnetLoad {
    // output directory for processing and temp files
    static String outputDirectory = "/Users/jdeck/IdeaProjects/biocode-fims/tripleOutput";
    // Project_id
    static Integer projectId = 25;
    // Input directory storing all the loaded files
    static String inputDirectory = "/Users/jdeck/Google Drive/!DIPnet_DB/Repository/1-cleaned_QC2_mdfasta_files";

    static String password;

    static boolean triplify = false;
    static boolean upload = true;
    static boolean expeditionCheck = true;
    static boolean forceAll = true;  // force loading, no questions
    private static String username = "dipnetCurator";

    public static void main(String[] args) throws FileNotFoundException {

        // Redirect output to file
        PrintStream out = new PrintStream(new FileOutputStream(outputDirectory + File.separator + "dipnetloading_output.txt"));
        System.setOut(out);

        // only argument is password
        password = args[0];

        // Call the connection with password as single argument
        Authenticator authenticator = new Authenticator();
        FimsPrinter.out.println("Authenticating ...");

        if (!authenticator.login(username, password)) {
            FimsPrinter.out.println("Unable to authenticate " + username +
                    " using the supplied credentials!");
            return;
        }

        // ONE-OFF Run the dataset Loader
        loadDataset("C2_acapla_CO1_all", "/Users/jdeck/Google Drive/!DIPnet_DB/Repository/1-cleaned_QC2_mdfasta_files/mdfastaQC2_acapla_CO1_all.txt");
        loadDataset("QC2_Eucmet_C01_HL_all","/Users/jdeck/Google Drive/!DIPnet_DB/Repository/1-cleaned_QC2_mdfasta_files/mdfastaQC2_Eucmet_CO1_HL.txt");
         /*
        // Loop all the files in the input directory
        Iterator it = FileUtils.iterateFiles(new File(inputDirectory), null, false);
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
    public static void loadDataset(String expeditionCode, String inputFile) {
        boolean readyToLoad = false;
        // Create the process controller object
        ProcessController pc = new ProcessController(projectId, expeditionCode);
        pc.setUserId(username);

        System.out.println("Initializing ...");

        // Build the process object
        Process p = new Process(
                inputFile,
                outputDirectory,
                pc
        );

        System.out.println("\tinputFilename = " + inputFile);

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
            FimsPrinter.out.println("NOT CHECKING FOR WARNINGS");
            readyToLoad = true;
            // Check for warnings if not forceload
        } else if (!pc.isValidated() && pc.getHasWarnings()) {
            System.out.println("WARNINGS PRESENT, NOT LOADING");
            System.out.println(pc.getCommandLineSB().toString());
        }

        // Run the loader
        if (readyToLoad) {
            p.runAllLocally(triplify, upload, expeditionCheck, forceAll);
        }
    }

}
