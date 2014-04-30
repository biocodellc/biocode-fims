package geneious.plugin;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.components.ProgressFrame;
import com.biomatters.geneious.publicapi.plugin.Options;
import com.biomatters.geneious.publicapi.utilities.ThreadUtilities;
import run.process;
import settings.FIMSException;
import settings.availableProject;
import settings.bcidConnector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Define the Dialog Box Options available from the Geneious Plugin Interface
 */
public class FIMSUploadOptions extends Options {

    //StringOption expeditionCodeOption;
    OptionValue chooseProject = new OptionValue("Choose Project", "Choose Project");
    ComboBoxOption projectOption;
    List projectValues = new ArrayList();
    StringOption expeditionCodeOption;

    FileSelectionOption sampleDataOption;
    BooleanOption uploadOption;
    //PasswordOption passwordOption;
    //StringOption usernameOption;
    LabelOption labelInitialHeaderOption;
    LabelOption labelInformationMessage;

    //LabelOption labelForgotPasswordOption;
    //LabelOption labelForgotUsernameOption;
    ButtonOption loginButtonOption;
    StringOption usernameOption;
    // PasswordOption passwordOption;

    LabelOption labelProjectHeaderOption;
    String username, password;

    bcidConnector connector = null;

    @Override
    public String verifyOptionsAreValid() {
        return super.verifyOptionsAreValid();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public FIMSUploadOptions() {
        super(FIMSUploadOptions.class);

        // Username/password Options

        //final StringOption usernameOption = addStringOption("username", "Username:", "");
        final StringOption usernameOption = addStringOption("username", "Username/Password: ", "");
        final PasswordOption passwordOption = new PasswordOption("password", "Password:");

        JPasswordField jPasswordField = passwordOption.createComponent();
        //addLabel("Password:");
        addCustomComponent(jPasswordField);


        //final StringOption passwordOption = addStringOption("password", "Password:", "");
        //passwordOption.setShouldSaveValue(false);
        //passwordOption.setRestorePreferenceApplies(false);
        addButtonOption("authenticate", "", "Authenticate").addActionListener(new ActionListener() {
            //@Override
            public void actionPerformed(ActionEvent e) {
                final ProgressFrame progressFrame = new ProgressFrame("Authenticating...", "", Dialogs.getCurrentModalDialog());
                progressFrame.setCancelable(false);
                progressFrame.setIndeterminateProgress();


                final Runnable afterAuthTry = new Runnable() {
                    public void run() {
                        ThreadUtilities.sleep(2000);
                        try {
                            // User authenticated... turn on the other options
                            if (connector != null) {
                                // run the userAuthenticated method sets up the next stage of operations
                                userAuthenticated();
                            } else {
                                Dialogs.showMessageDialog("Unable to authenticate " + username);
                            }
                        } catch (Exception e1) {
                            displayExceptionDialog("Authentication Error", e1.getMessage(), e1, null);
                            //Dialogs.showMessageDialog("Authentication Exception: " + e1.getMessage());
                        }
                    }
                };

                Thread connectingThread = new Thread() {
                    public void run() {
                        try {
                            username = usernameOption.getValue();
                            password = passwordOption.getValue();
                            connector = process.createConnection(username, password);
                        } catch (FIMSException e) {
                            displayExceptionDialog(e);
                        }
                        //frame.setComplete();
                        ThreadUtilities.invokeNowOrLater(afterAuthTry);
                        progressFrame.setComplete();
                    }
                };
                connectingThread.start();
            }
        });
        addDivider(" ");

        // Project-related Options
        //labelProjectHeaderOption = (LabelOption) addLabel("Data validation and loading options for " + this.usernameOption.getValue().toString());
        labelProjectHeaderOption = (LabelOption) addLabel("Data validation and loading options");

        projectValues = new ArrayList();
        projectValues.add(chooseProject);
        projectOption = addComboBoxOption("projectCode", "Project:", projectValues, chooseProject);

        // select Expedition
        expeditionCodeOption = addStringOption("expeditionCode", "Dataset Code:", "DEMOH");
        // pointer to File to load
        sampleDataOption = addFileSelectionOption("sampleData", "Sample Data:", "");
        // upload checkbox
        uploadOption = addBooleanOption("upload", "Upload", true);

        hideElements();

        // Code for adding user/password as dependent on upload being checked
        //uploadOption.addDependent(usernameOption, true);
        //uploadOption.addDependent(passwordOption, true);
    }


    /**
     * Make it convenient to turn off elements commonly that we don't want to show
     */
    private void hideElements() {
        // set them all to invisible for now
        labelProjectHeaderOption.setVisible(false);
        projectOption.setVisible(false);
        expeditionCodeOption.setVisible(false);
        sampleDataOption.setVisible(false);
        uploadOption.setVisible(false);
    }

    /**
     * Make it convenient to turn on elements commonly that we don't want to show
     */
    private void showElements() {
        labelProjectHeaderOption.setVisible(true);
        projectOption.setVisible(true);
        expeditionCodeOption.setVisible(true);
        sampleDataOption.setVisible(true);
        uploadOption.setVisible(true);
    }

    @Override
    public boolean areValuesGoodEnoughToContinue() {

        // TEST for user authentication before proceeding
        if (connector == null) {
            return false;
        } else {
            // Make sure the user has chosen a project
            if (projectOption.getValue() == chooseProject) {
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
    public boolean userAuthenticated() {

        // If there is a project then continue!
        if (hasProject()) {

            labelProjectHeaderOption.setValueFromString("Data validation / loading options for '" + username + "'");

            showElements();
            // Turn off visibility on Username/password options
            //labelInitialHeaderOption.setVisible(false);
            // labelForgotPasswordOption.setVisible(false);
            /// labelForgotUsernameOption.setVisible(false);
            //loginButtonOption.setVisible(false);
            // Evidently, the setHidden option cannot be called here
            //loginButtonOption.setHidden();
            return true;
        } else {
            Dialogs.showMessageDialog(username + " is not associated with any projects, talk " +
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
    private boolean hasProject() {
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
        // Re-initialize list
        projectValues = new ArrayList();
        projectValues.add(chooseProject);
        while (projectsIt.hasNext()) {
            availableProject p = (availableProject) projectsIt.next();
            OptionValue v = new OptionValue(p.getProject_id(), p.getProject_title());
            //Dialogs.showMessageDialog(p.getProject_code());
            //projectOption.addPossibleValue(v);
            projectValues.add(v);
            hasProject = true;
        }
        projectOption.setPossibleValues(projectValues);

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
