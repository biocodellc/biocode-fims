package geneious.plugin;

import com.biomatters.geneious.publicapi.plugin.Options;

import java.io.File;
import java.net.URL;

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

        labelOption = (LabelOption) addLabel("Username/password only necessary for uploading");
        usernameOption = addStringOption("username", "Username:", "");
        passwordOption = addCustomOption(new PasswordOption("password", "Password:"));

        // Code for adding user/password as dependent on upload being checked
        //uploadOption.addDependent(usernameOption, true);
        //uploadOption.addDependent(passwordOption, true);

    }
}
