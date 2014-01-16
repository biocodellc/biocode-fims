package geneious.plugin;

import com.biomatters.geneious.publicapi.plugin.Options;

import java.util.ArrayList;
import java.util.List;


/**
 * Define the Dialog Box Options available from the Geneious Plugin Interface
 */
public class FIMSUploadOptions extends Options {

    //StringOption projectCodeOption;
    ComboBoxOption expeditionOption;
    StringOption projectCodeOption;

    FileSelectionOption sampleDataOption;
    BooleanOption uploadOption;
    PasswordOption passwordOption;
    StringOption usernameOption;
    LabelOption labelOption;

    public FIMSUploadOptions() {
        super(FIMSUploadOptions.class);

        // Create an option value list of Projects
        // TODO: Make the expedition list dynamic. For now, i'm just hard-coding the expeditions that i know about
        List expeditionValues = new ArrayList();
        OptionValue optionValue1 = new OptionValue("1", "IndoPacific Database");
        OptionValue optionValue2 = new OptionValue("2", "Smithsonian LAB");
        OptionValue optionValue3 = new OptionValue("3", "Hawaii Dimensions");
        expeditionValues.add(optionValue1);
        expeditionValues.add(optionValue2);
        expeditionValues.add(optionValue3);
        expeditionOption = addComboBoxOption("expeditionCode", "Expedition:", expeditionValues, optionValue1);

        /*
        I'm not sure how to code this part.  Basically what i want is to

        FIRST have the user select a particular expedition from the list in the expeditionOption above.
        Based on the results of that operation, we will...

        SECOND, call the following service:  http://biscicol.org/id/expeditionService/graphs/1
        Where "1" equals the value from the expedition box above.  This returns JSON that when parsed for project_codes populates
        a String[] containing {"DEMOH","DEMOI","DEMOX"}

        THIRD: populate the projectValues box, like....

        String[] projectValues = {"DEMOX", "DEMOJ", "DEMOH"};
        projectCodeOption = addEditableComboBoxOption("projectCode", "Project Code:", "", projectValues);

        I can basically take care of the FIRST and SECOND above but need help with the THIRD... that is, don't let the user progress
        until they select an appropriate expedition and showing them an editableComboBox with project codes they already
        have (or enabling adding a new one)

        for now, i have the project coded as an input string
         */

        projectCodeOption = addStringOption("projectCode", "Project Code:", "DEMOH");

        sampleDataOption = addFileSelectionOption("sampleData", "Sample Data:", "");

        uploadOption = addBooleanOption("upload", "Upload", true);

        //labelOption = (LabelOption) addLabel("Username/password are necessary for verifying project codes, obtaining identifier keys, and uploading to the database");
        usernameOption = addStringOption("username", "Username:", "");
        passwordOption = addCustomOption(new PasswordOption("password", "Password:"));

        // Code for adding user/password as dependent on upload being checked
        //uploadOption.addDependent(usernameOption, true);
        //uploadOption.addDependent(passwordOption, true);
    }
}
