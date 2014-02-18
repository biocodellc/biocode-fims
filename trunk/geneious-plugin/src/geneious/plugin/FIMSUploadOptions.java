package geneious.plugin;

import com.biomatters.geneious.publicapi.plugin.Options;

import java.util.ArrayList;
import java.util.List;


/**
 * Define the Dialog Box Options available from the Geneious Plugin Interface
 */
public class FIMSUploadOptions extends Options {

    //StringOption expeditionCodeOption;
    ComboBoxOption projectOption;
    StringOption expeditionCodeOption;

    FileSelectionOption sampleDataOption;
    BooleanOption uploadOption;
    PasswordOption passwordOption;
    StringOption usernameOption;
    LabelOption labelOption;

    public FIMSUploadOptions() {
        super(FIMSUploadOptions.class);

        // Create an option value list of Expeditions
        // TODO: Make the project list dynamic. For now, i'm just hard-coding the projects that i know about
        List projectValues = new ArrayList();
        OptionValue optionValue1 = new OptionValue("1", "IndoPacific Database");
        OptionValue optionValue2 = new OptionValue("2", "Smithsonian LAB");
        OptionValue optionValue3 = new OptionValue("3", "Hawaii Dimensions");
        OptionValue optionValue4 = new OptionValue("5", "Barcode of Wildlife Training");

        projectValues.add(optionValue1);
        projectValues.add(optionValue2);
        projectValues.add(optionValue3);
        projectValues.add(optionValue4);

        projectOption = addComboBoxOption("projectCode", "Project:", projectValues, optionValue1);

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

        sampleDataOption = addFileSelectionOption("sampleData", "Sample Data:", "");

        uploadOption = addBooleanOption("upload", "Upload", true);

        //labelOption = (LabelOption) addLabel("Username/password are necessary for verifying expedition codes, obtaining identifier keys, and uploading to the database");
        usernameOption = addStringOption("username", "Username:", "");
        passwordOption = addCustomOption(new PasswordOption("password", "Password:"));

        // Code for adding user/password as dependent on upload being checked
        //uploadOption.addDependent(usernameOption, true);
        //uploadOption.addDependent(passwordOption, true);
    }
}
