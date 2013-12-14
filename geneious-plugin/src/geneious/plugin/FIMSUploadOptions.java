package geneious.plugin;

import com.biomatters.geneious.publicapi.plugin.Options;


/**
 * Define the Dialog Box Options available from the Geneious Plugin Interface
 */
public class FIMSUploadOptions extends Options {

    StringOption projectCodeOption;
    FileSelectionOption sampleDataOption;
    BooleanOption uploadOption;
    PasswordOption passwordOption;
    StringOption usernameOption;
    LabelOption labelOption;

    public FIMSUploadOptions() {
        super(FIMSUploadOptions.class);

        projectCodeOption = addStringOption("projectCode", "Biocode FIMS Project Code:", "DEMOH");
        sampleDataOption = addFileSelectionOption("sampleData", "Sample Data:", "");

        uploadOption = addBooleanOption("upload", "Upload", false);

        //labelOption = (LabelOption) addLabel("Username/password are necessary for verifying project codes, obtaining identifier keys, and uploading to the database");
        usernameOption = addStringOption("username", "Username:", "");
        passwordOption = addCustomOption(new PasswordOption("password", "Password:"));

        // Code for adding user/password as dependent on upload being checked
        //uploadOption.addDependent(usernameOption, true);
        //uploadOption.addDependent(passwordOption, true);
    }
}
