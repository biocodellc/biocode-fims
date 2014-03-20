package geneious.plugin;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.components.ProgressFrame;
import com.biomatters.geneious.publicapi.plugin.Options;
import com.biomatters.geneious.publicapi.utilities.GuiUtilities;
import jebl.util.CompositeProgressListener;
import jebl.util.ProgressListener;
import run.process;
import settings.FIMSException;
import settings.availableProject;
import settings.bcidConnector;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Define the Dialog Box Options available from the Geneious Plugin Interface
 */
public class FIMSUploadOptions extends Options {

    //StringOption expeditionCodeOption;
    OptionValue chooseProject;
    ComboBoxOption projectOption;
    List projectValues = new ArrayList();
    StringOption expeditionCodeOption;

    FileSelectionOption sampleDataOption;
    BooleanOption uploadOption;
    PasswordOption passwordOption;
    StringOption usernameOption;
    LabelOption labelInitialHeaderOption;
    LabelOption labelForgotPasswordOption;
    LabelOption labelForgotUsernameOption;

    LabelOption labelProjectHeaderOption;


    bcidConnector connector = null;

    @Override
    public String verifyOptionsAreValid() {
        return super.verifyOptionsAreValid();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public FIMSUploadOptions() {
        super(FIMSUploadOptions.class);

        // Username/password Options
        labelInitialHeaderOption = (LabelOption) addLabel("Login using your Biocode-FIMS username/password");

        usernameOption = addStringOption("username", "Username:", "");
        passwordOption = addCustomOption(new PasswordOption("password", "Password:"));

        labelForgotPasswordOption = (LabelOption) addLabel("Forgot Password?");
        labelForgotUsernameOption = (LabelOption) addLabel("Forgot Username?");

        // Project-related Options
        labelProjectHeaderOption = (LabelOption) addLabel("Data validation and loading options for user " + this.usernameOption.getValue().toString());

        chooseProject = new OptionValue("Choose Project", "Choose Project");
        projectValues.add(chooseProject);
        projectOption = addComboBoxOption("projectCode", "Project:", projectValues, chooseProject);

        // select Expedition
        expeditionCodeOption = addStringOption("expeditionCode", "Expedition Code:", "DEMOH");
        // pointer to File to load
        sampleDataOption = addFileSelectionOption("sampleData", "Sample Data:", "");
        // upload checkbox
        uploadOption = addBooleanOption("upload", "Upload", true);

        // set them all to invisible for now
        labelProjectHeaderOption.setVisible(false);
        projectOption.setVisible(false);
        expeditionCodeOption.setVisible(false);
        sampleDataOption.setVisible(false);
        uploadOption.setVisible(false);

        // Code for adding user/password as dependent on upload being checked
        //uploadOption.addDependent(usernameOption, true);
        //uploadOption.addDependent(passwordOption, true);
    }

    @Override
    public boolean areValuesGoodEnoughToContinue() {

        // TEST for user authentication before proceeding
        if (connector == null) {
            /*ProgressListener progress = ProgressListener.EMPTY;
            CompositeProgressListener compositeProgress=new CompositeProgressListener(progress);
            progress.setIndeterminateProgress();
            progress.setMessage("Authenticating user");
            */
            final ProgressFrame frame = new ProgressFrame("Authenticating user", "Checking with server...", GuiUtilities.getMainFrame());
            frame.setIndeterminateProgress();

            Thread connectingThread = new Thread() {
                public void run() {
                    try {
                        connector = process.createConnection(usernameOption.getValue(), passwordOption.getValue());
                    } catch (FIMSException e) {
                        frame.setComplete();
                        displayExceptionDialog(e);
                    }
                    frame.setComplete();
                }
            };
            connectingThread.start();

            try {
                connectingThread.join();
            } catch (InterruptedException e) {
                Dialogs.showMessageDialog("Interrupted connection process!");
            }

            // User authenticated... turn on the other options
            if (connector != null) {
                // run the userAuthenticated method sets up the next stage of operations
                userAuthenticated();
            } else {
                Dialogs.showMessageDialog("Unable to authenticate " + usernameOption.getValue());
            }
            // We're still not ready to continue... need user to select a project, etc...
            return false;
        }
        // Make sure the user has chosen a project
        else

        {

            if (projectOption.getValue() == chooseProject) {  // You'll need to make your "Choose Project" accessible.
                Dialogs.showMessageDialog("You need to choose a project");
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Set of processes to execute after a user has been authenticated
     *
     * @throws Exception
     */

    public boolean userAuthenticated
    () {

        // If there is a project then continue!
        if (hasProject()) {

            labelProjectHeaderOption.setValueFromString("Data validation and loading options for user " + this.usernameOption.getValue().toString());
            labelProjectHeaderOption.setVisible(true);
            projectOption.setVisible(true);
            expeditionCodeOption.setVisible(true);
            sampleDataOption.setVisible(true);
            uploadOption.setVisible(true);

            // Turn off visibility on Username/password options
            usernameOption.setVisible(false);
            passwordOption.setVisible(false);
            labelInitialHeaderOption.setVisible(false);
            labelForgotPasswordOption.setVisible(false);
            labelForgotUsernameOption.setVisible(false);
            return true;
        } else {
            Dialogs.showMessageDialog(usernameOption.getValue() + " is not associated with any projects, talk " +
                    "to a project administrator to get setup.");
            return false;
        }
    }

    /**
     * Convenience method for both populating the projects Combo Box and checking to see if there
     * are any projects associated with this user
     *
     * @return
     */
    private boolean hasProject
    () {
        // populate the user's projects
        //http://biscicol.org/id/projectService/listUserProjects
        // Create an option value list of Proects
        //availableProjectsFetcher fetcher = new availableProjectsFetcher();
        // Populate the OptionBoxes from the list of available Projects


        boolean hasProject = false;

        ArrayList<availableProject> availableProjects = null;
        try {
            availableProjects = connector.listAvailableProjects();
        } catch (Exception e) {
            //e.printStackTrace();
            // StringWriter errors = new StringWriter();
            // e.printStackTrace(new PrintWriter(errors));
            displayExceptionDialog(e);
            //Dialogs.showMessageDialog("Exception occurred while fetching user projects \n");
            return false;
        }
        Iterator projectsIt = availableProjects.iterator();

        while (projectsIt.hasNext()) {
            availableProject p = (availableProject) projectsIt.next();
            OptionValue v = new OptionValue(p.getProject_id(), p.getProject_title());
            //Dialogs.showMessageDialog(p.getProject_code());
            projectOption.addPossibleValue(v);
            projectValues.add(v);
            hasProject = true;
        }

        return hasProject;
    }

    public static void displayExceptionDialog
            (Exception
                     exception) {
        displayExceptionDialog("Error", exception.getMessage(), exception, null);
    }


    public static void displayExceptionDialog
            (String
                     title, String
                    message, Exception
                    exception, Component
                    owner) {
        StringWriter stacktrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stacktrace));
        Dialogs.DialogOptions dialogOptions = new Dialogs.DialogOptions(Dialogs.OK_ONLY, title, owner, Dialogs.DialogIcon.WARNING);
        dialogOptions.setMoreOptionsButtonText("Show details...", "Hide details...");
        Dialogs.showMoreOptionsDialog(dialogOptions, message, stacktrace.toString());
    }
}
