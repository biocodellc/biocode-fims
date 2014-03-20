package geneious.plugin;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.plugin.Options;
import run.process;
import settings.FIMSException;
import settings.availableProject;
import settings.availableProjectsFetcher;
import settings.bcidConnector;

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
    StringOption expeditionCodeOption;

    FileSelectionOption sampleDataOption;
    BooleanOption uploadOption;
    PasswordOption passwordOption;
    StringOption usernameOption;
    LabelOption labelOption;
    bcidConnector connector = null;

    @Override
    public String verifyOptionsAreValid() {
        return super.verifyOptionsAreValid();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public FIMSUploadOptions() {
        super(FIMSUploadOptions.class);

          // Username/password
        usernameOption = addStringOption("username", "Username:", "");
        passwordOption = addCustomOption(new PasswordOption("password", "Password:"));

        // Create an option value list of Proects
        availableProjectsFetcher fetcher = new availableProjectsFetcher();
        Iterator projectsIt = fetcher.getAvailableProjects().iterator();

        // Populate the OptionBoxes from the list of available Projects
        List projectValues = new ArrayList();
        chooseProject = new OptionValue("Choose Project", "Choose Project");
        projectValues.add(chooseProject);

        while (projectsIt.hasNext()) {
            availableProject p = (availableProject) projectsIt.next();
            OptionValue v = new OptionValue(p.getProject_id(), p.getProject_title());
            projectValues.add(v);
        }
        projectOption = addComboBoxOption("projectCode", "Project:", projectValues, chooseProject);
        projectOption.setVisible(false);

        /*
        I'm not sure how to code this part.  Basically what i want is to

        FIRST have the user select a particular project from the list in the projectOption above.
        Based on the results of that operation, we will...

        SECOND, call the following service:  http://biscicol.org/id/projectService/graphs/1
        Where "1" equals the value from the project box above.  This returns JSON that when parsed for expedition_codes populates
        a String[] containing {"DEMOH","DEMOI","DEMOX"}

        THIRD: populate the expeditionValues box, like....

        String[] expeditionValues = {"DEMOX", "DEMOJ", "DEMOH"};
        expeditionCodeOption = addEditableComboBoxOption("expeditionCode", "Expedition Code:", "", expeditionValues);

        I can basically take care of the FIRST and SECOND above but need help with the THIRD... that is, don't let the user progress
        until they select an appropriate project and showing them an editableComboBox with expedition codes they already
        have (or enabling adding a new one)

        for now, i have the expedition coded as an input string
         */

        expeditionCodeOption = addStringOption("expeditionCode", "Expedition Code:", "DEMOH");
         expeditionCodeOption.setVisible(false);

        sampleDataOption = addFileSelectionOption("sampleData", "Sample Data:", "");
        sampleDataOption.setVisible(false);

        uploadOption = addBooleanOption("upload", "Upload", true);
        uploadOption.setVisible(false);

        //labelOption = (LabelOption) addLabel("Username/password are necessary for verifying expedition codes, obtaining identifier keys, and uploading to the database");


        // Code for adding user/password as dependent on upload being checked
        //uploadOption.addDependent(usernameOption, true);
        //uploadOption.addDependent(passwordOption, true);
    }

    @Override
    public boolean areValuesGoodEnoughToContinue() {
       // Make sure the user has chosen a project
        if (projectOption.getValue() == chooseProject) {  // You'll need to make your "Choose Project" accessible.
            Dialogs.showMessageDialog("You need to choose a project");
            return false;
        }

        // TEST for user authentication before proceeding too far
        if (connector == null) {
            try {
                connector = process.createConnection(usernameOption.getValue(), passwordOption.getValue());
            } catch (FIMSException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                Dialogs.showMessageDialog("Authentication system not currently working, or no internet connection");
                return false;
            }

            // User authenticated... turn on the other options
            if (connector != null) {
                userAuthenticated();
            } else {
                Dialogs.showMessageDialog("Unable to authenticate " + usernameOption.getValue());
            }
            return false;
        } else {
            return true;
        }
    }

    public void userAuthenticated() {
        projectOption.setVisible(true);
        expeditionCodeOption.setVisible(true);
        sampleDataOption.setVisible(true);
        uploadOption.setVisible(true);
        usernameOption.setVisible(false);
        passwordOption.setVisible(false);
    }
}
